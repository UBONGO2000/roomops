package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.BookingsApi;
import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.BookingConflictException;
import com.coworking.roomops.backend.exception.InvalidBookingPeriodException;
import com.coworking.roomops.backend.exception.OptimisticLockConflictException;
import com.coworking.roomops.backend.mapper.BookingMapper;
import com.coworking.roomops.backend.mapper.DateTimeMapper;
import com.coworking.roomops.backend.model.BookingPageResponse;
import com.coworking.roomops.backend.model.BookingRequest;
import com.coworking.roomops.backend.model.BookingResponse;
import com.coworking.roomops.backend.model.BookingUpdateRequest;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController implements BookingsApi {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public BookingController(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            EquipmentRepository equipmentRepository,
            CurrentUserProvider currentUserProvider) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<BookingResponse> createBooking(BookingRequest bookingRequest) {
        User actingUser = currentUserProvider.get();
        Room room = getRoomOrThrow(bookingRequest.getRoomId());

        LocalDateTime start = DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateDebut());
        LocalDateTime end = DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateFin());
        requireValidPeriod(start, end);
        ensureRoomBookable(room);
        if (bookingRepository.existsOverlapping(room.getId(), start, end, null)) {
            throw new BookingConflictException("Cette salle est déjà réservée sur le créneau demandé");
        }

        Booking booking = new Booking();
        booking.setUser(actingUser);
        booking.setRoom(room);
        booking.setCompany(actingUser.getCompany());
        booking.setDateDebut(start);
        booking.setDateFin(end);
        booking.setMotif(bookingRequest.getMotif());

        Booking saved = saveOrThrowConflict(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<BookingResponse> getBookingById(Long id) {
        Booking booking = getBookingOrThrow(id);
        requireCanAccessBooking(currentUserProvider.get(), booking);
        return ResponseEntity.ok(BookingMapper.toResponse(booking));
    }

    @Override
    public ResponseEntity<BookingPageResponse> listBookings(
            Long roomId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            com.coworking.roomops.backend.model.BookingStatut statut,
            Integer page,
            Integer size) {
        User actingUser = currentUserProvider.get();
        Specification<Booking> spec =
                buildSpecification(
                        actingUser,
                        roomId,
                        dateDebut != null ? DateTimeMapper.toUtcLocalDateTime(dateDebut) : null,
                        dateFin != null ? DateTimeMapper.toUtcLocalDateTime(dateFin) : null,
                        statut != null
                                ? com.coworking.roomops.backend.domain.BookingStatut.valueOf(statut.name())
                                : null);

        Page<Booking> result =
                bookingRepository.findAll(spec, PageRequest.of(page, size, Sort.by("dateDebut").descending()));

        BookingPageResponse response =
                new BookingPageResponse()
                        .content(result.getContent().stream().map(BookingMapper::toResponse).toList())
                        .totalElements(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .number(result.getNumber())
                        .size(result.getSize());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BookingResponse> updateBooking(Long id, BookingUpdateRequest bookingUpdateRequest) {
        Booking booking = getBookingOrThrow(id);
        User actingUser = currentUserProvider.get();
        requireCanAccessBooking(actingUser, booking);

        if (!booking.getVersion().equals(bookingUpdateRequest.getVersion())) {
            throw new OptimisticLockConflictException(
                    "La réservation a été modifiée depuis votre dernière lecture, merci de recharger");
        }

        Room targetRoom = booking.getRoom();
        if (bookingUpdateRequest.getRoomId() != null
                && !bookingUpdateRequest.getRoomId().equals(booking.getRoom().getId())) {
            targetRoom = getRoomOrThrow(bookingUpdateRequest.getRoomId());
        }

        LocalDateTime start =
                bookingUpdateRequest.getDateDebut() != null
                        ? DateTimeMapper.toUtcLocalDateTime(bookingUpdateRequest.getDateDebut())
                        : booking.getDateDebut();
        LocalDateTime end =
                bookingUpdateRequest.getDateFin() != null
                        ? DateTimeMapper.toUtcLocalDateTime(bookingUpdateRequest.getDateFin())
                        : booking.getDateFin();
        requireValidPeriod(start, end);

        ensureRoomBookable(targetRoom);
        if (bookingRepository.existsOverlapping(targetRoom.getId(), start, end, booking.getId())) {
            throw new BookingConflictException("Cette salle est déjà réservée sur le créneau demandé");
        }

        booking.setRoom(targetRoom);
        booking.setDateDebut(start);
        booking.setDateFin(end);
        if (bookingUpdateRequest.getMotif() != null) {
            booking.setMotif(bookingUpdateRequest.getMotif());
        }

        Booking saved = saveOrThrowConflict(booking);
        return ResponseEntity.ok(BookingMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<Void> cancelBooking(Long id) {
        Booking booking = getBookingOrThrow(id);
        requireCanAccessBooking(currentUserProvider.get(), booking);

        booking.setStatut(com.coworking.roomops.backend.domain.BookingStatut.ANNULEE);
        bookingRepository.save(booking);
        return ResponseEntity.noContent().build();
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

    private void ensureRoomBookable(Room room) {
        if (!room.isEstActif()) {
            throw new BookingConflictException("Cette salle n'est pas active");
        }
        boolean hasPanne =
                equipmentRepository.findByRoomId(room.getId()).stream()
                        .anyMatch(equipment -> equipment.getStatut() == EquipmentStatut.EN_PANNE);
        if (hasPanne) {
            throw new BookingConflictException("Cette salle est indisponible (équipement en panne)");
        }
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
            User actingUser,
            Long roomId,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            com.coworking.roomops.backend.domain.BookingStatut statut) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

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
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
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
