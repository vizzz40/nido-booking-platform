package dev.vish.nido.listing;

import java.math.BigDecimal;
import java.util.Set;

public record ListingResponse(
        Long id,
        Long hostId,
        String hostName,
        String title,
        String city,
        String country,
        String description,
        PropertyType propertyType,
        int maxGuests,
        int bedrooms,
        int beds,
        BigDecimal bathrooms,
        BigDecimal nightlyPrice,
        String imageUrl,
        Set<String> amenities,
        Double averageRating,
        long reviewCount
) {
}
