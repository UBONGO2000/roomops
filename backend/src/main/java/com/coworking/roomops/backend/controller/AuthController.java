package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.AuthApi;
import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.model.LoginRequest;
import com.coworking.roomops.backend.model.RefreshToken200Response;
import com.coworking.roomops.backend.model.RefreshTokenRequest;
import com.coworking.roomops.backend.model.TokenResponse;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.InvalidTokenException;
import com.coworking.roomops.backend.security.JwtService;
import com.coworking.roomops.backend.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenRevocationService tokenRevocationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        User user =
                userRepository
                        .findByEmail(loginRequest.getEmail())
                        .filter(u -> passwordEncoder.matches(loginRequest.getPassword(), u.getPasswordHash()))
                        .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        TokenResponse response =
                new TokenResponse()
                        .accessToken(jwtService.generateAccessToken(user))
                        .refreshToken(jwtService.generateRefreshToken(user))
                        .tokenType("Bearer");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RefreshToken200Response> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(refreshTokenRequest.getRefreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidTokenException("Ce token n'est pas un refresh token");
        }
        String email = jwtService.extractEmail(claims);
        if (tokenRevocationService.isRevoked(email, jwtService.extractIssuedAt(claims))) {
            throw new InvalidTokenException("Ce token a été révoqué");
        }

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new InvalidTokenException("Utilisateur introuvable"));

        RefreshToken200Response response =
                new RefreshToken200Response().accessToken(jwtService.generateAccessToken(user)).tokenType("Bearer");
        return ResponseEntity.ok(response);
    }
}
