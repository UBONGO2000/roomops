package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.Booking;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import com.coworking.roomops.backend.security.TokenRevocationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenRevocationService tokenRevocationService;

    @InjectMocks private UserService userService;

    @Test
    void getCurrentUser_delegatesToProvider() {
        User user = new User();
        when(currentUserProvider.get()).thenReturn(user);

        assertEquals(user, userService.getCurrentUser());
    }

    @Test
    void exportCurrentUserData_includesOwnBookingsOnly() {
        User user = new User();
        user.setId(10L);
        Booking booking = new Booking();
        booking.setId(1L);

        when(currentUserProvider.get()).thenReturn(user);
        when(bookingRepository.findByUserId(10L)).thenReturn(List.of(booking));

        PersonalDataExport export = userService.exportCurrentUserData();

        assertEquals(user, export.user());
        assertEquals(1, export.bookings().size());
        assertEquals(booking, export.bookings().get(0));
    }

    @Test
    void anonymizeCurrentUser_scrubsFieldsAndRevokesTheOriginalEmail() {
        User user = new User();
        user.setId(42L);
        user.setEmail("jean.dupont@techcorp.com");
        user.setNom("Dupont");
        user.setPrenom("Jean");

        when(currentUserProvider.get()).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("random-hash");

        userService.anonymizeCurrentUser();

        assertEquals("deleted-user-42@roomops.local", user.getEmail());
        assertEquals("Utilisateur", user.getNom());
        assertEquals("Supprimé", user.getPrenom());
        assertEquals("random-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        // La révocation doit cibler l'email D'ORIGINE (celui présent dans les tokens déjà
        // émis), pas le nouvel email anonymisé qui vient d'être écrit sur l'entité.
        verify(tokenRevocationService).revokeAllTokensFor("jean.dupont@techcorp.com");
    }
}
