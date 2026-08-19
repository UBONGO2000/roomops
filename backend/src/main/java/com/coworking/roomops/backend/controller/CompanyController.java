package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.CompaniesApi;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.mapper.CompanyMapper;
import com.coworking.roomops.backend.mapper.UserMapper;
import com.coworking.roomops.backend.model.CompanyRequest;
import com.coworking.roomops.backend.model.CompanyResponse;
import com.coworking.roomops.backend.model.CreateUserRequest;
import com.coworking.roomops.backend.model.UserResponse;
import com.coworking.roomops.backend.service.CompanyService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompanyController implements CompaniesApi {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        return ResponseEntity.ok(companyService.listCompanies().stream().map(CompanyMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<CompanyResponse> createCompany(CompanyRequest companyRequest) {
        BigDecimal tarifHoraire =
                companyRequest.getTarifHoraire() != null ? BigDecimal.valueOf(companyRequest.getTarifHoraire()) : null;
        Company saved =
                companyService.createCompany(
                        companyRequest.getNom(), companyRequest.getSiret(), companyRequest.getAdresseFacturation(), tarifHoraire);
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<List<UserResponse>> getCompanyEmployees(Long companyId) {
        List<UserResponse> body =
                companyService.getCompanyEmployees(companyId).stream().map(UserMapper::toResponse).toList();
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<UserResponse> addEmployee(Long companyId, CreateUserRequest createUserRequest) {
        User saved =
                companyService.addEmployee(
                        companyId,
                        createUserRequest.getEmail(),
                        createUserRequest.getPassword(),
                        createUserRequest.getNom(),
                        createUserRequest.getPrenom(),
                        Role.valueOf(createUserRequest.getRole().name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<Void> removeEmployee(Long companyId, Long userId) {
        companyService.removeEmployee(companyId, userId);
        return ResponseEntity.noContent().build();
    }
}
