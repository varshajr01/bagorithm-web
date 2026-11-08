package com.bagorithm.web.service;

import com.bagorithm.web.model.PackingItem;
import com.bagorithm.web.model.Trip;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {
    private static final List<String> WEATHER_OPTIONS = List.of("moderate", "hot", "cold", "rainy");
    private static final List<String> PURPOSE_OPTIONS = List.of(
            "vacation", "business", "adventure", "family trip", "religious trip", "educational trip");
    private final JdbcTemplate jdbc;
    private final RecommendationService recommendations;

    public TripService(JdbcTemplate jdbc, RecommendationService recommendations) {
        this.jdbc = jdbc;
        this.recommendations = recommendations;
    }

    public long userId(String email) {
        Long id = jdbc.queryForObject("SELECT id FROM app_users WHERE lower(email) = lower(?)", Long.class, email);
        if (id == null) throw new IllegalStateException("Authenticated account could not be found.");
        return id;
    }

    public List<Trip> findTrips(long userId) {
        return jdbc.query("SELECT * FROM trips WHERE user_id = ? ORDER BY departure_date DESC NULLS LAST, id DESC",
                (row, number) -> readTrip(row.getLong("id"), row.getLong("user_id"), row.getString("destination"),
                        row.getDate("departure_date"), row.getDate("return_date"), row.getInt("duration_days"),
                        row.getString("weather"), row.getString("purpose"), row.getDouble("max_capacity_kg")), userId);
    }

    public Optional<Trip> findTrip(long tripId, long userId) {
        return jdbc.query("SELECT * FROM trips WHERE id = ? AND user_id = ?",
                        (row, number) -> readTrip(row.getLong("id"), row.getLong("user_id"), row.getString("destination"),
                                row.getDate("departure_date"), row.getDate("return_date"), row.getInt("duration_days"),
                                row.getString("weather"), row.getString("purpose"), row.getDouble("max_capacity_kg")),
                        tripId, userId).stream().findFirst();
    }

    @Transactional
    public Trip create(long userId, String destination, LocalDate departure, LocalDate returning,
                       String weather, String purpose, double capacity) {
        if (destination == null || destination.isBlank() || destination.strip().length() > 160
                || weather == null || !WEATHER_OPTIONS.contains(weather.toLowerCase(java.util.Locale.ROOT))
                || purpose == null || !PURPOSE_OPTIONS.contains(purpose.toLowerCase(java.util.Locale.ROOT))
                || !Double.isFinite(capacity) || capacity <= 0 || capacity > 999_999.99) {
            throw new IllegalArgumentException("Complete all trip fields and enter a positive luggage capacity.");
        }
        if (departure != null && returning != null && returning.isBefore(departure)) {
            throw new IllegalArgumentException("The return date cannot be before the departure date.");
        }
        Trip trip = new Trip(0, userId, destination.strip(), departure, returning, Trip.duration(departure, returning),
                weather.strip(), purpose.strip(), capacity);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO trips(user_id, destination, departure_date, return_date, duration_days, weather, purpose, max_capacity_kg)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, trip.userId());
            statement.setString(2, trip.destination());
            statement.setDate(3, departure == null ? null : Date.valueOf(departure));
            statement.setDate(4, returning == null ? null : Date.valueOf(returning));
            statement.setInt(5, trip.durationDays());
            statement.setString(6, trip.weather());
            statement.setString(7, trip.purpose());
            statement.setDouble(8, trip.maxCapacityKg());
            return statement;
        }, key);
        Number generated = key.getKey();
        if (generated == null) throw new IllegalStateException("Could not retrieve the new trip ID.");
        Trip saved = new Trip(generated.longValue(), userId, trip.destination(), departure, returning,
                trip.durationDays(), weather, purpose, capacity);
        List<PackingItem> suggestions = recommendations.recommend(saved);
        for (PackingItem item : suggestions) {
            jdbc.update("INSERT INTO packing_items(trip_id, name, category, quantity, unit_weight_grams) VALUES(?, ?, ?, ?, ?)",
                    saved.id(), item.name(), item.category(), item.quantity(), item.unitWeightGrams());
        }
        return saved;
    }

    public List<PackingItem> packingItems(long tripId) {
        return jdbc.query("SELECT * FROM packing_items WHERE trip_id = ? ORDER BY category, name",
                (row, number) -> new PackingItem(row.getLong("id"), row.getLong("trip_id"), row.getString("name"),
                        row.getString("category"), row.getInt("quantity"), row.getDouble("unit_weight_grams"),
                        row.getBoolean("packed"), row.getBoolean("selected")), tripId);
    }

    public void addPackingItem(long tripId, String name, String category, int quantity, double grams) {
        if (name == null || name.isBlank() || name.strip().length() > 160
                || category == null || category.isBlank() || category.strip().length() > 100
                || quantity < 1 || !Double.isFinite(grams) || grams <= 0 || grams > 99_999_999.99)
            throw new IllegalArgumentException("Enter a name, positive quantity, and positive item weight.");
        try {
            jdbc.update("INSERT INTO packing_items(trip_id, name, category, quantity, unit_weight_grams, selected) VALUES(?, ?, ?, ?, ?, TRUE)",
                    tripId, name.strip(), category.strip(), quantity, grams);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("This packing item already exists. Edit its quantity instead.");
        }
    }

    public void updatePackingItem(long tripId, long itemId, int quantity, double grams) {
        if (quantity < 1 || !Double.isFinite(grams) || grams <= 0 || grams > 99_999_999.99)
            throw new IllegalArgumentException("Quantity and item weight must be positive.");
        if (jdbc.update("UPDATE packing_items SET quantity = ?, unit_weight_grams = ? WHERE id = ? AND trip_id = ?",
                quantity, grams, itemId, tripId) == 0) throw new IllegalArgumentException("Packing item not found.");
    }

    public void togglePacked(long tripId, long itemId) {
        if (jdbc.update("UPDATE packing_items SET packed = NOT packed WHERE id = ? AND trip_id = ?", itemId, tripId) == 0)
            throw new IllegalArgumentException("Packing item not found.");
    }

    public void toggleSelected(long tripId, long itemId) {
        if (jdbc.update("UPDATE packing_items SET selected = NOT selected WHERE id = ? AND trip_id = ?", itemId, tripId) == 0)
            throw new IllegalArgumentException("Packing item not found.");
    }

    public void deletePackingItem(long tripId, long itemId) {
        if (jdbc.update("DELETE FROM packing_items WHERE id = ? AND trip_id = ?", itemId, tripId) == 0)
            throw new IllegalArgumentException("Packing item not found.");
    }

    private Trip readTrip(long id, long userId, String destination, Date departure, Date returning, int days,
                          String weather, String purpose, double capacity) {
        LocalDate start = departure == null ? null : departure.toLocalDate();
        LocalDate end = returning == null ? null : returning.toLocalDate();
        return new Trip(id, userId, destination, start, end, days, weather, purpose, capacity);
    }
}
