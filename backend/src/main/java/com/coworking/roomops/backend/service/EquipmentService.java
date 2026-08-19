package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.BookingStatut;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, BookingRepository bookingRepository) {
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<Equipment> listEquipments() {
        return equipmentRepository.findAll();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public Equipment updateStatus(Long equipmentId, EquipmentStatut newStatus) {
        Equipment equipment =
                equipmentRepository
                        .findById(equipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Équipement introuvable : " + equipmentId));

        equipment.setStatut(newStatus);
        equipmentRepository.save(equipment);

        // @Transactional : le changement de statut et l'annulation en cascade doivent réussir
        // ou échouer ensemble, pas être commités dans deux transactions séparées.
        if (newStatus == EquipmentStatut.EN_PANNE) {
            cancelFutureBookingsForRoom(equipment.getRoom().getId());
        }

        return equipment;
    }

    private void cancelFutureBookingsForRoom(Long roomId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Booking> affected =
                bookingRepository.findByRoomIdAndStatutAndDateDebutAfter(roomId, BookingStatut.CONFIRMEE, now);
        affected.forEach(booking -> booking.setStatut(BookingStatut.ANNULEE));
        bookingRepository.saveAll(affected);
    }
}
