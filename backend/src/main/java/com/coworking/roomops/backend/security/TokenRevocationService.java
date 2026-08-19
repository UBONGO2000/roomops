package com.coworking.roomops.backend.security;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Liste de révocation de tokens, adossée à Redis. On ne stocke pas les tokens eux-mêmes : une
 * seule entrée par email, valant "tout token émis avant cet instant est invalide". C'est
 * volontairement grossier (un access ET un refresh token émis avant l'anonymisation sont tous
 * les deux révoqués, même l'access token qui aurait expiré tout seul dans les 15 minutes) mais
 * suffisant pour le droit à l'oubli : l'objectif est qu'aucun token émis avant l'anonymisation
 * ne reste utilisable après.
 */
@Component
public class TokenRevocationService {

    private static final String KEY_PREFIX = "revoked-tokens:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenTtlMillis;

    public TokenRevocationService(
            StringRedisTemplate redisTemplate,
            @Value("${roomops.security.jwt.refresh-token-ttl-ms}") long refreshTokenTtlMillis) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtlMillis = refreshTokenTtlMillis;
    }

    public void revokeAllTokensFor(String email) {
        // TTL = durée de vie max d'un refresh token : passé ce délai, tout token concerné par
        // cette révocation aurait expiré naturellement de toute façon, l'entrée peut disparaître.
        redisTemplate
                .opsForValue()
                .set(KEY_PREFIX + email, Instant.now().toString(), refreshTokenTtlMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isRevoked(String email, Instant tokenIssuedAt) {
        String revokedAt = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        if (revokedAt == null) {
            return false;
        }
        return !tokenIssuedAt.isAfter(Instant.parse(revokedAt));
    }
}
