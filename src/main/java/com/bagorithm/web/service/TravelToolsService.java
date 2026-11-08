package com.bagorithm.web.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;
import java.util.ArrayList;

@Service
public class TravelToolsService {
    private static final List<String> DEFAULT_TASKS = List.of(
            "Switch off unnecessary electrical appliances.", "Lock all doors and windows.", "Check water taps.",
            "Turn off gas supply.", "Keep important documents safely.", "Arrange care for pets.",
            "Arrange care for plants.", "Check travel tickets.", "Keep emergency contact information ready.");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private final JdbcTemplate jdbc;
    private final Path uploadDirectory;

    public TravelToolsService(JdbcTemplate jdbc, org.springframework.core.env.Environment environment) {
        this.jdbc = jdbc;
        this.uploadDirectory = Path.of(environment.getProperty("bagorithm.upload-dir", "./uploads")).toAbsolutePath().normalize();
    }

    public List<Map<String, Object>> rows(String table, long tripId) {
        String query = switch (table) {
            case "itinerary" -> "SELECT id, activity_name AS name, description, planned_date, planned_time, location, priority, completed FROM itinerary_activities WHERE trip_id = ? ORDER BY planned_date, planned_time";
            case "shopping" -> "SELECT id, item_name AS name, category, estimated_price, quantity, estimated_price * quantity AS total, purchased AS completed FROM shopping_items WHERE trip_id = ? ORDER BY purchased, item_name";
            case "souvenirs" -> "SELECT id, name, description, collected_place, collection_date, photo_path FROM souvenirs WHERE trip_id = ? ORDER BY collection_date DESC";
            case "checklist" -> "SELECT id, task_name AS name, completed, is_default FROM home_checklist_tasks WHERE trip_id = ? ORDER BY is_default DESC, id";
            case "memories" -> "SELECT id, title AS name, experience AS description, favourite_places, rating, created_at, (SELECT string_agg(photo_path, '|') FROM travel_memory_photos p WHERE p.memory_id = travel_memories.id) AS photo_paths FROM travel_memories WHERE trip_id = ? ORDER BY created_at DESC";
            default -> throw new IllegalArgumentException("Unsupported travel tool.");
        };
        return jdbc.queryForList(query, tripId);
    }

    public List<Map<String, Object>> souvenirsForUser(long userId, Long tripId) {
        String query = """
                SELECT s.id, s.trip_id, s.name, s.description, s.collected_place, s.collection_date,
                       s.photo_path, t.destination
                FROM souvenirs s JOIN trips t ON t.id = s.trip_id
                WHERE t.user_id = ?
                """ + (tripId == null ? "" : " AND s.trip_id = ?")
                + " ORDER BY s.collection_date DESC, s.id DESC";
        return tripId == null ? jdbc.queryForList(query, userId) : jdbc.queryForList(query, userId, tripId);
    }

    @Transactional
    public void ensureChecklist(long tripId) {
        for (String task : DEFAULT_TASKS) {
            jdbc.update("INSERT INTO home_checklist_tasks(trip_id, task_name, is_default) VALUES (?, ?, TRUE) ON CONFLICT (trip_id, task_name) DO NOTHING",
                    tripId, task);
        }
    }

