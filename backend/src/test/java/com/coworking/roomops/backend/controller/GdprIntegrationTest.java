package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.JwtService;
import com.coworking.roomops.backend.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vérifie /users/export (données du seul utilisateur connecté) et /users/anonymize (les champs
 * personnels sont scrubés, mais la réservation reste rattachée au même User.id — elle n'est ni
 * supprimée ni orpheline).
 */
@SpringBootTest
@Transactional
class GdprIntegrationTest {

    @Autowired private UserController userController;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JwtService jwtService;
    @Autowired private TokenRevocationService tokenRevocationService;

    private User employee;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Room room =
                roomRepository.findAll().stream()
                        .filter(r -> "Salle Gamma".equals(r.getNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Gamma"));

        Company company = new Company();
        company.setNom("Gdpr Test Corp");
        company = companyRepository.save(company);

        User user = new User();
        user.setEmail("gdpr.test." + System.nanoTime() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setNom("Dupont");
        user.setPrenom("Jean");
        user.setRole(Role.EMPLOYEE);
        user.setCompany(company);
        employee = userRepository.save(user);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Booking b = new Booking();
        b.setUser(employee);
        b.setRoom(room);
        b.setCompany(company);
        b.setDateDebut(now.plusDays(2));
        b.setDateFin(now.plusDays(2).plusHours(1));
        booking = bookingRepository.saveAndFlush(b);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                employee.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    @Test
    void exportUserData_returnsOwnProfileAndOwnBookingsOnly() {
        var export = userController.exportUserData().getBody();

        assertEquals(employee.getEmail(), export.getUser().getEmail());
        assertEquals(1, export.getBookings().size());
        assertEquals(booking.getId(), export.getBookings().get(0).getId());
    }

    @Test
    void anonymizeUser_scrubsPersonalFields_butKeepsBookingLinked() {
        userController.anonymizeUser();

        User reloaded = userRepository.findById(employee.getId()).orElseThrow();
        assertTrue(reloaded.getEmail().startsWith("deleted-user-" + employee.getId()));
        assertEquals("Utilisateur", reloaded.getNom());
        assertEquals("Supprimé", reloaded.getPrenom());

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(employee.getId(), reloadedBooking.getUser().getId());
    }

    @Test
    void anonymizeUser_revokesTokensIssuedBeforeAnonymization() {
        String originalEmail = employee.getEmail();
        Claims tokenClaimsBeforeAnonymize = jwtService.parseAndValidate(jwtService.generateAccessToken(employee));

        userController.anonymizeUser();

        assertTrue(
                tokenRevocationService.isRevoked(
                        originalEmail, jwtService.extractIssuedAt(tokenClaimsBeforeAnonymize)));
    }
}
