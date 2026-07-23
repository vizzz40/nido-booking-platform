package dev.vish.nido.listing;

import dev.vish.nido.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false)
    private int beds;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bathrooms;

    @Column(name = "nightly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal nightlyPrice;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "listing_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity", nullable = false, length = 60)
    private Set<String> amenities = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Listing() {
    }

    public Listing(User host, String title, String city, String country, String description,
                   PropertyType propertyType, int maxGuests, int bedrooms, int beds,
                   BigDecimal bathrooms, BigDecimal nightlyPrice, String imageUrl,
                   Set<String> amenities) {
        this.host = host;
        this.title = title;
        this.city = city;
        this.country = country;
        this.description = description;
        this.propertyType = propertyType;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.beds = beds;
        this.bathrooms = bathrooms;
        this.nightlyPrice = nightlyPrice;
        this.imageUrl = imageUrl;
        this.amenities = new LinkedHashSet<>(amenities);
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void update(String title, String city, String country, String description,
                       PropertyType propertyType, int maxGuests, int bedrooms, int beds,
                       BigDecimal bathrooms, BigDecimal nightlyPrice, String imageUrl,
                       Set<String> amenities) {
        this.title = title;
        this.city = city;
        this.country = country;
        this.description = description;
        this.propertyType = propertyType;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.beds = beds;
        this.bathrooms = bathrooms;
        this.nightlyPrice = nightlyPrice;
        this.imageUrl = imageUrl;
        this.amenities = new LinkedHashSet<>(amenities);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public User getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getDescription() {
        return description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public int getBeds() {
        return beds;
    }

    public BigDecimal getBathrooms() {
        return bathrooms;
    }

    public BigDecimal getNightlyPrice() {
        return nightlyPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Set<String> getAmenities() {
        return Set.copyOf(amenities);
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Listing listing)) {
            return false;
        }
        return id != null && Objects.equals(id, listing.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
