package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;

    public RoomService(
            RoomRepository roomRepository, EquipmentRepository equipmentRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    public Room getRoom(Long roomId) {
        return getRoomOrThrow(roomId);
    }

    public RoomAvailability checkAvailability(Long roomId, LocalDateTime start, LocalDateTime end) {
        Room room = getRoomOrThrow(roomId);

        String panne = panneReason(room);
        if (panne != null) {
            return new RoomAvailability(room, false, panne);
        }
        if (bookingRepository.existsOverlapping(roomId, start, end, null)) {
            return new RoomAvailability(room, false, "Créneau déjà réservé");
        }
        return new RoomAvailability(room, true, null);
    }

    public String panneReason(Room room) {
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
