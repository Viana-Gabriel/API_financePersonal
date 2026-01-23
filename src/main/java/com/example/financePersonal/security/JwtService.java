package com.example.financePersonal.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(email)
                .withClaim("uid", userId.toString())
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    /** Lança exceção se inválido/expirado */
    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }

    public String extractEmail(DecodedJWT jwt) {
        return jwt.getSubject();
    }

    public UUID extractUserId(DecodedJWT jwt) {
        String uid = jwt.getClaim("uid").asString();
        return UUID.fromString(uid);
    }
}
