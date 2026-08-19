package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.Room;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.exception.BookingConflictException;
import com.coworking.roomops.backend.exception.InvalidBookingPeriodException;
import com.coworking.roomops.backend.exception.OptimisticLockConflictException;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.EquipmentRepository;
import com.coworking.roomops.backend.repository.RoomRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests unitaires purs (repositories mockés, aucune base réelle) de la logique de conflit de
 * BookingService. Complète, sans le remplacer, BookingConflictIntegrationTest qui prouve la
 * garantie côté base de données.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private BookingService bookingService;

    private User employee;
    private Company company;
    private Room room;
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 15, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 1, 15, 11, 0);

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        employee = new User();
        employee.setId(10L);
        employee.setRole(Role.EMPLOYEE);
        employee.setCompany(company);

        room = new Room();
        room.setId(100L);
        room.setEstActif(true);
    }

    @Test
    void createBooking_success_whenRoomFreeAndBookable() {
        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(room.getId())).thenReturn(List.of());
        when(bookingRepository.existsOverlapping(room.getId(), start, end, null)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createBooking(room.getId(), start, end, "Réunion");

        assertEquals(employee, result.getUser());
        assertEquals(company, result.getCompany());
        assertEquals(room, result.getRoom());
        verify(bookingRepository).flush();
    }

    @Test
    void createBooking_rejectsOverlappingSlot() {
        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(room.getId())).thenReturn(List.of());
        when(bookingRepository.existsOverlapping(room.getId(), start, end, null)).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(room.getId(), start, end, null));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_rejectsInactiveRoom() {
        room.setEstActif(false);
        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(room.getId(), start, end, null));
        verify(bookingRepository, never()).existsOverlapping(anyLong(), any(), any(), any());
    }

    @Test
    void createBooking_rejectsRoomWithBrokenEquipment() {
        Equipment brokenProjector = new Equipment();
        brokenProjector.setType("Projecteur");
        brokenProjector.setStatut(EquipmentStatut.EN_PANNE);

        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(room.getId())).thenReturn(List.of(brokenProjector));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(room.getId(), start, end, null));
    }

    @Test
    void createBooking_rejectsEndBeforeStart() {
        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThrows(
                InvalidBookingPeriodException.class,
                () -> bookingService.createBooking(room.getId(), end, start, null));
        verify(equipmentRepository, never()).findByRoomId(any());
    }

    @Test
    void createBooking_translatesDatabaseConstraintViolationIntoConflict() {
        // Simule la course gagnée par une autre requête entre le contrôle applicatif et
        // l'insertion : la contrainte EXCLUDE de la base rejette l'INSERT.
        when(currentUserProvider.get()).thenReturn(employee);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(equipmentRepository.findByRoomId(room.getId())).thenReturn(List.of());
        when(bookingRepository.existsOverlapping(room.getId(), start, end, null)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("exclusion constraint")).when(bookingRepository).flush();

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(room.getId(), start, end, null));
    }

    @Test
    void getBooking_deniesEmployeeAccessingSomeoneElsesBooking() {
        User otherEmployee = new User();
        otherEmployee.setId(999L);
        otherEmployee.setRole(Role.EMPLOYEE);

        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUser(otherEmployee);
        booking.setCompany(company);

        when(currentUserProvider.get()).thenReturn(employee);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(AccessDeniedException.class, () -> bookingService.getBooking(5L));
    }

    @Test
    void getBooking_allowsManagerAccessingSameCompanyBooking() {
        User manager = new User();
        manager.setId(20L);
        manager.setRole(Role.MANAGER);
        manager.setCompany(company);

        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUser(employee);
        booking.setCompany(company);

        when(currentUserProvider.get()).thenReturn(manager);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertEquals(booking, bookingService.getBooking(5L));
    }

    @Test
    void updateBooking_rejectsStaleVersion() {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setUser(employee);
        booking.setCompany(company);
        booking.setRoom(room);
        booking.setDateDebut(start);
        booking.setDateFin(end);
        booking.setVersion(5L);

        when(currentUserProvider.get()).thenReturn(employee);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(
                OptimisticLockConflictException.class,
                () -> bookingService.updateBooking(5L, null, null, null, null, 4L));
        verify(bookingRepository, never()).save(any());
    }
}
