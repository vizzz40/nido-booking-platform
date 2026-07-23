package dev.vish.nido.listing;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @EntityGraph(attributePaths = {"host", "amenities"})
    List<Listing> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"host", "amenities"})
    List<Listing> findByHostIdAndActiveTrueOrderByCreatedAtDesc(Long hostId);
}
