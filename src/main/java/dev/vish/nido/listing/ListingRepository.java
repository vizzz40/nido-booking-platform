package dev.vish.nido.listing;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @EntityGraph(attributePaths = {"host", "amenities"})
    List<Listing> findByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"host", "amenities"})
    List<Listing> findByHostIdAndActiveTrueOrderByCreatedAtDesc(Long hostId);

    @EntityGraph(attributePaths = {"host", "amenities"})
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l join fetch l.host where l.id = :id")
    Optional<Listing> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"host", "amenities"})
    @Query("select l from Listing l where l.id in :ids")
    List<Listing> findDetailedByIdIn(@Param("ids") List<Long> ids);
}
