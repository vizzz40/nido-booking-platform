package dev.vish.nido.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookingId,
        Long listingId,
        Long authorId,
        String authorName,
        int rating,
        String comment,
        Instant createdAt
) {
}
