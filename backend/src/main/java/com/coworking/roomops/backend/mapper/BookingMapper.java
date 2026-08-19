package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.model.BookingResponse;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingResponse toResponse(Booking booking) {
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
                .version(booking.getVersion());
    }
}
