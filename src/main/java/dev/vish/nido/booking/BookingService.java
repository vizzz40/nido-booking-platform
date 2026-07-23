package dev.vish.nido.booking;

import dev.vish.nido.common.ConflictException;
import dev.vish.nido.common.ForbiddenOperationException;
import dev.vish.nido.common.NotFoundException;
import dev.vish.nido.listing.Listing;
import dev.vish.nido.listing.ListingRepository;
import dev.vish.nido.security.CurrentUserService;
import dev.vish.nido.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final BookingPricingService pricingService;
    private final CurrentUserService currentUserService;

    public BookingService(BookingRepository bookingRepository,
                          ListingRepository listingRepository,
                          BookingPricingService pricingService,
                          CurrentUserService currentUserService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.pricingService = pricingService;
        this.currentUserService = currentUserService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse create(BookingRequest request) {
        validateDates(request.checkIn(), request.checkOut());
        User guest = currentUserService.get();
        Listing listing = listingRepository.findLockedById(request.listingId())
                .orElseThrow(() -> new NotFoundException("Listing not found"));
        if (!listing.isActive()) {
            throw new NotFoundException("Listing not found");
        }
        if (listing.getHost().getId().equals(guest.getId())) {
            throw new ConflictException("Hosts cannot book their own listing");
        }
        if (request.guests() > listing.getMaxGuests()) {
            throw new IllegalArgumentException("Guest count exceeds the listing capacity");
        }
        if (bookingRepository.hasOverlappingBooking(
                listing.getId(), request.checkIn(), request.checkOut())) {
            throw new ConflictException("The listing is unavailable for these dates");
        }
        PriceBreakdown price = pricingService.calculate(
                listing.getNightlyPrice(), request.checkIn(), request.checkOut());
        Booking booking = new Booking(
                listing,
                guest,
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                price.total()
        );
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine() {
        User user = currentUserService.get();
        return bookingRepository.findByGuestIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> forMyListings() {
        User host = currentUserService.get();
        return bookingRepository.findByListingHostIdOrderByCreatedAtDesc(host.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        User user = currentUserService.get();
        Booking booking = bookingRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getGuest().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Only the guest can cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }
        if (!LocalDate.now().isBefore(booking.getCheckIn())) {
            throw new ConflictException("Bookings cannot be cancelled after check-in");
        }
        booking.cancel();
        return toResponse(booking);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        if (!checkIn.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in must be in the future");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        PriceBreakdown price = pricingService.calculate(
                booking.getListing().getNightlyPrice(),
                booking.getCheckIn(),
                booking.getCheckOut()
        );
        return new BookingResponse(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getTitle(),
                booking.getListing().getCity(),
                booking.getListing().getImageUrl(),
                booking.getGuest().getId(),
                booking.getGuest().getName(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getGuests(),
                price.nights(),
                price.subtotal(),
                price.serviceFee(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
