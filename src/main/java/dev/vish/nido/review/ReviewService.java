package dev.vish.nido.review;

import dev.vish.nido.booking.Booking;
import dev.vish.nido.booking.BookingRepository;
import dev.vish.nido.booking.BookingStatus;
import dev.vish.nido.common.ConflictException;
import dev.vish.nido.common.ForbiddenOperationException;
import dev.vish.nido.common.NotFoundException;
import dev.vish.nido.security.CurrentUserService;
import dev.vish.nido.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ReviewResponse create(Long listingId, ReviewRequest request) {
        User author = currentUserService.get();
        Booking booking = bookingRepository.findDetailedById(request.bookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getGuest().getId().equals(author.getId())) {
            throw new ForbiddenOperationException("Only the booking guest can leave a review");
        }
        if (!booking.getListing().getId().equals(listingId)) {
            throw new IllegalArgumentException("The booking does not belong to this listing");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                || !booking.getCheckOut().isBefore(LocalDate.now())) {
            throw new ConflictException("Reviews can be added only after a completed stay");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("This booking has already been reviewed");
        }
        Review review = new Review(
                booking,
                booking.getListing(),
                author,
                request.rating(),
                request.comment().trim()
        );
        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getListing().getId(),
                review.getAuthor().getId(),
                review.getAuthor().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
