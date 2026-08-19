package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.EquipmentsApi;
import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.BookingStatut;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.model.EquipmentResponse;
import com.coworking.roomops.backend.model.UpdateEquipmentStatusRequest;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EquipmentController implements EquipmentsApi {

    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;

    public EquipmentController(EquipmentRepository equipmentRepository, BookingRepository bookingRepository) {
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<EquipmentResponse>> listEquipments() {
        return ResponseEntity.ok(equipmentRepository.findAll().stream().map(this::toResponse).toList());
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(
            Long equipmentId, UpdateEquipmentStatusRequest updateEquipmentStatusRequest) {
        Equipment equipment =
                equipmentRepository
                        .findById(equipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Équipement introuvable : " + equipmentId));

        EquipmentStatut newStatus = EquipmentStatut.valueOf(updateEquipmentStatusRequest.getStatut().name());
        equipment.setStatut(newStatus);
        equipmentRepository.save(equipment);

        // @Transactional : le changement de statut et l'annulation en cascade doivent réussir
        // ou échouer ensemble, pas être commités dans deux transactions séparées.
        if (newStatus == EquipmentStatut.EN_PANNE) {
            cancelFutureBookingsForRoom(equipment.getRoom().getId());
        }

        return ResponseEntity.ok(toResponse(equipment));
    }

    private void cancelFutureBookingsForRoom(Long roomId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<Booking> affected =
                bookingRepository.findByRoomIdAndStatutAndDateDebutAfter(roomId, BookingStatut.CONFIRMEE, now);
        affected.forEach(booking -> booking.setStatut(BookingStatut.ANNULEE));
        bookingRepository.saveAll(affected);
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse()
                .id(equipment.getId())
                .type(equipment.getType())
                .roomId(equipment.getRoom().getId())
                .roomName(equipment.getRoom().getNom())
                .statut(com.coworking.roomops.backend.model.EquipmentStatut.valueOf(equipment.getStatut().name()));
    }
}
