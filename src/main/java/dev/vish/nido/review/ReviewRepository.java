package dev.vish.nido.review;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    @EntityGraph(attributePaths = "author")
    List<Review> findByListingIdOrderByCreatedAtDesc(Long listingId);
}
