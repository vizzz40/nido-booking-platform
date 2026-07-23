package dev.vish.nido.security;

import dev.vish.nido.user.User;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenDuration;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${nido.security.token-minutes}") long tokenMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.tokenDuration = Duration.of(tokenMinutes, ChronoUnit.MINUTES);
    }

    public Token issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenDuration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nido")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new Token(value, expiresAt);
    }

    public record Token(String value, Instant expiresAt) {
    }
}
