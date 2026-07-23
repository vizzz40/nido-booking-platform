package dev.vish.nido.listing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListingSearch(
        String location,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guests,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
