package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.EmployeeHasBookingsException;
import com.coworking.roomops.backend.exception.CompanyHasBookingsException;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public CompanyService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<Company> listCompanies() {
        return companyRepository.findAll();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Company createCompany(String nom, String siret, String adresseFacturation, BigDecimal tarifHoraire) {
        Company company = new Company();
        company.setNom(nom);
        company.setSiret(siret);
        company.setAdresseFacturation(adresseFacturation);
        company.setTarifHoraire(tarifHoraire);
        return companyRepository.save(company);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public void deleteCompany(Long companyId) {
        Company company = getCompanyOrThrow(companyId);
        if (bookingRepository.existsByCompanyId(companyId)) {
            throw new CompanyHasBookingsException("Cette entreprise possède des réservations");
        }
        userRepository.deleteByCompanyId(companyId);
        companyRepository.delete(company);
    }

    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public List<User> getCompanyEmployees(Long companyId) {
        getCompanyOrThrow(companyId);
        requireSameCompanyIfManager(companyId);
        return userRepository.findByCompanyId(companyId);
    }

    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public User addEmployee(Long companyId, String email, String rawPassword, String nom, String prenom, Role role) {
        Company company = getCompanyOrThrow(companyId);
        User actingUser = currentUserProvider.get();
        requireSameCompanyIfManager(companyId);

        Role requestedRole = role != null ? role : Role.EMPLOYEE;
        if (requestedRole == Role.SUPER_ADMIN
                || (actingUser.getRole() == Role.MANAGER && requestedRole != Role.EMPLOYEE)) {
            throw new AccessDeniedException("Ce rôle ne peut pas être créé depuis cette entreprise");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setRole(requestedRole);
        user.setCompany(company);
        return userRepository.save(user);
    }

    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public void removeEmployee(Long companyId, Long userId) {
        getCompanyOrThrow(companyId);
        requireSameCompanyIfManager(companyId);

        User target =
                userRepository
                        .findById(userId)
                        .filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId))
                        .orElseThrow(() -> new EntityNotFoundException("Employé introuvable dans cette entreprise"));

        try {
            // delete() est transactionnel et committe avant de rendre la main : une violation
            // de contrainte (l'employé a des réservations en base) est donc levée ici, pas
            // silencieusement plus tard.
            userRepository.delete(target);
        } catch (DataIntegrityViolationException ex) {
            throw new EmployeeHasBookingsException(
                    "Impossible de supprimer cet employé : il a des réservations associées");
        }
    }

    private Company getCompanyOrThrow(Long companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable : " + companyId));
    }

    private void requireSameCompanyIfManager(Long companyId) {
        User actingUser = currentUserProvider.get();
        if (actingUser.getRole() == Role.MANAGER
                && (actingUser.getCompany() == null || !actingUser.getCompany().getId().equals(companyId))) {
            throw new AccessDeniedException("Un Manager ne peut gérer que les employés de sa propre entreprise");
        }
    }
}
