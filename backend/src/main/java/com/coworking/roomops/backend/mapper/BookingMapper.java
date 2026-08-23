package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.model.BookingResponse;
import java.util.List;

public final class BookingMapper {

    private BookingMapper() {}

    /**
     * @param activeEquipment équipements réellement demandés pour cette réservation (table de
     *     jonction booking_equipment)
     * @param totalRoomEquipmentCount nombre total d'équipements de la salle, pour calculer
     *     ecoScore
     */
    public static BookingResponse toResponse(
            Booking booking, List<Equipment> activeEquipment, int totalRoomEquipmentCount) {
        return new BookingResponse()
                .id(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getNom())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getPrenom() + " " + booking.getUser().getNom())
                .companyName(booking.getCompany().getNom())
                .dateDebut(DateTimeMapper.toOffsetDateTime(booking.getDateDebut()))
                .dateFin(DateTimeMapper.toOffsetDateTime(booking.getDateFin()))
                .statut(com.coworking.roomops.backend.model.BookingStatut.valueOf(booking.getStatut().name()))
                .motif(booking.getMotif())
                .equipmentsActives(activeEquipment.stream().map(EquipmentMapper::toResponse).toList())
                .ecoScore(computeEcoScore(activeEquipment.size(), totalRoomEquipmentCount))
                .version(booking.getVersion());
    }

    // Convention : 100 si la salle ne possède aucun équipement (rien à ne pas activer).
    private static int computeEcoScore(int activeCount, int totalRoomEquipmentCount) {
        if (totalRoomEquipmentCount == 0) {
            return 100;
        }
        int notActivated = totalRoomEquipmentCount - activeCount;
        return (int) Math.round(100.0 * notActivated / totalRoomEquipmentCount);
    }
}
