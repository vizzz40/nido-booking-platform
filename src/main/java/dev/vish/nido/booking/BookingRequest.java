package dev.vish.nido.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull Long listingId,
        @NotNull @Future LocalDate checkIn,
        @NotNull @Future LocalDate checkOut,
        @Min(1) @Max(30) int guests
) {
}
