package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.RoomsApi;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.mapper.DateTimeMapper;
import com.coworking.roomops.backend.model.AvailabilityResponse;
import com.coworking.roomops.backend.model.RoomResponse;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController implements RoomsApi {

    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;

    public RoomController(
            RoomRepository roomRepository, EquipmentRepository equipmentRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public ResponseEntity<List<RoomResponse>> listRooms() {
        return ResponseEntity.ok(roomRepository.findAll().stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<RoomResponse> getRoomById(Long roomId) {
        return ResponseEntity.ok(toResponse(getRoomOrThrow(roomId)));
    }

    @Override
    public ResponseEntity<AvailabilityResponse> checkRoomAvailability(
            Long roomId, OffsetDateTime dateDebut, OffsetDateTime dateFin) {
        Room room = getRoomOrThrow(roomId);
        LocalDateTime start = DateTimeMapper.toUtcLocalDateTime(dateDebut);
        LocalDateTime end = DateTimeMapper.toUtcLocalDateTime(dateFin);

        String panneReason = panneReason(room);
        boolean available;
        String reason = null;
        if (panneReason != null) {
            available = false;
            reason = panneReason;
        } else if (bookingRepository.existsOverlapping(roomId, start, end, null)) {
            available = false;
            reason = "Créneau déjà réservé";
        } else {
            available = true;
        }

        AvailabilityResponse response =
                new AvailabilityResponse()
                        .roomId(room.getId())
                        .roomName(room.getNom())
                        .isAvailable(available)
                        .reason(reason)
                        .dateDebut(dateDebut)
                        .dateFin(dateFin);
        return ResponseEntity.ok(response);
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse()
                .id(room.getId())
                .nom(room.getNom())
                .capacite(room.getCapacite())
                .buildingId(room.getBuilding().getId())
                .buildingName(room.getBuilding().getNom())
                .estActif(room.isEstActif())
                .indisponibilite(panneReason(room));
    }

    private String panneReason(Room room) {
        return equipmentRepository.findByRoomId(room.getId()).stream()
                .filter(equipment -> equipment.getStatut() == EquipmentStatut.EN_PANNE)
                .map(equipment -> "Panne : " + equipment.getType())
                .findFirst()
                .orElse(null);
    }

    private Room getRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("Salle introuvable : " + roomId));
    }
}
