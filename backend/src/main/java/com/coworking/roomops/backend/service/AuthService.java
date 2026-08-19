package com.coworking.roomops.backend.service;

import com.coworking.roomops.backend.domain.User;
import com.coworking.roomops.backend.repository.UserRepository;
import com.coworking.roomops.backend.security.InvalidTokenException;
import com.coworking.roomops.backend.security.JwtService;
import com.coworking.roomops.backend.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenRevocationService tokenRevocationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    public TokenPair login(String email, String rawPassword) {
        User user =
                userRepository
                        .findByEmail(email)
                        .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                        .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        return new TokenPair(jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user));
    }

    public String refreshAccessToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(refreshToken);
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

        User user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidTokenException("Utilisateur introuvable"));
        return jwtService.generateAccessToken(user);
    }
}
