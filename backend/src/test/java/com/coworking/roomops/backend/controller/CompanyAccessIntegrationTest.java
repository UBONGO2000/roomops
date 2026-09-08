package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.coworking.roomops.backend.model.CreateUserRequest;
import com.coworking.roomops.backend.service.CompanyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CompanyAccessIntegrationTest {

    @Autowired private CompanyController companyController;

    @MockitoBean private CompanyService companyService;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotReadCreateOrDeleteEmployees() {
        CreateUserRequest request = new CreateUserRequest();

        assertThrows(AccessDeniedException.class, () -> companyController.getCompanyEmployees(1L));
        assertThrows(AccessDeniedException.class, () -> companyController.addEmployee(1L, request));
        assertThrows(AccessDeniedException.class, () -> companyController.removeEmployee(1L, 2L));

        verify(companyService, never()).getCompanyEmployees(1L);
        verify(companyService, never()).addEmployee(1L, null, null, null, null, null);
        verify(companyService, never()).removeEmployee(1L, 2L);
    }
}