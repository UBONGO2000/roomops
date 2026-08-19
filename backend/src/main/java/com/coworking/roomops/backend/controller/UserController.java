package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.UsersApi;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.mapper.BookingMapper;
import com.coworking.roomops.backend.mapper.UserMapper;
import com.coworking.roomops.backend.model.UserDataExport;
import com.coworking.roomops.backend.model.UserResponse;
import com.coworking.roomops.backend.repository.BookingRepository;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.CurrentUserProvider;
import com.coworking.roomops.backend.security.TokenRevocationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UsersApi {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;

    public UserController(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            TokenRevocationService tokenRevocationService) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserMapper.toResponse(currentUserProvider.get()));
    }

    @Override
    public ResponseEntity<UserDataExport> exportUserData() {
        User user = currentUserProvider.get();
        UserDataExport export =
                new UserDataExport()
                        .user(UserMapper.toResponse(user))
                        .bookings(
                                bookingRepository.findByUserId(user.getId()).stream()
                                        .map(BookingMapper::toResponse)
                                        .toList())
                        .exportDate(OffsetDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.ok(export);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> anonymizeUser() {
        User user = currentUserProvider.get();
        // Capturé avant la mutation : c'est cet email (celui présent dans tout token déjà
        // émis) qui doit être révoqué, pas le futur email anonymisé.
        String originalEmail = user.getEmail();

        // Les réservations (Booking) sont conservées pour préserver l'intégrité de
        // l'historique d'occupation des salles (booking.user_id est NOT NULL), mais elles
        // sont détachées de toute donnée personnelle identifiante puisque le compte User
        // lui-même n'en porte plus.
        user.setEmail("deleted-user-" + user.getId() + "@roomops.local");
        user.setNom("Utilisateur");
        user.setPrenom("Supprimé");
        // Mot de passe aléatoire et jamais communiqué : rend le compte définitivement
        // non-connectable, même si l'email anonymisé venait à être découvert.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(user);

        tokenRevocationService.revokeAllTokensFor(originalEmail);

        return ResponseEntity.noContent().build();
    }
}
