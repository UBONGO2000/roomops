package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.CompaniesApi;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.EmployeeHasBookingsException;
import com.coworking.roomops.backend.mapper.CompanyMapper;
import com.coworking.roomops.backend.mapper.UserMapper;
import com.coworking.roomops.backend.model.CompanyRequest;
import com.coworking.roomops.backend.model.CompanyResponse;
import com.coworking.roomops.backend.model.CreateUserRequest;
import com.coworking.roomops.backend.model.UserResponse;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompanyController implements CompaniesApi {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public CompanyController(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        List<CompanyResponse> body = companyRepository.findAll().stream().map(CompanyMapper::toResponse).toList();
        return ResponseEntity.ok(body);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(CompanyRequest companyRequest) {
        Company company = new Company();
        company.setNom(companyRequest.getNom());
        company.setSiret(companyRequest.getSiret());
        company.setAdresseFacturation(companyRequest.getAdresseFacturation());
        if (companyRequest.getTarifHoraire() != null) {
            company.setTarifHoraire(BigDecimal.valueOf(companyRequest.getTarifHoraire()));
        }

        Company saved = companyRepository.save(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyMapper.toResponse(saved));
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getCompanyEmployees(Long companyId) {
        getCompanyOrThrow(companyId);
        requireSameCompanyIfManager(companyId);

        List<UserResponse> body =
                userRepository.findByCompanyId(companyId).stream().map(UserMapper::toResponse).toList();
        return ResponseEntity.ok(body);
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public ResponseEntity<UserResponse> addEmployee(Long companyId, CreateUserRequest createUserRequest) {
        Company company = getCompanyOrThrow(companyId);
        requireSameCompanyIfManager(companyId);

        User user = new User();
        user.setEmail(createUserRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setNom(createUserRequest.getNom());
        user.setPrenom(createUserRequest.getPrenom());
        user.setRole(Role.valueOf(createUserRequest.getRole().name()));
        user.setCompany(company);

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(saved));
    }

    @Override
    @PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
    public ResponseEntity<Void> removeEmployee(Long companyId, Long userId) {
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

        return ResponseEntity.noContent().build();
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
