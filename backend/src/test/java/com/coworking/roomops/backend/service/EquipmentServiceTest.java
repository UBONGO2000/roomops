package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.BookingStatut;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private EquipmentService equipmentService;

    @Test
    @SuppressWarnings("unchecked")
    void updateStatus_toEnPanne_cancelsFutureBookings() {
        Room room = new Room();
        room.setId(1L);

        Equipment equipment = new Equipment();
        equipment.setId(2L);
        equipment.setRoom(room);
        equipment.setStatut(EquipmentStatut.OPERATIONNEL);

        Booking futureBooking = new Booking();
        futureBooking.setId(3L);
        futureBooking.setStatut(BookingStatut.CONFIRMEE);

        when(equipmentRepository.findById(2L)).thenReturn(Optional.of(equipment));
        when(bookingRepository.findByRoomIdAndStatutAndDateDebutAfter(eq(1L), eq(BookingStatut.CONFIRMEE), any()))
                .thenReturn(List.of(futureBooking));

        EquipmentService.EquipmentStatusUpdateResult result =
                equipmentService.updateStatus(2L, EquipmentStatut.EN_PANNE);

        assertEquals(EquipmentStatut.EN_PANNE, result.equipment().getStatut());
        assertEquals(1, result.cancelledBookingsCount());

        ArgumentCaptor<List<Booking>> savedBookings = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(savedBookings.capture());
        assertEquals(BookingStatut.ANNULEE, savedBookings.getValue().get(0).getStatut());
    }

    @Test
    void updateStatus_toOperationnel_doesNotTouchBookings() {
        Room room = new Room();
        room.setId(1L);

        Equipment equipment = new Equipment();
        equipment.setId(2L);
        equipment.setRoom(room);
        equipment.setStatut(EquipmentStatut.EN_PANNE);

        when(equipmentRepository.findById(2L)).thenReturn(Optional.of(equipment));

        EquipmentService.EquipmentStatusUpdateResult result =
                equipmentService.updateStatus(2L, EquipmentStatut.OPERATIONNEL);

        assertEquals(EquipmentStatut.OPERATIONNEL, result.equipment().getStatut());
        assertEquals(0, result.cancelledBookingsCount());
        verify(bookingRepository, never())
                .findByRoomIdAndStatutAndDateDebutAfter(any(), any(), any(LocalDateTime.class));
        verify(bookingRepository, never()).saveAll(any());
    }
}
