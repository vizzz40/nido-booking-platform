package dev.vish.nido.review;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    @EntityGraph(attributePaths = "author")
    List<Review> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @Query("""
            select r.listing.id as listingId, avg(r.rating) as averageRating, count(r) as reviewCount
            from Review r
            where r.listing.id in :listingIds
            group by r.listing.id
            """)
    List<RatingSummary> summarizeRatings(@Param("listingIds") List<Long> listingIds);

    interface RatingSummary {
        Long getListingId();

        Double getAverageRating();

        long getReviewCount();
    }
}
