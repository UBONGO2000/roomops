package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
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
        equipmentController.updateEquipmentStatus(
                equipment.getId(), new UpdateEquipmentStatusRequest(EquipmentStatut.EN_PANNE));

        Booking reloadedFuture = bookingRepository.findById(futureBooking.getId()).orElseThrow();
        Booking reloadedPast = bookingRepository.findById(pastBooking.getId()).orElseThrow();

        assertEquals(
                com.coworking.roomops.backend.domain.BookingStatut.ANNULEE, reloadedFuture.getStatut());
        assertEquals(
                com.coworking.roomops.backend.domain.BookingStatut.CONFIRMEE, reloadedPast.getStatut());
        assertEquals(
                com.coworking.roomops.backend.domain.EquipmentStatut.EN_PANNE,
                equipmentRepository.findById(equipment.getId()).orElseThrow().getStatut());
    }

    private Booking newBooking(Room room, User user, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCompany(user.getCompany());
        booking.setDateDebut(start);
        booking.setDateFin(end);
        return booking;
    }
}
