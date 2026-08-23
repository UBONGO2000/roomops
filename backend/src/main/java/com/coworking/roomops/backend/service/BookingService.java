package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.BookingEquipment;
import com.coworking.roomops.backend.domain.BookingStatut;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.BookingConflictException;
import com.coworking.roomops.backend.exception.InvalidBookingPeriodException;
import com.coworking.roomops.backend.exception.InvalidEquipmentSelectionException;
import com.coworking.roomops.backend.exception.OptimisticLockConflictException;
import com.coworking.roomops.backend.repository.BookingEquipmentRepository;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingEquipmentRepository bookingEquipmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            EquipmentRepository equipmentRepository,
            BookingEquipmentRepository bookingEquipmentRepository,
            CurrentUserProvider currentUserProvider) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.bookingEquipmentRepository = bookingEquipmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public Booking createBooking(
            Long roomId, LocalDateTime start, LocalDateTime end, String motif, List<Long> equipmentIds) {
        User actingUser = currentUserProvider.get();
        Room room = getRoomOrThrow(roomId);

        requireValidPeriod(start, end);
        List<Equipment> requestedEquipment = resolveRequestedEquipment(room, equipmentIds);
        ensureRoomBookable(room, requestedEquipment);
        if (bookingRepository.existsOverlapping(room.getId(), start, end, null)) {
            throw new BookingConflictException("Cette salle est déjà réservée sur le créneau demandé");
        }

        Booking booking = new Booking();
        booking.setUser(actingUser);
        booking.setRoom(room);
        booking.setCompany(actingUser.getCompany());
        booking.setDateDebut(start);
        booking.setDateFin(end);
        booking.setMotif(motif);

        Booking saved = saveOrThrowConflict(booking);
        replaceRequestedEquipment(saved, requestedEquipment);
        return saved;
    }

    public Booking getBooking(Long id) {
        Booking booking = getBookingOrThrow(id);
        requireCanAccessBooking(currentUserProvider.get(), booking);
        return booking;
    }

    public Page<Booking> listBookings(
            Long roomId, LocalDateTime dateDebut, LocalDateTime dateFin, BookingStatut statut, int page, int size) {
        User actingUser = currentUserProvider.get();
        Specification<Booking> spec = buildSpecification(actingUser, roomId, dateDebut, dateFin, statut);
        return bookingRepository.findAll(spec, PageRequest.of(page, size, Sort.by("dateDebut").descending()));
    }

    public Booking updateBooking(
            Long id,
            Long newRoomId,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            String motif,
            Long expectedVersion,
            List<Long> equipmentIds) {
        Booking booking = getBookingOrThrow(id);
        User actingUser = currentUserProvider.get();
        requireCanAccessBooking(actingUser, booking);

        if (!booking.getVersion().equals(expectedVersion)) {
            throw new OptimisticLockConflictException(
                    "La réservation a été modifiée depuis votre dernière lecture, merci de recharger");
        }

        Room targetRoom = booking.getRoom();
        if (newRoomId != null && !newRoomId.equals(booking.getRoom().getId())) {
            targetRoom = getRoomOrThrow(newRoomId);
        }

        LocalDateTime start = newStart != null ? newStart : booking.getDateDebut();
        LocalDateTime end = newEnd != null ? newEnd : booking.getDateFin();
        requireValidPeriod(start, end);

        // equipmentIds absent : on conserve les équipements déjà associés, mais ils doivent
        // toujours appartenir à la salle cible — resolveRequestedEquipment rejette en 400 si un
        // équipement conservé n'appartient plus à la nouvelle salle (même règle qu'une liste
        // explicitement fournie ; le client doit alors la refournir pour la nouvelle salle).
        List<Long> idsToResolve =
                equipmentIds != null
                        ? equipmentIds
                        : bookingEquipmentRepository.findByBookingId(booking.getId()).stream()
                                .map(be -> be.getEquipment().getId())
                                .toList();
        List<Equipment> requestedEquipment = resolveRequestedEquipment(targetRoom, idsToResolve);

        ensureRoomBookable(targetRoom, requestedEquipment);
        if (bookingRepository.existsOverlapping(targetRoom.getId(), start, end, booking.getId())) {
            throw new BookingConflictException("Cette salle est déjà réservée sur le créneau demandé");
        }

        booking.setRoom(targetRoom);
        booking.setDateDebut(start);
        booking.setDateFin(end);
        if (motif != null) {
            booking.setMotif(motif);
        }

        Booking saved = saveOrThrowConflict(booking);
        replaceRequestedEquipment(saved, requestedEquipment);
        return saved;
    }

    public void cancelBooking(Long id) {
        Booking booking = getBookingOrThrow(id);
        requireCanAccessBooking(currentUserProvider.get(), booking);

        booking.setStatut(BookingStatut.ANNULEE);
        bookingRepository.save(booking);
    }

    private Booking saveOrThrowConflict(Booking booking) {
        try {
            Booking saved = bookingRepository.save(booking);
            // flush() force l'exécution immédiate du INSERT/UPDATE dans cette méthode : si la
            // contrainte EXCLUDE (no_overlapping_booking) est violée par une course avec une
            // autre requête passée entre-temps, l'exception est levée ici et pas plus tard.
            bookingRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new BookingConflictException("Cette salle est déjà réservée sur le créneau demandé");
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new OptimisticLockConflictException(
                    "La réservation a été modifiée depuis votre dernière lecture, merci de recharger");
        }
    }

    // N'importe pas la panne d'un équipement non sollicité par cette réservation (éco-toggle) :
    // seul un équipement présent dans requestedEquipment bloque, cf. resolveRequestedEquipment.
    private void ensureRoomBookable(Room room, List<Equipment> requestedEquipment) {
        if (!room.isEstActif()) {
            throw new BookingConflictException("Cette salle n'est pas active");
        }
        boolean hasRequestedPanne =
                requestedEquipment.stream().anyMatch(equipment -> equipment.getStatut() == EquipmentStatut.EN_PANNE);
        if (hasRequestedPanne) {
            throw new BookingConflictException("Cette salle est indisponible (équipement en panne)");
        }
    }

    /**
     * Résout et valide les équipements demandés pour une salle : chaque id doit appartenir à
     * {@code room}, sinon la requête est rejetée (même règle que RoomService.checkAvailability).
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

    private void replaceRequestedEquipment(Booking booking, List<Equipment> requestedEquipment) {
        bookingEquipmentRepository.deleteByBookingId(booking.getId());
        List<BookingEquipment> rows =
                requestedEquipment.stream()
                        .map(
                                equipment -> {
                                    BookingEquipment bookingEquipment = new BookingEquipment();
                                    bookingEquipment.setBooking(booking);
                                    bookingEquipment.setEquipment(equipment);
                                    return bookingEquipment;
                                })
                        .toList();
        bookingEquipmentRepository.saveAll(rows);
    }

    public List<Equipment> getActiveEquipment(Booking booking) {
        return bookingEquipmentRepository.findByBookingId(booking.getId()).stream()
                .map(BookingEquipment::getEquipment)
                .toList();
    }

    public int countRoomEquipment(Booking booking) {
        return equipmentRepository.findByRoomId(booking.getRoom().getId()).size();
    }

    private void requireValidPeriod(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidBookingPeriodException("La date de fin doit être après la date de début");
        }
    }

    private void requireCanAccessBooking(User actingUser, Booking booking) {
        boolean allowed =
                switch (actingUser.getRole()) {
                    case SUPER_ADMIN -> true;
                    case MANAGER ->
                            actingUser.getCompany() != null
                                    && actingUser.getCompany().getId().equals(booking.getCompany().getId());
                    case EMPLOYEE -> actingUser.getId().equals(booking.getUser().getId());
                };
        if (!allowed) {
            throw new AccessDeniedException("Vous n'avez pas le droit d'accéder à cette réservation");
        }
    }

    private Specification<Booking> buildSpecification(
            User actingUser, Long roomId, LocalDateTime dateDebut, LocalDateTime dateFin, BookingStatut statut) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            switch (actingUser.getRole()) {
                case EMPLOYEE -> predicates.add(criteriaBuilder.equal(root.get("user").get("id"), actingUser.getId()));
                case MANAGER ->
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("company").get("id"), actingUser.getCompany().getId()));
                case SUPER_ADMIN -> {
                    // Aucune restriction : le Super-Admin voit toutes les réservations.
                }
            }
            if (roomId != null) {
                predicates.add(criteriaBuilder.equal(root.get("room").get("id"), roomId));
            }
            if (statut != null) {
                predicates.add(criteriaBuilder.equal(root.get("statut"), statut));
            }
            if (dateDebut != null) {
                predicates.add(criteriaBuilder.greaterThan(root.get("dateFin"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("dateDebut"), dateFin));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Room getRoomOrThrow(Long roomId) {
        return roomRepository
                .findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Salle introuvable : " + roomId));
    }

    private Booking getBookingOrThrow(Long id) {
        return bookingRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réservation introuvable : " + id));
    }
}
