package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.exception.InvalidIcalFileException;
import com.coworking.roomops.backend.mapper.IcalBookingParams;
import com.coworking.roomops.backend.mapper.IcalMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;
import org.springframework.stereotype.Service;

/**
 * Orchestration ical4j (lecture/écriture de flux, construction du VCALENDAR) : la conversion
 * Booking <-> VEVENT elle-même vit dans IcalMapper.
 */
@Service
public class IcalService {

    public byte[] export(Booking booking) {
        VEvent event = IcalMapper.toVEvent(booking);

        // Version n'a pas de constructeur à un seul argument : le no-arg laisse la valeur vide
        // ("VERSION:" au lieu de "VERSION:2.0"), un fichier iCal invalide bien que le parseur
        // ical4j reste tolérant à la relecture. setValue explicite obligatoire.
        Version version = new Version();
        version.setValue(Version.VALUE_2_0);

        Calendar calendar = new Calendar();
        calendar = calendar.add(new ProdId("-//RoomOps//iCal Export//FR"));
        calendar = calendar.add(version);
        calendar = calendar.add(new CalScale(CalScale.VALUE_GREGORIAN));
        calendar = calendar.add(event);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new CalendarOutputter().output(calendar, out);
        } catch (IOException | ValidationException e) {
            // Ne peut arriver qu'en cas de bug de construction du VEVENT ci-dessus (calendrier
            // toujours valide par construction) : pas un cas d'erreur utilisateur, donc pas
            // traduit en InvalidIcalFileException.
            throw new IllegalStateException("Échec de sérialisation du fichier iCal", e);
        }
        return out.toByteArray();
    }

    public IcalBookingParams parse(InputStream input) {
        Calendar calendar;
        try {
            calendar = new CalendarBuilder().build(input);
        } catch (IOException | ParserException e) {
            throw new InvalidIcalFileException("Fichier iCal invalide ou illisible");
        }

        List<VEvent> events = calendar.getComponentList().getComponents(Component.VEVENT);
        if (events.isEmpty()) {
            throw new InvalidIcalFileException("Le fichier iCal ne contient aucun VEVENT");
        }
        return IcalMapper.toBookingParams(events.get(0));
    }
}
