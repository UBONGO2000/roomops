package com.coworking.roomops.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coworking.roomops.backend.domain.Role;
import com.coworking.roomops.backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dwlr18CVc9Fv5XYAy7XY1h93alLRHn91HUfD13DbKd8c=";

    private User someUser() {
        User user = new User();
        user.setEmail("jean.dupont@techcorp.com");
        user.setRole(Role.MANAGER);
        return user;
    }

    @Test
    void accessToken_roundTripsClaims_andIsRecognizedAsAccessToken() {
        JwtService jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        User user = someUser();

        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseAndValidate(token);

        assertEquals(user.getEmail(), jwtService.extractEmail(claims));
        assertEquals(user.getRole().name(), jwtService.extractRole(claims));
        assertTrue(jwtService.isAccessToken(claims));
        assertFalse(jwtService.isRefreshToken(claims));
    }

    @Test
    void refreshToken_isRecognizedAsRefreshToken_notAccessToken() {
        JwtService jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        User user = someUser();

        String token = jwtService.generateRefreshToken(user);
        Claims claims = jwtService.parseAndValidate(token);

        assertTrue(jwtService.isRefreshToken(claims));
        assertFalse(jwtService.isAccessToken(claims));
    }

    @Test
    void parseAndValidate_rejectsGarbageToken() {
        JwtService jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);

        assertThrows(JwtException.class, () -> jwtService.parseAndValidate("not-a-jwt"));
    }

    @Test
    void parseAndValidate_rejectsExpiredToken() {
        // TTL négatif : le token est déjà expiré au moment où il est émis.
        JwtService jwtService = new JwtService(SECRET, -1_000L, 604_800_000L);
        String token = jwtService.generateAccessToken(someUser());

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseAndValidate(token));
    }
}
