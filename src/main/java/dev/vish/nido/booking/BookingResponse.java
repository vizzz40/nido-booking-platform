package dev.vish.nido.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long listingId,
        String listingTitle,
        String listingCity,
        String listingImageUrl,
        Long guestId,
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        long nights,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal totalPrice,
        BookingStatus status,
        Instant createdAt
) {
}
