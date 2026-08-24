package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.BookingsApi;
import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.exception.InvalidIcalFileException;
import com.coworking.roomops.backend.mapper.BookingMapper;
import com.coworking.roomops.backend.mapper.DateTimeMapper;
import com.coworking.roomops.backend.mapper.IcalBookingParams;
import com.coworking.roomops.backend.model.BookingPageResponse;
import com.coworking.roomops.backend.model.BookingRequest;
import com.coworking.roomops.backend.model.BookingResponse;
import com.coworking.roomops.backend.model.BookingUpdateRequest;
import com.coworking.roomops.backend.service.BookingService;
import com.coworking.roomops.backend.service.IcalService;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final IcalService icalService;

    public BookingController(BookingService bookingService, IcalService icalService) {
        this.bookingService = bookingService;
        this.icalService = icalService;
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(BookingRequest bookingRequest) {
        Booking saved =
                bookingService.createBooking(
                        bookingRequest.getRoomId(),
                        DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateDebut()),
                        DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateFin()),
                        bookingRequest.getMotif(),
                        bookingRequest.getEquipmentIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @Override
    public ResponseEntity<BookingResponse> getBookingById(Long id) {
        return ResponseEntity.ok(toResponse(bookingService.getBooking(id)));
    }

    @Override
    public ResponseEntity<BookingPageResponse> listBookings(
            Long roomId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFin,
            com.coworking.roomops.backend.model.BookingStatut statut,
            Integer page,
            Integer size) {
        Page<Booking> result =
                bookingService.listBookings(
                        roomId,
                        dateDebut != null ? DateTimeMapper.toUtcLocalDateTime(dateDebut) : null,
                        dateFin != null ? DateTimeMapper.toUtcLocalDateTime(dateFin) : null,
                        statut != null
                                ? com.coworking.roomops.backend.domain.BookingStatut.valueOf(statut.name())
                                : null,
                        page,
                        size);

        BookingPageResponse response =
                new BookingPageResponse()
                        .content(result.getContent().stream().map(this::toResponse).toList())
                        .totalElements(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .number(result.getNumber())
                        .size(result.getSize());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BookingResponse> updateBooking(Long id, BookingUpdateRequest bookingUpdateRequest) {
        Booking saved =
                bookingService.updateBooking(
                        id,
                        bookingUpdateRequest.getRoomId(),
                        bookingUpdateRequest.getDateDebut() != null
                                ? DateTimeMapper.toUtcLocalDateTime(bookingUpdateRequest.getDateDebut())
                                : null,
                        bookingUpdateRequest.getDateFin() != null
                                ? DateTimeMapper.toUtcLocalDateTime(bookingUpdateRequest.getDateFin())
                                : null,
                        bookingUpdateRequest.getMotif(),
                        bookingUpdateRequest.getVersion(),
                        bookingUpdateRequest.getEquipmentIds());
        return ResponseEntity.ok(toResponse(saved));
    }

    @Override
    public ResponseEntity<Void> cancelBooking(Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Resource> exportBookingIcal(Long id) {
        // getBooking porte déjà le contrôle d'accès (requireCanAccessBooking) : pas de logique
        // de droits dupliquée ici.
        Booking booking = bookingService.getBooking(id);
        byte[] ics = icalService.export(booking);
        return ResponseEntity.ok().contentType(MediaType.valueOf("text/calendar")).body(new ByteArrayResource(ics));
    }

    @Override
    public ResponseEntity<BookingResponse> importBookingIcal(Resource body) {
        IcalBookingParams params;
        try (InputStream input = body.getInputStream()) {
            params = icalService.parse(input);
        } catch (IOException e) {
            throw new InvalidIcalFileException("Impossible de lire le fichier envoyé");
        }

        // equipmentIds volontairement vide : aucun équipement n'est reconstruit depuis l'import
        // (cf. export, qui n'encode pas les équipements sélectionnés). Les 400/409 de conflit ou
        // de période invalide déjà gérés par createBooking remontent tels quels.
        Booking saved =
                bookingService.createBooking(
                        params.roomId(), params.dateDebut(), params.dateFin(), params.motif(), List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingMapper.toResponse(
                booking, bookingService.getActiveEquipment(booking), bookingService.countRoomEquipment(booking));
    }
}
