package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.exception.InvalidIcalFileException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;

public final class IcalMapper {

    // Propriété non-standard (préfixe X- imposé par RFC 5545) portant l'id interne de la salle,
    // pour permettre à POST /bookings/import-ical de retrouver la salle sans dépendre du nom
    // (SUMMARY), qui n'est pas un identifiant fiable.
    static final String X_ROOMOPS_ROOM_ID = "X-ROOMOPS-ROOM-ID";

    private IcalMapper() {}

    public static VEvent toVEvent(Booking booking) {
        Instant start = booking.getDateDebut().toInstant(ZoneOffset.UTC);
        Instant end = booking.getDateFin().toInstant(ZoneOffset.UTC);
        String motif = booking.getMotif();
        String summary = (motif != null && !motif.isBlank()) ? motif : booking.getRoom().getNom();

        VEvent event = new VEvent(start, end, summary);
        event = event.add(new Uid(String.valueOf(booking.getId())));
        event = event.add(new XProperty(X_ROOMOPS_ROOM_ID, String.valueOf(booking.getRoom().getId())));
        return event;
    }

    public static IcalBookingParams toBookingParams(VEvent event) {
        Long roomId =
                event.getPropertyList().<XProperty>getProperty(X_ROOMOPS_ROOM_ID)
                        .map(XProperty::getValue)
                        .map(IcalMapper::parseRoomId)
                        .orElseThrow(
                                () ->
                                        new InvalidIcalFileException(
                                                "Le fichier iCal ne contient pas la propriété " + X_ROOMOPS_ROOM_ID));

        LocalDateTime dateDebut =
                event.<Temporal>getStartDate()
                        .map(DateProperty::getDate)
                        .map(IcalMapper::toUtcLocalDateTime)
                        .orElseThrow(() -> new InvalidIcalFileException("DTSTART manquant dans le fichier iCal"));
        LocalDateTime dateFin =
                event.<Temporal>getEndDate()
                        .map(DateProperty::getDate)
                        .map(IcalMapper::toUtcLocalDateTime)
                        .orElseThrow(() -> new InvalidIcalFileException("DTEND manquant dans le fichier iCal"));

        String motif = event.getSummary() != null ? event.getSummary().getValue() : null;

        return new IcalBookingParams(roomId, dateDebut, dateFin, motif);
    }

    private static Long parseRoomId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidIcalFileException("Propriété " + X_ROOMOPS_ROOM_ID + " invalide : " + value);
        }
    }

    private static LocalDateTime toUtcLocalDateTime(Temporal temporal) {
        if (temporal instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        return LocalDateTime.from(temporal);
    }
}
