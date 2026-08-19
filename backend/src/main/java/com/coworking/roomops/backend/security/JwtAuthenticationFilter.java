package com.coworking.roomops.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenRevocationService tokenRevocationService) {
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parseAndValidate(token);
                String email = jwtService.extractEmail(claims);
                boolean revoked = tokenRevocationService.isRevoked(email, jwtService.extractIssuedAt(claims));
                if (jwtService.isAccessToken(claims) && !revoked) {
                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority("ROLE_" + jwtService.extractRole(claims));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Token absent/invalide/expiré : on continue non authentifié plutôt que de
                // faire échouer la chaîne de filtres ; Spring Security renverra 401/403
                // lui-même si l'endpoint appelé exige une authentification.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
