package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
