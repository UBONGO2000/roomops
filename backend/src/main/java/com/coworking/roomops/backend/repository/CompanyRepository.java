package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
