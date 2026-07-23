package dev.vish.nido.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select count(b) > 0 from Booking b
            where b.listing.id = :listingId
              and b.status = dev.vish.nido.booking.BookingStatus.CONFIRMED
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    boolean hasOverlappingBooking(@Param("listingId") Long listingId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    @EntityGraph(attributePaths = {"listing", "listing.host"})
    List<Booking> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    @EntityGraph(attributePaths = {"listing", "guest"})
    List<Booking> findByListingHostIdOrderByCreatedAtDesc(Long hostId);

    @EntityGraph(attributePaths = {"listing", "listing.host", "guest"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") Long id);
}
