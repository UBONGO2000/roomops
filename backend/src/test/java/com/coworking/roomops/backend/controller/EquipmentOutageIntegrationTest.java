package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.BookingStatut;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.model.EquipmentStatusUpdateResponse;
import com.coworking.roomops.backend.model.EquipmentStatut;
import com.coworking.roomops.backend.model.UpdateEquipmentStatusRequest;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vérifie la règle métier de V3/PUT /equipments/{id}/status : déclarer une panne annule les
 * réservations CONFIRMEE futures de la salle concernée, mais laisse intactes celles déjà
 * passées.
 */
@SpringBootTest
@Transactional
class EquipmentOutageIntegrationTest {

    @Autowired private EquipmentController equipmentController;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;

    private com.coworking.roomops.backend.domain.Equipment equipment;
    private Booking futureBooking;
    private Booking pastBooking;

    @BeforeEach
    void setUp() {
        Room room =
                roomRepository.findAll().stream()
                        .filter(r -> "Salle Beta".equals(r.getNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Beta"));
        equipment = equipmentRepository.findByRoomId(room.getId()).get(0);

        Company company = new Company();
        company.setNom("Outage Test Corp");
        company = companyRepository.save(company);

        User user = new User();
        user.setEmail("outage.test." + System.nanoTime() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setNom("Test");
        user.setPrenom("Outage");
        user.setRole(Role.EMPLOYEE);
        user.setCompany(company);
        User employee = userRepository.save(user);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        futureBooking = bookingRepository.saveAndFlush(newBooking(room, employee, now.plusDays(1), now.plusDays(1).plusHours(1)));
        pastBooking = bookingRepository.saveAndFlush(newBooking(room, employee, now.minusDays(1), now.minusDays(1).plusHours(1)));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void declaringPanneCancelsFutureBookingButNotPastOne() {
        // Précondition explicite : sans une réservation future réellement CONFIRMEE, l'assertion
        // reservationsAnnulees == 1 plus bas passerait pour une mauvaise raison (aucune réservation
        // à annuler plutôt qu'une réservation correctement annulée).
        assertEquals(BookingStatut.CONFIRMEE, futureBooking.getStatut());

        EquipmentStatusUpdateResponse response =
                equipmentController
                        .updateEquipmentStatus(
                                equipment.getId(), new UpdateEquipmentStatusRequest(EquipmentStatut.EN_PANNE))
                        .getBody();

        Booking reloadedFuture = bookingRepository.findById(futureBooking.getId()).orElseThrow();
        Booking reloadedPast = bookingRepository.findById(pastBooking.getId()).orElseThrow();

        assertEquals(BookingStatut.ANNULEE, reloadedFuture.getStatut());
        assertEquals(BookingStatut.CONFIRMEE, reloadedPast.getStatut());
        assertEquals(
                com.coworking.roomops.backend.domain.EquipmentStatut.EN_PANNE,
                equipmentRepository.findById(equipment.getId()).orElseThrow().getStatut());
        assertEquals(1, response.getReservationsAnnulees());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void declaringPanneOnRoomWithNoFutureBookingsReportsZeroCancellations() {
        // Salle Gamma n'a aucune réservation créée dans setUp() : le compte doit refléter cette
        // absence, pas juste retourner une valeur par défaut qu'on ne testerait jamais à zéro.
        Room roomWithoutBookings =
                roomRepository.findAll().stream()
                        .filter(r -> "Salle Gamma".equals(r.getNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Gamma"));
        com.coworking.roomops.backend.domain.Equipment equipmentWithoutBookings =
                equipmentRepository.findByRoomId(roomWithoutBookings.getId()).get(0);

        EquipmentStatusUpdateResponse response =
                equipmentController
                        .updateEquipmentStatus(
                                equipmentWithoutBookings.getId(),
                                new UpdateEquipmentStatusRequest(EquipmentStatut.EN_PANNE))
                        .getBody();

        assertEquals(0, response.getReservationsAnnulees());
    }

    private Booking newBooking(Room room, User user, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCompany(user.getCompany());
        booking.setDateDebut(start);
        booking.setDateFin(end);
        booking.setStatut(BookingStatut.CONFIRMEE);
        return booking;
    }
}
