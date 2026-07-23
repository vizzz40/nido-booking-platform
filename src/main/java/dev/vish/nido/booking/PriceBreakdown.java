package dev.vish.nido.booking;

import java.math.BigDecimal;

public record PriceBreakdown(
        long nights,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal total
) {
}
