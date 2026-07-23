package dev.vish.nido.auth;

import dev.vish.nido.user.UserRole;
import java.time.Instant;

public record AuthResponse(
        String token,
        Instant expiresAt,
        Long userId,
        String name,
        String email,
        UserRole role
) {
}
