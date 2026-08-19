package com.coworking.roomops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.InvalidTokenException;
import com.coworking.roomops.backend.security.JwtService;
import com.coworking.roomops.backend.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "jean.dupont@techcorp.com";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenRevocationService tokenRevocationService;
    @Mock private Claims claims;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed");
    }

    @Test
    void login_success_returnsTokenPair() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        TokenPair tokens = authService.login(EMAIL, "secret");

        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(EMAIL, "wrong"));
    }

    @Test
    void login_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login("ghost@example.com", "whatever"));
    }

    @Test
    void refreshAccessToken_success_issuesNewAccessToken() {
        when(jwtService.parseAndValidate("refresh-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(jwtService.extractEmail(claims)).thenReturn(EMAIL);
        when(jwtService.extractIssuedAt(claims)).thenReturn(Instant.now());
        when(tokenRevocationService.isRevoked(eq(EMAIL), any())).thenReturn(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        assertEquals("new-access-token", authService.refreshAccessToken("refresh-token"));
    }

    @Test
    void refreshAccessToken_malformedToken_throwsInvalidTokenException() {
        when(jwtService.parseAndValidate("garbage")).thenThrow(new MalformedJwtException("bad token"));

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken("garbage"));
    }

    @Test
    void refreshAccessToken_notARefreshToken_throwsInvalidTokenException() {
        when(jwtService.parseAndValidate("access-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken("access-token"));
    }

    @Test
    void refreshAccessToken_revokedToken_throwsInvalidTokenException() {
        when(jwtService.parseAndValidate("refresh-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(jwtService.extractEmail(claims)).thenReturn(EMAIL);
        when(jwtService.extractIssuedAt(claims)).thenReturn(Instant.now());
        when(tokenRevocationService.isRevoked(eq(EMAIL), any())).thenReturn(true);

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken("refresh-token"));
    }
}
