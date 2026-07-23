package dev.vish.nido.config;

import dev.vish.nido.booking.Booking;
import dev.vish.nido.booking.BookingPricingService;
import dev.vish.nido.listing.Listing;
import dev.vish.nido.listing.ListingRepository;
import dev.vish.nido.listing.PropertyType;
import dev.vish.nido.review.Review;
import dev.vish.nido.review.ReviewRepository;
import dev.vish.nido.booking.BookingRepository;
import dev.vish.nido.user.User;
import dev.vish.nido.user.UserRepository;
import dev.vish.nido.user.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final BookingPricingService pricingService;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoDataInitializer(UserRepository userRepository,
                               ListingRepository listingRepository,
                               BookingRepository bookingRepository,
                               ReviewRepository reviewRepository,
                               BookingPricingService pricingService,
                               PasswordEncoder passwordEncoder,
                               @Value("${nido.demo-data}") boolean enabled) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.pricingService = pricingService;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        User host = userRepository.save(new User(
                "Elena Rossi",
                "host@nido.dev",
                passwordEncoder.encode("Host123!"),
                UserRole.HOST
        ));
        User guest = userRepository.save(new User(
                "Marco Bianchi",
                "guest@nido.dev",
                passwordEncoder.encode("Guest123!"),
                UserRole.GUEST
        ));
        List<Listing> listings = listingRepository.saveAll(List.of(
                listing(host, "Canal-side studio", "Venice", "Italy",
                        "A calm studio near Cannaregio with original beams and a private courtyard.",
                        PropertyType.APARTMENT, 2, 1, 1, "1.0", "118.00",
                        "/images/venice.svg", Set.of("Wi-Fi", "Kitchen", "Air conditioning")),
                listing(host, "Stone house in the hills", "Florence", "Italy",
                        "A restored Tuscan home surrounded by olive trees, twenty minutes from the city.",
                        PropertyType.HOUSE, 5, 2, 3, "2.0", "164.00",
                        "/images/florence.svg", Set.of("Pool", "Parking", "Kitchen", "Garden")),
                listing(host, "Bright loft near Navigli", "Milan", "Italy",
                        "An open-plan loft with a quiet workspace and fast connections across Milan.",
                        PropertyType.LOFT, 3, 1, 2, "1.0", "142.00",
                        "/images/milan.svg", Set.of("Wi-Fi", "Workspace", "Washer", "Air conditioning")),
                listing(host, "Terrace above Trastevere", "Rome", "Italy",
                        "A warm apartment with a rooftop terrace close to markets and local restaurants.",
                        PropertyType.APARTMENT, 4, 2, 2, "1.5", "156.00",
                        "/images/rome.svg", Set.of("Terrace", "Kitchen", "Wi-Fi", "Washer")),
                listing(host, "Lake cabin with a view", "Como", "Italy",
                        "A compact timber cabin with wide lake views and direct access to walking trails.",
                        PropertyType.CABIN, 2, 1, 1, "1.0", "132.00",
                        "/images/como.svg", Set.of("Lake view", "Parking", "Fireplace", "Kitchen")),
                listing(host, "Courtyard home by the sea", "Bari", "Italy",
                        "A whitewashed home in the old town with a shaded courtyard and modern kitchen.",
                        PropertyType.HOUSE, 4, 2, 3, "2.0", "124.00",
                        "/images/bari.svg", Set.of("Courtyard", "Kitchen", "Wi-Fi", "Air conditioning"))
        ));
        LocalDate pastCheckIn = LocalDate.now().minusDays(18);
        LocalDate pastCheckOut = LocalDate.now().minusDays(14);
        Listing reviewedListing = listings.get(0);
        BigDecimal total = pricingService.calculate(
                reviewedListing.getNightlyPrice(), pastCheckIn, pastCheckOut).total();
        Booking booking = bookingRepository.save(new Booking(
                reviewedListing, guest, pastCheckIn, pastCheckOut, 2, total));
        reviewRepository.save(new Review(
                booking,
                reviewedListing,
                guest,
                5,
                "Quiet, comfortable and close to everything we wanted to see."
        ));
    }

    private Listing listing(User host, String title, String city, String country,
                            String description, PropertyType type, int maxGuests,
                            int bedrooms, int beds, String bathrooms, String price,
                            String imageUrl, Set<String> amenities) {
        return new Listing(
                host,
                title,
                city,
                country,
                description,
                type,
                maxGuests,
                bedrooms,
                beds,
                new BigDecimal(bathrooms),
                new BigDecimal(price),
                imageUrl,
                amenities
        );
    }
}
