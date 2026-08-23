package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.exception.InvalidEquipmentSelectionException;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    /**
     * Éco-toggle : un équipement en panne ne bloque la disponibilité que s'il figure dans
     * {@code equipmentIds} (liste vide ou absente = aucun équipement requis pour cet usage,
     * mêmes règles que BookingService.ensureRoomBookable pour la création de réservation).
     */
    public RoomAvailability checkAvailability(Long roomId, LocalDateTime start, LocalDateTime end, List<Long> equipmentIds) {
        Room room = getRoomOrThrow(roomId);

        List<Equipment> requestedEquipment = resolveRequestedEquipment(room, equipmentIds);
        String panne =
                requestedEquipment.stream()
                        .filter(equipment -> equipment.getStatut() == EquipmentStatut.EN_PANNE)
                        .map(equipment -> "Panne : " + equipment.getType())
                        .findFirst()
                        .orElse(null);
        if (panne != null) {
            return new RoomAvailability(room, false, panne);
        }
        if (bookingRepository.existsOverlapping(roomId, start, end, null)) {
            return new RoomAvailability(room, false, "Créneau déjà réservé");
        }
        return new RoomAvailability(room, true, null);
    }

    // Statut général de la salle (listRooms/getRoomById) : indépendant de tout usage précis,
    // donc toute panne est signalée ici, contrairement à checkAvailability qui ne bloque que
    // sur un équipement explicitement demandé.
    public String panneReason(Room room) {
        return equipmentRepository.findByRoomId(room.getId()).stream()
                .filter(equipment -> equipment.getStatut() == EquipmentStatut.EN_PANNE)
                .map(equipment -> "Panne : " + equipment.getType())
                .findFirst()
                .orElse(null);
    }

    public List<Equipment> equipmentsOf(Room room) {
        return equipmentRepository.findByRoomId(room.getId());
    }

    /**
     * Résout et valide les équipements demandés pour une salle : chaque id doit appartenir à
     * {@code room}, sinon la requête est rejetée (cf. BookingService, même règle appliquée à la
     * création/modification de réservation).
     */
    private List<Equipment> resolveRequestedEquipment(Room room, List<Long> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return List.of();
        }
        List<Equipment> found = equipmentRepository.findByRoomIdAndIdIn(room.getId(), equipmentIds);
        if (found.size() != new HashSet<>(equipmentIds).size()) {
            throw new InvalidEquipmentSelectionException(
                    "Un ou plusieurs équipements demandés n'appartiennent pas à cette salle");
        }
        return found;
    }

    private Room getRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("Salle introuvable : " + roomId));
    }
}
