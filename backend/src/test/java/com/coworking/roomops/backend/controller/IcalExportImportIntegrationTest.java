package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.InvalidIcalFileException;
import com.coworking.roomops.backend.model.BookingRequest;
import com.coworking.roomops.backend.model.BookingResponse;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preuve bout-en-bout (vrai PostgreSQL) du round-trip export/import iCal : GET
 * /bookings/{id}/ical produit un fichier que POST /bookings/import-ical accepte et qui recrée
 * une réservation pointant vers la même salle et les mêmes horaires ; un fichier sans la
 * propriété X-ROOMOPS-ROOM-ID (non émis par RoomOps) est rejeté en 400.
 */
@SpringBootTest
@Transactional
class IcalExportImportIntegrationTest {

    @Autowired private BookingController bookingController;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        room =
                roomRepository.findAll().stream()
                        .filter(r -> "Salle Beta".equals(r.getNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Beta"));

        Company company = new Company();
        company.setNom("Ical Test Corp");
        company = companyRepository.save(company);

        User user = new User();
        user.setEmail("ical.test." + System.nanoTime() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setNom("Test");
        user.setPrenom("Ical");
        user.setRole(Role.EMPLOYEE);
        user.setCompany(company);
        User employee = userRepository.save(user);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                employee.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    @Test
    void exportThenReimport_recreatesBookingWithSameRoomAndSchedule() throws IOException {
        OffsetDateTime start = OffsetDateTime.of(2033, 4, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2033, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        BookingRequest request = new BookingRequest().roomId(room.getId()).dateDebut(start).dateFin(end).motif("Export test");
        BookingResponse created = bookingController.createBooking(request).getBody();

        byte[] ics = bookingController.exportBookingIcal(created.getId()).getBody().getInputStream().readAllBytes();
        String icsText = new String(ics, StandardCharsets.UTF_8);
        // Vérifie explicitement que le fichier produit est un iCal valide (VERSION:2.0, pas
        // juste "VERSION:" vide) : ical4j reste tolérant à la relecture d'un fichier malformé,
        // ce qui masquerait la régression sans cette assertion.
        assertTrue(icsText.contains("VERSION:2.0"), "le fichier exporté doit déclarer VERSION:2.0");

        // Libère le créneau avant réimport : sinon la réservation encore active provoquerait un
        // 409 (le round-trip teste la fidélité des données, pas la double réservation).
        bookingController.cancelBooking(created.getId());

        BookingResponse reimported = bookingController.importBookingIcal(new ByteArrayResource(ics)).getBody();

        assertNotEquals(created.getId(), reimported.getId());
        assertEquals(created.getRoomId(), reimported.getRoomId());
        assertEquals(created.getDateDebut(), reimported.getDateDebut());
        assertEquals(created.getDateFin(), reimported.getDateFin());
    }

    @Test
    void import_rejectsIcalFileWithoutRoomOpsRoomIdProperty() {
        String genericIcal =
                String.join(
                        "\r\n",
                        "BEGIN:VCALENDAR",
                        "VERSION:2.0",
                        "PRODID:-//Generic//Generic//EN",
                        "BEGIN:VEVENT",
                        "UID:generic-event-1",
                        "DTSTART:20330101T090000Z",
                        "DTEND:20330101T100000Z",
                        "SUMMARY:Evenement generique",
                        "END:VEVENT",
                        "END:VCALENDAR",
                        "");
        ByteArrayResource genericFile = new ByteArrayResource(genericIcal.getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidIcalFileException.class, () -> bookingController.importBookingIcal(genericFile));
    }
}
