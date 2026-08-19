package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private RoomService roomService;

    private Room room;
    private final LocalDateTime start = LocalDateTime.of(2030, 2, 1, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 2, 1, 10, 0);

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(1L);
        room.setNom("Salle Test");
    }

    @Test
    void checkAvailability_availableWhenNoPanneAndNoOverlap() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(1L)).thenReturn(List.of());
        when(bookingRepository.existsOverlapping(1L, start, end, null)).thenReturn(false);

        RoomAvailability availability = roomService.checkAvailability(1L, start, end);

        assertTrue(availability.available());
        assertNull(availability.reason());
    }

    @Test
    void checkAvailability_unavailableWhenEquipmentBroken() {
        Equipment broken = new Equipment();
        broken.setType("Visioconférence");
        broken.setStatut(EquipmentStatut.EN_PANNE);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(1L)).thenReturn(List.of(broken));

        RoomAvailability availability = roomService.checkAvailability(1L, start, end);

        assertFalse(availability.available());
        assertEquals("Panne : Visioconférence", availability.reason());
        // Court-circuit attendu : pas besoin d'interroger les réservations si la salle est
        // déjà indisponible pour cause de panne.
        verify(bookingRepository, never()).existsOverlapping(any(), any(), any(), any());
    }

    @Test
    void checkAvailability_unavailableWhenOverlapping() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(1L)).thenReturn(List.of());
        when(bookingRepository.existsOverlapping(1L, start, end, null)).thenReturn(true);

        RoomAvailability availability = roomService.checkAvailability(1L, start, end);

        assertFalse(availability.available());
        assertEquals("Créneau déjà réservé", availability.reason());
    }

    @Test
    void getRoom_unknownId_throwsNotFound() {
        when(roomRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roomService.getRoom(404L));
    }
}
