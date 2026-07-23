package dev.vish.nido.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class BookingPricingService {

    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.12");

    public PriceBreakdown calculate(BigDecimal nightlyPrice,
                                    LocalDate checkIn,
                                    LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) {
            throw new IllegalArgumentException("A booking must include at least one night");
        }
        BigDecimal subtotal = nightlyPrice
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = subtotal
                .multiply(SERVICE_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(nights, subtotal, serviceFee, subtotal.add(serviceFee));
    }
}
