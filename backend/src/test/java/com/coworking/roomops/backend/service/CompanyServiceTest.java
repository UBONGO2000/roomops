package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.EmployeeHasBookingsException;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests unitaires purs (repositories mockés) du scoping "un Manager ne gère que sa propre
 * entreprise" — la partie de CompanyService qui n'est pas qu'une simple annotation
 * @PreAuthorize et qui mérite un vrai test.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private CompanyService companyService;

    private Company ownCompany;
    private Company otherCompany;

    @BeforeEach
    void setUp() {
        ownCompany = new Company();
        ownCompany.setId(1L);
        otherCompany = new Company();
        otherCompany.setId(2L);
    }

    @Test
    void manager_canListEmployeesOfOwnCompany() {
        User manager = new User();
        manager.setRole(Role.MANAGER);
        manager.setCompany(ownCompany);

        when(currentUserProvider.get()).thenReturn(manager);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(ownCompany));
        when(userRepository.findByCompanyId(1L)).thenReturn(java.util.List.of());

        assertEquals(0, companyService.getCompanyEmployees(1L).size());
    }

    @Test
    void manager_cannotListEmployeesOfAnotherCompany() {
        User manager = new User();
        manager.setRole(Role.MANAGER);
        manager.setCompany(ownCompany);

        when(currentUserProvider.get()).thenReturn(manager);
        when(companyRepository.findById(2L)).thenReturn(Optional.of(otherCompany));

        assertThrows(AccessDeniedException.class, () -> companyService.getCompanyEmployees(2L));
        verify(userRepository, never()).findByCompanyId(any());
    }

    @Test
    void superAdmin_canListEmployeesOfAnyCompany() {
        User superAdmin = new User();
        superAdmin.setRole(Role.SUPER_ADMIN);

        when(currentUserProvider.get()).thenReturn(superAdmin);
        when(companyRepository.findById(2L)).thenReturn(Optional.of(otherCompany));
        when(userRepository.findByCompanyId(2L)).thenReturn(java.util.List.of());

        assertEquals(0, companyService.getCompanyEmployees(2L).size());
    }

    @Test
    void getCompanyEmployees_unknownCompany_throwsNotFound() {
        // getCompanyOrThrow() est appelé avant requireSameCompanyIfManager(), qui est donc
        // jamais atteint : currentUserProvider n'a pas besoin d'être stubé ici.
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> companyService.getCompanyEmployees(99L));
    }

    @Test
    void removeEmployee_translatesForeignKeyViolationIntoDomainException() {
        User superAdmin = new User();
        superAdmin.setRole(Role.SUPER_ADMIN);

        User target = new User();
        target.setId(5L);
        target.setCompany(ownCompany);

        when(currentUserProvider.get()).thenReturn(superAdmin);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(ownCompany));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        doThrow(new DataIntegrityViolationException("fk violation")).when(userRepository).delete(target);

        assertThrows(EmployeeHasBookingsException.class, () -> companyService.removeEmployee(1L, 5L));
    }
}
