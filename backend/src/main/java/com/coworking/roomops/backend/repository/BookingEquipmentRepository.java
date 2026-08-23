package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.BookingEquipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {

    List<BookingEquipment> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
