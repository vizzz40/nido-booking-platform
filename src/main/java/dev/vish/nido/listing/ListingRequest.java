package dev.vish.nido.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record ListingRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 2000) String description,
        @NotNull PropertyType propertyType,
        @Min(1) @Max(30) int maxGuests,
        @Min(1) @Max(20) int bedrooms,
        @Min(1) @Max(30) int beds,
        @NotNull @DecimalMin("0.5") BigDecimal bathrooms,
        @NotNull @DecimalMin("1.00") BigDecimal nightlyPrice,
        @NotBlank @Size(max = 1000) String imageUrl,
        @NotNull @Size(max = 20) Set<@NotBlank @Size(max = 60) String> amenities
) {
}
