package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.RoomsApi;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.mapper.DateTimeMapper;
import com.coworking.roomops.backend.model.AvailabilityResponse;
import com.coworking.roomops.backend.model.RoomResponse;
import com.coworking.roomops.backend.service.RoomAvailability;
import com.coworking.roomops.backend.service.RoomService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController implements RoomsApi {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public ResponseEntity<List<RoomResponse>> listRooms() {
        return ResponseEntity.ok(roomService.listRooms().stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<RoomResponse> getRoomById(Long roomId) {
        return ResponseEntity.ok(toResponse(roomService.getRoom(roomId)));
    }

    @Override
    public ResponseEntity<AvailabilityResponse> checkRoomAvailability(
            Long roomId, OffsetDateTime dateDebut, OffsetDateTime dateFin) {
        RoomAvailability availability =
                roomService.checkAvailability(
                        roomId, DateTimeMapper.toUtcLocalDateTime(dateDebut), DateTimeMapper.toUtcLocalDateTime(dateFin));

        AvailabilityResponse response =
                new AvailabilityResponse()
                        .roomId(availability.room().getId())
                        .roomName(availability.room().getNom())
                        .isAvailable(availability.available())
                        .reason(availability.reason())
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
                .indisponibilite(roomService.panneReason(room));
    }
}
