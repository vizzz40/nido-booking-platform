package dev.vish.nido.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.vish.nido.common.ConflictException;
import dev.vish.nido.listing.Listing;
import dev.vish.nido.listing.ListingRepository;
import dev.vish.nido.listing.PropertyType;
import dev.vish.nido.security.CurrentUserService;
import dev.vish.nido.user.User;
import dev.vish.nido.user.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CurrentUserService currentUserService;

    private BookingService bookingService;
    private User host;
    private User guest;
    private Listing listing;

    @BeforeEach
    void setUp() {
        BookingPricingService pricingService = new BookingPricingService();
        bookingService = new BookingService(
                bookingRepository, listingRepository, pricingService, currentUserService);
        host = user(1L, "Host", "host@nido.dev", UserRole.HOST);
        guest = user(2L, "Guest", "guest@nido.dev", UserRole.GUEST);
        listing = new Listing(
                host,
                "Lake cabin",
                "Como",
                "Italy",
                "A cabin by the lake",
                PropertyType.CABIN,
                4,
                2,
                2,
                new BigDecimal("1.0"),
                new BigDecimal("150.00"),
                "/images/como.svg",
                Set.of("Wi-Fi")
        );
        ReflectionTestUtils.setField(listing, "id", 8L);
    }

    @Test
    void createsAConfirmedBookingWithCalculatedTotal() {
        BookingRequest request = futureRequest(2);
        when(currentUserService.get()).thenReturn(guest);
        when(listingRepository.findLockedById(8L)).thenReturn(Optional.of(listing));
        when(bookingRepository.hasOverlappingBooking(
                8L, request.checkIn(), request.checkOut())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", 10L);
            return booking;
        });

        BookingResponse response = bookingService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.nights()).isEqualTo(3);
        assertThat(response.subtotal()).isEqualByComparingTo("450.00");
        assertThat(response.serviceFee()).isEqualByComparingTo("54.00");
        assertThat(response.totalPrice()).isEqualByComparingTo("504.00");
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void rejectsAnOverlappingBooking() {
        BookingRequest request = futureRequest(2);
        when(currentUserService.get()).thenReturn(guest);
        when(listingRepository.findLockedById(8L)).thenReturn(Optional.of(listing));
        when(bookingRepository.hasOverlappingBooking(
                8L, request.checkIn(), request.checkOut())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("The listing is unavailable for these dates");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void preventsTheHostBookingTheirOwnPlace() {
        BookingRequest request = futureRequest(2);
        when(currentUserService.get()).thenReturn(host);
        when(listingRepository.findLockedById(8L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Hosts cannot book their own listing");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsTooManyGuests() {
        BookingRequest request = futureRequest(5);
        when(currentUserService.get()).thenReturn(guest);
        when(listingRepository.findLockedById(8L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Guest count exceeds the listing capacity");
        verify(bookingRepository, never()).save(any());
    }

    private BookingRequest futureRequest(int guests) {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        return new BookingRequest(8L, checkIn, checkIn.plusDays(3), guests);
    }

    private User user(Long id, String name, String email, UserRole role) {
        User user = new User(name, email, "encoded", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
