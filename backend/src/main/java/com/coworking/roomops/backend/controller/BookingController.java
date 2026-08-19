package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.BookingsApi;
import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.mapper.BookingMapper;
import com.coworking.roomops.backend.mapper.DateTimeMapper;
import com.coworking.roomops.backend.model.BookingPageResponse;
import com.coworking.roomops.backend.model.BookingRequest;
import com.coworking.roomops.backend.model.BookingResponse;
import com.coworking.roomops.backend.model.BookingUpdateRequest;
import com.coworking.roomops.backend.service.BookingService;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController implements BookingsApi {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(BookingRequest bookingRequest) {
        Booking saved =
                bookingService.createBooking(
                        bookingRequest.getRoomId(),
                        DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateDebut()),
                        DateTimeMapper.toUtcLocalDateTime(bookingRequest.getDateFin()),
                        bookingRequest.getMotif());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<BookingResponse> getBookingById(Long id) {
        return ResponseEntity.ok(BookingMapper.toResponse(bookingService.getBooking(id)));
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
                        .content(result.getContent().stream().map(BookingMapper::toResponse).toList())
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
                        bookingUpdateRequest.getVersion());
        return ResponseEntity.ok(BookingMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<Void> cancelBooking(Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
