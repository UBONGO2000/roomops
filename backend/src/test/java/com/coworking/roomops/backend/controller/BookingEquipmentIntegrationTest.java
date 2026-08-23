package com.coworking.roomops.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.model.BookingRequest;
import com.coworking.roomops.backend.model.BookingResponse;
import com.coworking.roomops.backend.repository.CompanyRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import java.time.OffsetDateTime;
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
 * Preuve bout-en-bout (vrai PostgreSQL) que l'éco-toggle (BookingRequest.equipmentIds) est
 * réellement persisté dans booking_equipment et correctement resérialisé : crée une réservation
 * en ne sollicitant qu'une partie des équipements de la salle, puis relit via GET /bookings/{id}
 * pour vérifier que equipmentsActives et ecoScore reflètent l'état stocké, pas seulement la
 * réponse de création.
 */
@SpringBootTest
@Transactional
class BookingEquipmentIntegrationTest {

    @Autowired private BookingController bookingController;
    @Autowired private RoomRepository roomRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;

    private Room room;
    private Equipment requestedEquipment;
    private int totalRoomEquipment;

    @BeforeEach
    void setUp() {
        room =
                roomRepository.findAll().stream()
                        .filter(r -> "Salle Alpha".equals(r.getNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Seed V3 introuvable : Salle Alpha"));

        List<Equipment> roomEquipment = equipmentRepository.findByRoomId(room.getId());
        // Précondition explicite : le test suppose Salle Alpha équipée d'au moins deux
        // équipements (Projecteur + Visioconférence, V3), pour distinguer un ecoScore < 100 d'un
        // score obtenu par accident (aucun équipement à ne pas activer).
        if (roomEquipment.size() < 2) {
            throw new IllegalStateException("Seed V3 introuvable : Salle Alpha doit avoir au moins 2 équipements");
        }
        totalRoomEquipment = roomEquipment.size();
        requestedEquipment = roomEquipment.get(0);

        Company company = new Company();
        company.setNom("Eco Toggle Test Corp");
        company = companyRepository.save(company);

        User user = new User();
        user.setEmail("eco.toggle.test." + System.nanoTime() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setNom("Test");
        user.setPrenom("EcoToggle");
        user.setRole(Role.EMPLOYEE);
        user.setCompany(company);
        User employee = userRepository.save(user);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                employee.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    @Test
    void createdBooking_equipmentsActivesAndEcoScore_survivePersistenceAndReread() {
        OffsetDateTime start = OffsetDateTime.of(2032, 3, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2032, 3, 10, 11, 0, 0, 0, ZoneOffset.UTC);

        BookingRequest request =
                new BookingRequest()
                        .roomId(room.getId())
                        .dateDebut(start)
                        .dateFin(end)
                        .motif("Test éco-toggle")
                        .equipmentIds(List.of(requestedEquipment.getId()));

        BookingResponse created = bookingController.createBooking(request).getBody();
        assertEcoToggleFields(created);

        // La relecture doit refléter ce qui est réellement en base (booking_equipment), pas
        // seulement la réponse de création : preuve que la persistance a eu lieu.
        BookingResponse reread = bookingController.getBookingById(created.getId()).getBody();
        assertEcoToggleFields(reread);
    }

    private void assertEcoToggleFields(BookingResponse response) {
        assertEquals(1, response.getEquipmentsActives().size());
        assertEquals(requestedEquipment.getId(), response.getEquipmentsActives().get(0).getId());
        assertEquals(requestedEquipment.getType(), response.getEquipmentsActives().get(0).getType());

        int expectedEcoScore = (int) Math.round(100.0 * (totalRoomEquipment - 1) / totalRoomEquipment);
        assertEquals(expectedEcoScore, response.getEcoScore());
        assertTrue(response.getEcoScore() < 100, "ecoScore doit être < 100 puisqu'un équipement est activé");
    }
}