    public void addItinerary(long tripId, String name, String description, LocalDate date, String time,
                             String location, String priority) {
        requireText(name, "Activity name");
        validateItinerary(name, date, time, priority);
        jdbc.update("INSERT INTO itinerary_activities(trip_id, activity_name, description, planned_date, planned_time, location, priority) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tripId, name.strip(), safe(description), date, java.time.LocalTime.parse(time), safe(location), priority);
    }

    public void addShopping(long tripId, String name, String category, double price, int quantity) {
        requireText(name, "Item name");
        validateLength(name, 160, "Item name");
        if (!Double.isFinite(price) || price < 0 || price > 9_999_999_999.99 || quantity < 1)
            throw new IllegalArgumentException("Enter a valid non-negative price and positive quantity.");
        jdbc.update("INSERT INTO shopping_items(trip_id, item_name, category, estimated_price, quantity) VALUES (?, ?, ?, ?, ?)",
                tripId, name.strip(), safe(category), price, quantity);
    }

    public void addChecklistTask(long tripId, String name) {
        requireText(name, "Task name");
        validateLength(name, 200, "Task name");
        jdbc.update("INSERT INTO home_checklist_tasks(trip_id, task_name) VALUES (?, ?)", tripId, name.strip());
    }

    @Transactional
    public void addSouvenir(long tripId, String name, String description, String place, LocalDate date, MultipartFile photo) {
        requireText(name, "Souvenir name");
        validateLength(name, 160, "Souvenir name");
        if (date == null) throw new IllegalArgumentException("Collection date is required.");
        String saved = savePhoto(photo);
        try {
            jdbc.update("INSERT INTO souvenirs(trip_id, name, description, collected_place, collection_date, photo_path) VALUES (?, ?, ?, ?, ?, ?)",
                    tripId, name.strip(), safe(description), safe(place), date, saved);
        } catch (RuntimeException exception) {
            deleteUploadedFile(saved);
            throw exception;
        }
    }

    @Transactional
    public void addMemory(long tripId, String title, String experience, String places, int rating, List<MultipartFile> photos) {
        requireText(title, "Memory title");
        validateLength(title, 160, "Memory title");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Choose a rating from 1 to 5.");
        List<String> uploaded = new ArrayList<>();
        try {
            Long memoryId = jdbc.queryForObject("INSERT INTO travel_memories(trip_id, title, experience, favourite_places, rating) VALUES (?, ?, ?, ?, ?) RETURNING id",
                    Long.class, tripId, title.strip(), safe(experience), safe(places), rating);
            if (memoryId == null) throw new IllegalStateException("Could not save travel memory.");
            for (MultipartFile photo : photos) {
                String saved = savePhoto(photo);
                if (saved != null) {
                    uploaded.add(saved);
                    jdbc.update("INSERT INTO travel_memory_photos(memory_id, photo_path) VALUES (?, ?)", memoryId, saved);
                }
            }
        } catch (RuntimeException exception) {
            uploaded.forEach(this::deleteUploadedFile);
            throw exception;
        }
    }

    public void toggle(String table, long tripId, long id) {
        String sql = switch (table) {
            case "itinerary" -> "UPDATE itinerary_activities SET completed = NOT completed WHERE id = ? AND trip_id = ?";
            case "shopping" -> "UPDATE shopping_items SET purchased = NOT purchased WHERE id = ? AND trip_id = ?";
            case "checklist" -> "UPDATE home_checklist_tasks SET completed = NOT completed WHERE id = ? AND trip_id = ?";
            default -> throw new IllegalArgumentException("This item does not have a completion status.");
        };
        if (jdbc.update(sql, id, tripId) == 0) throw new IllegalArgumentException("Item not found.");
    }

    public void delete(String table, long tripId, long id) {
        String sql = switch (table) {
            case "itinerary" -> "DELETE FROM itinerary_activities WHERE id = ? AND trip_id = ?";
            case "shopping" -> "DELETE FROM shopping_items WHERE id = ? AND trip_id = ?";
            case "souvenirs" -> "DELETE FROM souvenirs WHERE id = ? AND trip_id = ?";
            case "checklist" -> "DELETE FROM home_checklist_tasks WHERE id = ? AND trip_id = ? AND is_default = FALSE";
            case "memories" -> "DELETE FROM travel_memories WHERE id = ? AND trip_id = ?";
            default -> throw new IllegalArgumentException("Unsupported travel tool.");
        };
        if (jdbc.update(sql, id, tripId) == 0) throw new IllegalArgumentException("Item not found or cannot be deleted.");
    }

    public void updateChecklistTask(long tripId, long id, String name) {
        requireText(name, "Task name");
        validateLength(name, 200, "Task name");
        if (jdbc.update("UPDATE home_checklist_tasks SET task_name = ? WHERE id = ? AND trip_id = ? AND is_default = FALSE",
                name.strip(), id, tripId) == 0) throw new IllegalArgumentException("Custom task not found.");
    }

    public void updateItinerary(long tripId, long id, String name, String description, LocalDate date,
                                String time, String location, String priority) {
        requireText(name, "Activity name");
        validateItinerary(name, date, time, priority);
        if (jdbc.update("""
                UPDATE itinerary_activities SET activity_name = ?, description = ?, planned_date = ?,
                    planned_time = ?, location = ?, priority = ? WHERE id = ? AND trip_id = ?
                """, name.strip(), safe(description), date, java.time.LocalTime.parse(time), safe(location), priority, id, tripId) == 0)
            throw new IllegalArgumentException("Activity not found.");
    }

    public void updateShopping(long tripId, long id, String name, String category, double price, int quantity) {
        requireText(name, "Item name");
        validateLength(name, 160, "Item name");
        if (!Double.isFinite(price) || price < 0 || price > 9_999_999_999.99 || quantity < 1)
            throw new IllegalArgumentException("Enter a valid non-negative price and positive quantity.");
        if (jdbc.update("""
                UPDATE shopping_items SET item_name = ?, category = ?, estimated_price = ?, quantity = ?
                WHERE id = ? AND trip_id = ?
                """, name.strip(), safe(category), price, quantity, id, tripId) == 0)
            throw new IllegalArgumentException("Shopping item not found.");
    }

    public void updateSouvenir(long tripId, long id, String name, String description, String place,
                               LocalDate date, MultipartFile photo) {
        requireText(name, "Souvenir name");
        validateLength(name, 160, "Souvenir name");
        if (date == null) throw new IllegalArgumentException("Collection date is required.");
        String saved = savePhoto(photo);
        String sql = saved == null
                ? "UPDATE souvenirs SET name = ?, description = ?, collected_place = ?, collection_date = ? WHERE id = ? AND trip_id = ?"
                : "UPDATE souvenirs SET name = ?, description = ?, collected_place = ?, collection_date = ?, photo_path = ? WHERE id = ? AND trip_id = ?";
        int changed;
        try {
            changed = saved == null
                    ? jdbc.update(sql, name.strip(), safe(description), safe(place), date, id, tripId)
                    : jdbc.update(sql, name.strip(), safe(description), safe(place), date, saved, id, tripId);
        } catch (RuntimeException exception) {
            deleteUploadedFile(saved);
            throw exception;
        }
        if (changed == 0) {
            deleteUploadedFile(saved);
            throw new IllegalArgumentException("Souvenir not found.");
        }
    }

    public void updateMemory(long tripId, long id, String title, String experience, String places, int rating) {
        requireText(title, "Memory title");
        validateLength(title, 160, "Memory title");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Choose a rating from 1 to 5.");
        if (jdbc.update("""
                UPDATE travel_memories SET title = ?, experience = ?, favourite_places = ?, rating = ?
                WHERE id = ? AND trip_id = ?
                """, title.strip(), safe(experience), safe(places), rating, id, tripId) == 0)
            throw new IllegalArgumentException("Travel memory not found.");
    }

    public double shoppingTotal(long tripId) {
        Double total = jdbc.queryForObject("SELECT COALESCE(SUM(estimated_price * quantity), 0) FROM shopping_items WHERE trip_id = ?",
                Double.class, tripId);
        return total == null ? 0 : total;
    }

    public boolean ownsPhoto(String email, String filename) {
        String query = """
                SELECT
                    (SELECT COUNT(*) FROM souvenirs s JOIN trips t ON t.id = s.trip_id
                     WHERE t.user_id = (SELECT id FROM app_users WHERE lower(email) = lower(?)) AND s.photo_path = ?)
                  + (SELECT COUNT(*) FROM travel_memory_photos p
                     JOIN travel_memories m ON m.id = p.memory_id JOIN trips t ON t.id = m.trip_id
                     WHERE t.user_id = (SELECT id FROM app_users WHERE lower(email) = lower(?)) AND p.photo_path = ?)
                """;
        Long count = jdbc.queryForObject(query, Long.class, email, filename, email, filename);
        return count != null && count > 0;
    }

    public Path photoPath(String filename) {
        Path target = uploadDirectory.resolve(filename).normalize();
        if (!target.startsWith(uploadDirectory)) throw new IllegalArgumentException("Invalid photo path.");
        return target;
    }

    private String savePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return null;
        if (photo.getSize() > 5 * 1024 * 1024 || !IMAGE_TYPES.contains(photo.getContentType()))
            throw new IllegalArgumentException("Use a JPG, PNG or GIF image up to 5 MB.");
        String mimeType = photo.getContentType();
        String extension = switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> throw new IllegalArgumentException("Unsupported image format.");
        };
        String filename = UUID.randomUUID() + extension;
        Path temporaryFile = null;
        try {
            verifyImage(photo, mimeType);
            Files.createDirectories(uploadDirectory);
            temporaryFile = Files.createTempFile(uploadDirectory, ".upload-", ".tmp");
            try (InputStream input = photo.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Path destination = uploadDirectory.resolve(filename);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination);
            }
            return filename;
        } catch (IOException exception) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new IllegalStateException("Could not store uploaded image.", exception);
        }
    }

    private void verifyImage(MultipartFile photo, String mimeType) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(photo.getInputStream())) {
            if (input == null) throw new IllegalArgumentException("The selected file is not a readable image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("The selected file is not a readable image.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expected = switch (mimeType) {
                    case "image/jpeg" -> "jpeg";
                    case "image/png" -> "png";
                    case "image/gif" -> "gif";
                    default -> throw new IllegalArgumentException("Unsupported image format.");
                };
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!format.equals(expected) && !(expected.equals("jpeg") && format.equals("jpg")))
                    throw new IllegalArgumentException("The selected image content does not match its file type.");
                if (width <= 0 || height <= 0 || (long) width * height > 20_000_000L)
                    throw new IllegalArgumentException("Image dimensions are too large.");
                try {
                    reader.read(0);
                } catch (IOException exception) {
                    throw new IllegalArgumentException("The selected file is not a readable image.", exception);
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private void deleteUploadedFile(String filename) {
        if (filename == null) return;
        try {
            Files.deleteIfExists(uploadDirectory.resolve(filename).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not clean up an unused uploaded image.", exception);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
    }

    private void validateItinerary(String name, LocalDate date, String time, String priority) {
        validateLength(name, 160, "Activity name");
        if (date == null || time == null || time.isBlank())
            throw new IllegalArgumentException("Activity date and time are required.");
        if (priority == null || !Set.of("High", "Medium", "Low").contains(priority))
            throw new IllegalArgumentException("Choose a valid priority.");
        try {
            java.time.LocalTime.parse(time);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid activity time.");
        }
    }

    private void validateLength(String value, int maximum, String field) {
        if (value.strip().length() > maximum)
            throw new IllegalArgumentException(field + " must be no more than " + maximum + " characters.");
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }
}
