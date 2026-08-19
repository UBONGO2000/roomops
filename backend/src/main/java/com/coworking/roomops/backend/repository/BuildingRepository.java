package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}
