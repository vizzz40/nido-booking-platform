package dev.vish.nido.listing;

import dev.vish.nido.common.ForbiddenOperationException;
import dev.vish.nido.common.NotFoundException;
import dev.vish.nido.review.ReviewRepository;
import dev.vish.nido.security.CurrentUserService;
import dev.vish.nido.user.User;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingSearchRepository searchRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserService currentUserService;

    public ListingService(ListingRepository listingRepository,
                          ListingSearchRepository searchRepository,
                          ReviewRepository reviewRepository,
                          CurrentUserService currentUserService) {
        this.listingRepository = listingRepository;
        this.searchRepository = searchRepository;
        this.reviewRepository = reviewRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> search(ListingSearch search) {
        validateSearch(search);
        List<Long> ids = searchRepository.search(search);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Listing> listings = listingRepository.findDetailedByIdIn(ids).stream()
                .collect(Collectors.toMap(Listing::getId, Function.identity()));
        Map<Long, ReviewRepository.RatingSummary> ratings = reviewRepository.summarizeRatings(ids)
                .stream()
                .collect(Collectors.toMap(ReviewRepository.RatingSummary::getListingId,
                        Function.identity()));
        return ids.stream()
                .map(listings::get)
                .filter(java.util.Objects::nonNull)
                .map(listing -> toResponse(listing, ratings.get(listing.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse get(Long id) {
        Listing listing = listingRepository.findDetailedById(id)
                .filter(Listing::isActive)
                .orElseThrow(() -> new NotFoundException("Listing not found"));
        ReviewRepository.RatingSummary rating = reviewRepository.summarizeRatings(List.of(id))
                .stream()
                .findFirst()
                .orElse(null);
        return toResponse(listing, rating);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine() {
        User host = currentUserService.get();
        List<Listing> listings = listingRepository
                .findByHostIdAndActiveTrueOrderByCreatedAtDesc(host.getId());
        List<Long> ids = listings.stream().map(Listing::getId).toList();
        Map<Long, ReviewRepository.RatingSummary> ratings = ids.isEmpty()
                ? Map.of()
                : reviewRepository.summarizeRatings(ids).stream()
                        .collect(Collectors.toMap(
                                ReviewRepository.RatingSummary::getListingId,
                                Function.identity()));
        return listings.stream()
                .map(listing -> toResponse(listing, ratings.get(listing.getId())))
                .toList();
    }

    @Transactional
    public ListingResponse create(ListingRequest request) {
        User host = currentUserService.get();
        Listing listing = new Listing(
                host,
                request.title().trim(),
                request.city().trim(),
                request.country().trim(),
                request.description().trim(),
                request.propertyType(),
                request.maxGuests(),
                request.bedrooms(),
                request.beds(),
                request.bathrooms(),
                request.nightlyPrice(),
                request.imageUrl().trim(),
                normalizeAmenities(request.amenities())
        );
        return toResponse(listingRepository.save(listing), null);
    }

    @Transactional
    public ListingResponse update(Long id, ListingRequest request) {
        Listing listing = ownedListing(id);
        listing.update(
                request.title().trim(),
                request.city().trim(),
                request.country().trim(),
                request.description().trim(),
                request.propertyType(),
                request.maxGuests(),
                request.bedrooms(),
                request.beds(),
                request.bathrooms(),
                request.nightlyPrice(),
                request.imageUrl().trim(),
                normalizeAmenities(request.amenities())
        );
        return toResponse(listing, null);
    }

    @Transactional
    public void deactivate(Long id) {
        ownedListing(id).deactivate();
    }

    private Listing ownedListing(Long id) {
        User host = currentUserService.get();
        Listing listing = listingRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Listing not found"));
        if (!listing.getHost().getId().equals(host.getId())) {
            throw new ForbiddenOperationException("Only the host can change this listing");
        }
        return listing;
    }

    private void validateSearch(ListingSearch search) {
        if ((search.checkIn() == null) != (search.checkOut() == null)) {
            throw new IllegalArgumentException("Check-in and check-out must be provided together");
        }
        if (search.checkIn() != null) {
            if (!search.checkOut().isAfter(search.checkIn())) {
                throw new IllegalArgumentException("Check-out must be after check-in");
            }
            if (search.checkIn().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Check-in cannot be in the past");
            }
        }
        if (search.guests() != null && search.guests() < 1) {
            throw new IllegalArgumentException("Guests must be at least one");
        }
        if (search.minPrice() != null && search.maxPrice() != null
                && search.maxPrice().compareTo(search.minPrice()) < 0) {
            throw new IllegalArgumentException("Maximum price cannot be lower than minimum price");
        }
    }

    private Set<String> normalizeAmenities(Set<String> amenities) {
        return amenities.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private ListingResponse toResponse(Listing listing,
                                       ReviewRepository.RatingSummary rating) {
        Double average = rating == null ? null
                : Math.round(rating.getAverageRating() * 10.0) / 10.0;
        long count = rating == null ? 0 : rating.getReviewCount();
        return new ListingResponse(
                listing.getId(),
                listing.getHost().getId(),
                listing.getHost().getName(),
                listing.getTitle(),
                listing.getCity(),
                listing.getCountry(),
                listing.getDescription(),
                listing.getPropertyType(),
                listing.getMaxGuests(),
                listing.getBedrooms(),
                listing.getBeds(),
                listing.getBathrooms(),
                listing.getNightlyPrice(),
                listing.getImageUrl(),
                listing.getAmenities(),
                average,
                count
        );
    }
}
