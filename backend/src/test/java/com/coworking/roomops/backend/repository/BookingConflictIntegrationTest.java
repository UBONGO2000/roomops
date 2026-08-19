package com.coworking.roomops.backend.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preuve d'exécution de la contrainte no_overlapping_booking (EXCLUDE USING GIST,
 * V1__create_initial_schema.sql) : les insertions ci-dessous passent directement par le
 * repository, sans passer par BookingController.createBooking, donc sans son contrôle
 * applicatif de recouvrement. Seule la base de données peut encore rejeter le conflit.
 */
@SpringBootTest
@Transactional
class BookingConflictIntegrationTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;

    private Room room;
    private User employee;

    @BeforeEach
    void setUp() {
        room = roomRepository.findAll().stream()
                .filter(r -> "Salle Alpha".equals(r.getNom()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Alpha"));

        Company company = new Company();
        company.setNom("Test Corp");
        company = companyRepository.save(company);

        User user = new User();
        user.setEmail("test.employee." + System.nanoTime() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setNom("Test");
        user.setPrenom("Employee");
        user.setRole(Role.EMPLOYEE);
        user.setCompany(company);
        employee = userRepository.save(user);
    }

    @Test
    void databaseRejectsOverlappingBookingEvenWithoutApplicationCheck() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2030, 1, 15, 11, 0);

        bookingRepository.saveAndFlush(newBooking(start, end));

        // Chevauchement partiel (9h30-11h30) inséré directement au niveau repository.
        Booking overlapping = newBooking(start.plusMinutes(30), end.plusMinutes(30));

        assertThrows(DataIntegrityViolationException.class, () -> bookingRepository.saveAndFlush(overlapping));
    }

    @Test
    void databaseAllowsAdjacentNonOverlappingBookings() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 16, 9, 0);
        LocalDateTime end = LocalDateTime.of(2030, 1, 16, 11, 0);

        bookingRepository.saveAndFlush(newBooking(start, end));

        // Créneau immédiatement consécutif (11h-12h), ne chevauche pas : doit être accepté.
        assertDoesNotThrow(() -> bookingRepository.saveAndFlush(newBooking(end, end.plusHours(1))));
    }

    private Booking newBooking(LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setUser(employee);
        booking.setRoom(room);
        booking.setCompany(employee.getCompany());
        booking.setDateDebut(start);
        booking.setDateFin(end);
        return booking;
    }
}
