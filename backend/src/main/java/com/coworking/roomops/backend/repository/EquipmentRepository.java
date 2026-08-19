package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
