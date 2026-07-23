package dev.vish.nido.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingPricingServiceTest {

    private final BookingPricingService pricingService = new BookingPricingService();

    @Test
    void calculatesNightsSubtotalFeeAndTotal() {
        PriceBreakdown result = pricingService.calculate(
                new BigDecimal("125.00"),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14)
        );

        assertThat(result.nights()).isEqualTo(4);
        assertThat(result.subtotal()).isEqualByComparingTo("500.00");
        assertThat(result.serviceFee()).isEqualByComparingTo("60.00");
        assertThat(result.total()).isEqualByComparingTo("560.00");
    }

    @Test
    void roundsTheServiceFeeToCents() {
        PriceBreakdown result = pricingService.calculate(
                new BigDecimal("99.99"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4)
        );

        assertThat(result.serviceFee()).isEqualByComparingTo("36.00");
        assertThat(result.total()).isEqualByComparingTo("335.97");
    }

    @Test
    void rejectsAnEmptyStay() {
        LocalDate date = LocalDate.of(2026, 8, 10);

        assertThatThrownBy(() -> pricingService.calculate(
                new BigDecimal("100.00"), date, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A booking must include at least one night");
    }
}
