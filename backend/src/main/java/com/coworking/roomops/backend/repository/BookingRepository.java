package com.coworking.roomops.backend.repository;

import com.coworking.roomops.backend.domain.Booking;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

        boolean existsByCompanyId(Long companyId);

    /**
     * Vérification applicative de recouvrement (première ligne de défense contre le
     * double-booking). {@code excludeBookingId} permet, en modification, d'ignorer la
     * réservation elle-même dans la comparaison. La contrainte {@code no_overlapping_booking}
     * (EXCLUDE USING GIST, V1__create_initial_schema.sql) reste le garde-fou ultime en cas
     * de course entre deux requêtes concurrentes passant toutes deux ce contrôle.
     */
    @Query(
            "SELECT COUNT(b) > 0 FROM Booking b "
                    + "WHERE b.room.id = :roomId "
                    + "AND b.statut <> com.coworking.roomops.backend.domain.BookingStatut.ANNULEE "
                    + "AND b.dateDebut < :dateFin "
                    + "AND b.dateFin > :dateDebut "
                    + "AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)")
    boolean existsOverlapping(
            @Param("roomId") Long roomId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("excludeBookingId") Long excludeBookingId);

    /**
     * Réservations CONFIRMEE futures d'une salle qui sollicitent réellement un équipement donné
     * (table de jonction booking_equipment) : utilisé pour l'annulation en cascade lors d'un
     * passage EN_PANNE, afin de ne pas annuler une réservation qui n'a jamais demandé
     * l'équipement fautif (même règle éco-toggle que BookingService.ensureRoomBookable /
     * RoomService.checkAvailability).
     */
    @Query(
            "SELECT DISTINCT b FROM Booking b JOIN BookingEquipment be ON be.booking = b "
                    + "WHERE b.room.id = :roomId "
                    + "AND b.statut = com.coworking.roomops.backend.domain.BookingStatut.CONFIRMEE "
                    + "AND b.dateDebut > :now "
                    + "AND be.equipment.id = :equipmentId")
    java.util.List<Booking> findByRoomIdAndStatutAndDateDebutAfterAndEquipmentId(
            @Param("roomId") Long roomId, @Param("now") LocalDateTime now, @Param("equipmentId") Long equipmentId);

    java.util.List<Booking> findByUserId(Long userId);
}
