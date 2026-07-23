package dev.vish.nido.listing;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ListingSearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ListingSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> search(ListingSearch search) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id
                FROM listings l
                WHERE l.active = TRUE
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (search.location() != null && !search.location().isBlank()) {
            sql.append("""
                    AND (LOWER(l.city) LIKE :location OR LOWER(l.country) LIKE :location)
                    """);
            parameters.addValue("location", "%" + search.location().trim().toLowerCase() + "%");
        }
        if (search.guests() != null) {
            sql.append("AND l.max_guests >= :guests\n");
            parameters.addValue("guests", search.guests());
        }
        if (search.minPrice() != null) {
            sql.append("AND l.nightly_price >= :minPrice\n");
            parameters.addValue("minPrice", search.minPrice());
        }
        if (search.maxPrice() != null) {
            sql.append("AND l.nightly_price <= :maxPrice\n");
            parameters.addValue("maxPrice", search.maxPrice());
        }
        if (search.checkIn() != null && search.checkOut() != null) {
            sql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM bookings b
                        WHERE b.listing_id = l.id
                          AND b.status = 'CONFIRMED'
                          AND b.check_in < :checkOut
                          AND b.check_out > :checkIn
                    )
                    """);
            parameters.addValue("checkIn", search.checkIn());
            parameters.addValue("checkOut", search.checkOut());
        }
        sql.append("ORDER BY l.created_at DESC LIMIT 100");
        return jdbc.queryForList(sql.toString(), parameters, Long.class);
    }
}
