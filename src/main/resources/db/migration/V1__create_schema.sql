CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    property_type VARCHAR(30) NOT NULL,
    max_guests INTEGER NOT NULL CHECK (max_guests > 0),
    bedrooms INTEGER NOT NULL CHECK (bedrooms > 0),
    beds INTEGER NOT NULL CHECK (beds > 0),
    bathrooms NUMERIC(3, 1) NOT NULL CHECK (bathrooms > 0),
    nightly_price NUMERIC(10, 2) NOT NULL CHECK (nightly_price > 0),
    image_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE listing_amenities (
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    amenity VARCHAR(60) NOT NULL,
    PRIMARY KEY (listing_id, amenity)
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    guest_id BIGINT NOT NULL REFERENCES users(id),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INTEGER NOT NULL CHECK (guests > 0),
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price > 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (check_out > check_in)
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id),
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_listings_location ON listings (LOWER(city), LOWER(country));
CREATE INDEX idx_listings_price_guests ON listings (nightly_price, max_guests);
CREATE INDEX idx_bookings_listing_dates ON bookings (listing_id, check_in, check_out, status);
CREATE INDEX idx_bookings_guest ON bookings (guest_id, created_at DESC);
CREATE INDEX idx_reviews_listing ON reviews (listing_id, created_at DESC);
