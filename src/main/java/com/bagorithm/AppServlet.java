package com.bagorithm;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet("/app")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 6291456)
public class AppServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String[] HOME_TASKS = {
        "Lock doors and windows", "Switch off electrical appliances",
        "Arrange plant watering", "Secure important documents", "Check travel tickets"
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        try {
            if (request.getParameter("photo") != null) {
                streamMemory(request, response, (Integer) session.getAttribute("userId"));
                return;
            }
            loadPage(request, (Integer) session.getAttribute("userId"));
            request.getRequestDispatcher("/WEB-INF/app.jsp").forward(request, response);
        } catch (Exception exception) {
            throw new ServletException("Unable to load this page.", exception);
        }
    }

    private void streamMemory(HttpServletRequest request, HttpServletResponse response, int userId)
            throws Exception {
        int memoryId = parseInteger(request.getParameter("photo"), -1);
        List<Map<String, Object>> rows = query(
                "SELECT m.file_name FROM travel_memories m JOIN trips t ON m.trip_id=t.id "
                        + "WHERE m.id=? AND t.user_id=?", memoryId, userId);
        if (rows.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path photo = Db.getUploadDirectory().resolve(String.valueOf(rows.get(0).get("file_name")));
        if (!Files.isRegularFile(photo)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(photo);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, max-age=3600");
        try (java.io.InputStream input = Files.newInputStream(photo);
                java.io.OutputStream output = response.getOutputStream()) {
            input.transferTo(output);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        if ("register".equals(action)) {
            register(request, response);
            return;
        }
        if ("login".equals(action)) {
            login(request, response);
            return;
        }
        if ("logout".equals(action)) {
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String section = safeSection(request.getParameter("section"));
        String redirectTripId = request.getParameter("tripId");
        try {
            if ("createTrip".equals(action)) {
                redirectTripId = String.valueOf(createTrip(request, userId));
                section = "trips";
            } else if ("add".equals(action)) {
                addRecord(request, userId);
            } else if ("edit".equals(action)) {
                editRecord(request, userId);
            } else if ("delete".equals(action)) {
                deleteRecord(request, userId);
            } else if ("toggle".equals(action)) {
                toggleRecord(request, userId);
            } else if ("upload".equals(action)) {
                uploadMemory(request, userId);
            }
            request.getSession().setAttribute("flash", "Your changes have been saved.");
        } catch (IllegalArgumentException exception) {
            request.getSession().setAttribute("error", exception.getMessage());
        } catch (Exception exception) {
            throw new ServletException("Unable to save your changes.", exception);
        }
        response.sendRedirect(request.getContextPath() + "/app?section=" + section
                + (redirectTripId == null || redirectTripId.isBlank() ? "" : "&tripId=" + redirectTripId));
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");
        if (isBlank(name) || isBlank(email) || password == null || password.length() < 8
                || !password.equals(confirm) || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            redirectAuth(request, response, "register", "Enter a valid name and email, and use matching passwords of at least 8 characters.");
            return;
        }
        String salt = PasswordUtil.createSalt();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users(name,email,password_hash,password_salt) VALUES(?,?,?,?)")) {
            statement.setString(1, name.trim());
            statement.setString(2, email.trim().toLowerCase(Locale.ROOT));
            statement.setString(3, PasswordUtil.hash(password, salt));
            statement.setString(4, salt);
            statement.executeUpdate();
            response.sendRedirect(request.getContextPath() + "/auth.jsp?mode=login&message=Account%20created.%20Please%20sign%20in.");
        } catch (SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("UNIQUE")) {
                redirectAuth(request, response, "register", "That email address is already registered.");
            } else {
                throw new IOException("Registration failed.", exception);
            }
        } catch (Exception exception) {
            throw new IOException("Registration failed.", exception);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id,name,password_hash,password_salt FROM users WHERE email=?")) {
            statement.setString(1, email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || password == null
                        || !PasswordUtil.hash(password, result.getString("password_salt"))
                                .equals(result.getString("password_hash"))) {
                    redirectAuth(request, response, "login", "Email or password is incorrect.");
                    return;
                }
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", result.getInt("id"));
                session.setAttribute("userName", result.getString("name"));
                response.sendRedirect(request.getContextPath() + "/app");
            }
        } catch (Exception exception) {
            throw new IOException("Login failed.", exception);
        }
    }

    private void redirectAuth(HttpServletRequest request, HttpServletResponse response,
            String mode, String message) throws IOException {
        response.sendRedirect(request.getContextPath() + "/auth.jsp?mode=" + mode
                + "&error=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8));
    }

    private void loadPage(HttpServletRequest request, int userId) throws Exception {
        String section = safeSection(request.getParameter("section"));
        List<Map<String, Object>> trips = query(
                "SELECT * FROM trips WHERE user_id=? ORDER BY departure_date DESC", userId);
        int tripId = parseInteger(request.getParameter("tripId"), -1);
        if (tripId < 0 && !trips.isEmpty()) {
            tripId = ((Number) trips.get(0).get("id")).intValue();
        }
        Map<String, Object> selectedTrip = tripById(userId, tripId);
        if (selectedTrip == null) {
            tripId = -1;
        }
        request.setAttribute("section", section);
        request.setAttribute("trips", trips);
        request.setAttribute("trip", selectedTrip);
        request.setAttribute("tripId", tripId);
        request.setAttribute("userName", request.getSession().getAttribute("userName"));
        request.setAttribute("flash", request.getSession().getAttribute("flash"));
        request.setAttribute("error", request.getSession().getAttribute("error"));
        request.getSession().removeAttribute("flash");
        request.getSession().removeAttribute("error");

        if ("dashboard".equals(section)) {
            List<Map<String, Object>> upcoming = query(
                    "SELECT * FROM trips WHERE user_id=? AND departure_date>=? ORDER BY departure_date LIMIT 3",
                    userId, LocalDate.now().toString());
            request.setAttribute("upcomingTrips", upcoming);
            request.setAttribute("tripCount", trips.size());
            int packed = selectedTrip == null ? 0 : count("SELECT COUNT(*) FROM packing_items WHERE trip_id=? AND packed=1", tripId);
            int total = selectedTrip == null ? 0 : count("SELECT COUNT(*) FROM packing_items WHERE trip_id=?", tripId);
            request.setAttribute("packedCount", packed);
            request.setAttribute("packingCount", total);
        } else if ("trips".equals(section)) {
            request.setAttribute("records", trips);
        } else if (selectedTrip != null) {
            loadTripSection(request, section, tripId);
        }
    }

    private void loadTripSection(HttpServletRequest request, String section, int tripId) throws Exception {
        if ("packing".equals(section)) {
            List<Map<String, Object>> items = query(
                    "SELECT * FROM packing_items WHERE trip_id=? ORDER BY category,name", tripId);
            request.setAttribute("records", items);
            BigDecimal weight = BigDecimal.ZERO;
            int packed = 0;
            for (Map<String, Object> item : items) {
                BigDecimal itemWeight = new BigDecimal(String.valueOf(item.get("weight")))
                        .multiply(BigDecimal.valueOf(((Number) item.get("quantity")).longValue()));
                item.put("total_weight", itemWeight);
                weight = weight.add(itemWeight);
                if (((Number) item.get("packed")).intValue() == 1) {
                    packed++;
                }
            }
            request.setAttribute("totalWeight", weight);
            request.setAttribute("packedCount", packed);
            request.setAttribute("packingCount", items.size());
            BigDecimal maxWeight = new BigDecimal(String.valueOf(
                    ((Map<?, ?>) request.getAttribute("trip")).get("max_weight")));
            request.setAttribute("remainingWeight", maxWeight.subtract(weight));
        } else if ("itinerary".equals(section)) {
            request.setAttribute("records", query("SELECT * FROM itinerary WHERE trip_id=? ORDER BY plan_date,plan_time", tripId));
        } else if ("shopping".equals(section)) {
            request.setAttribute("records", query("SELECT * FROM shopping_list WHERE trip_id=? ORDER BY id DESC", tripId));
        } else if ("home".equals(section)) {
            request.setAttribute("records", query("SELECT * FROM home_checklist WHERE trip_id=? ORDER BY id", tripId));
        } else if ("souvenirs".equals(section)) {
            request.setAttribute("records", query("SELECT * FROM souvenirs WHERE trip_id=? ORDER BY id DESC", tripId));
        } else if ("memories".equals(section)) {
            request.setAttribute("records", query("SELECT * FROM travel_memories WHERE trip_id=? ORDER BY uploaded_at DESC", tripId));
        }
        if (request.getAttribute("trip") != null) {
            Map<?, ?> trip = (Map<?, ?>) request.getAttribute("trip");
            String departure = String.valueOf(trip.get("departure_date"));
            long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(departure));
            request.setAttribute("daysRemaining", days);
        }
    }

    private int createTrip(HttpServletRequest request, int userId) throws Exception {
        String name = required(request, "name");
        String destination = required(request, "destination");
        LocalDate departure;
        LocalDate returning;
        try {
            departure = LocalDate.parse(required(request, "departureDate"));
            returning = LocalDate.parse(required(request, "returnDate"));
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("Enter valid departure and return dates.");
        }
        int travellers = parseInteger(request.getParameter("travellers"), 0);
        double maxWeight;
        try {
            maxWeight = Double.parseDouble(request.getParameter("maxWeight"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid maximum luggage weight.");
        }
        String purpose = required(request, "purpose");
        String weather = required(request, "weather");
        if (returning.isBefore(departure) || travellers < 1 || maxWeight <= 0
                || Double.isInfinite(maxWeight) || Double.isNaN(maxWeight)) {
            throw new IllegalArgumentException("Return date must follow departure; travellers and luggage limit must be positive.");
        }
        int tripId;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO trips(user_id,name,destination,departure_date,return_date,travellers,purpose,weather,max_weight) "
                                + "VALUES(?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, name);
            statement.setString(3, destination);
            statement.setString(4, departure.toString());
            statement.setString(5, returning.toString());
            statement.setInt(6, travellers);
            statement.setString(7, purpose);
            statement.setString(8, weather);
            statement.setDouble(9, maxWeight);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Trip identifier was not generated.");
                }
                tripId = keys.getInt(1);
            }
        }
        addRecommendations(tripId, destination, weather, purpose,
                (int) ChronoUnit.DAYS.between(departure, returning) + 1);
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO home_checklist(trip_id,task) VALUES(?,?)")) {
            for (String task : HOME_TASKS) {
                statement.setInt(1, tripId);
                statement.setString(2, task);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return tripId;
    }

    private void addRecommendations(int tripId, String destination, String weather,
            String purpose, int days) throws SQLException, IOException {
        List<String[]> items = new ArrayList<>();
        items.add(new String[] {"Clothing", "T-shirts", Math.max(2, Math.min(days, 7)) + ""});
        items.add(new String[] {"Clothing", "Underwear and socks", Math.max(2, Math.min(days, 7)) + ""});
        items.add(new String[] {"Clothing", "Comfortable outfit", "1"});
        items.add(new String[] {"Footwear", "Walking shoes", "1"});
        items.add(new String[] {"Toiletries", "Personal toiletries", "1"});
        items.add(new String[] {"Electronics", "Phone charger", "1"});
        items.add(new String[] {"Documents", "ID and travel documents", "1"});
        items.add(new String[] {"Medicines", "Personal medication", "1"});
        items.add(new String[] {"Travel essentials", "Reusable water bottle", "1"});
        String conditions = (destination + " " + weather).toLowerCase(Locale.ROOT);
        if (conditions.contains("rain") || conditions.contains("wet")) {
            items.add(new String[] {"Clothing", "Rain jacket or umbrella", "1"});
            items.add(new String[] {"Footwear", "Water-resistant shoes", "1"});
        } else if (conditions.contains("cold") || conditions.contains("snow")
                || conditions.contains("winter")) {
            items.add(new String[] {"Clothing", "Warm coat and layers", "1"});
            items.add(new String[] {"Clothing", "Gloves and scarf", "1"});
        } else if (conditions.contains("hot") || conditions.contains("sun")
                || conditions.contains("tropical")) {
            items.add(new String[] {"Clothing", "Lightweight clothing", "2"});
            items.add(new String[] {"Travel essentials", "Sunscreen and sunglasses", "1"});
        }
        String lowerPurpose = purpose.toLowerCase(Locale.ROOT);
        if (lowerPurpose.contains("business") || lowerPurpose.contains("work")) {
            items.add(new String[] {"Clothing", "Business outfit", "1"});
            items.add(new String[] {"Electronics", "Laptop and presentation adapter", "1"});
        } else if (lowerPurpose.contains("adventure") || lowerPurpose.contains("hike")) {
            items.add(new String[] {"Footwear", "Hiking boots", "1"});
            items.add(new String[] {"Travel essentials", "Daypack", "1"});
        } else if (lowerPurpose.contains("beach")) {
            items.add(new String[] {"Clothing", "Swimwear", "1"});
            items.add(new String[] {"Travel essentials", "Beach towel", "1"});
        }
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO packing_items(trip_id,category,name,quantity) VALUES(?,?,?,?)")) {
            for (String[] item : items) {
                statement.setInt(1, tripId);
                statement.setString(2, item[0]);
                statement.setString(3, item[1]);
                statement.setInt(4, Integer.parseInt(item[2]));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void addRecord(HttpServletRequest request, int userId) throws Exception {
        int tripId = authorizedTripId(request, userId);
        String section = safeSection(request.getParameter("section"));
        try (Connection connection = Db.getConnection()) {
            if ("packing".equals(section)) {
                execute(connection, "INSERT INTO packing_items(trip_id,category,name,quantity,weight) VALUES(?,?,?,?,?)",
                        tripId, value(request, "category", "Travel essentials"), required(request, "name"),
                        positiveInteger(request, "quantity"), nonNegativeDouble(request, "weight"));
            } else if ("itinerary".equals(section)) {
                execute(connection, "INSERT INTO itinerary(trip_id,plan_date,activity,plan_time,location) VALUES(?,?,?,?,?)",
                        tripId, required(request, "date"), required(request, "activity"),
                        required(request, "time"), required(request, "location"));
            } else if ("shopping".equals(section)) {
                execute(connection, "INSERT INTO shopping_list(trip_id,name,quantity) VALUES(?,?,?)",
                        tripId, required(request, "name"), positiveInteger(request, "quantity"));
            } else if ("home".equals(section)) {
                execute(connection, "INSERT INTO home_checklist(trip_id,task) VALUES(?,?)",
                        tripId, required(request, "task"));
            } else if ("souvenirs".equals(section)) {
                execute(connection, "INSERT INTO souvenirs(trip_id,name,destination,notes) VALUES(?,?,?,?)",
                        tripId, required(request, "name"), required(request, "destination"),
                        value(request, "notes", ""));
            } else {
                throw new IllegalArgumentException("Choose a valid trip section.");
            }
        }
    }

    private void editRecord(HttpServletRequest request, int userId) throws Exception {
        int tripId = authorizedTripId(request, userId);
        int id = parseInteger(request.getParameter("id"), -1);
        String section = safeSection(request.getParameter("section"));
        String table;
        String columns;
        Object[] values;
        if ("packing".equals(section)) {
            table = "packing_items"; columns = "category=?,name=?,quantity=?,weight=?";
            values = new Object[] {value(request, "category", "Travel essentials"), required(request, "name"),
                    positiveInteger(request, "quantity"), nonNegativeDouble(request, "weight")};
        } else if ("itinerary".equals(section)) {
            table = "itinerary"; columns = "plan_date=?,activity=?,plan_time=?,location=?";
            values = new Object[] {required(request, "date"), required(request, "activity"),
                    required(request, "time"), required(request, "location")};
        } else if ("shopping".equals(section)) {
            table = "shopping_list"; columns = "name=?,quantity=?";
            values = new Object[] {required(request, "name"), positiveInteger(request, "quantity")};
        } else if ("home".equals(section)) {
            table = "home_checklist"; columns = "task=?";
            values = new Object[] {required(request, "task")};
        } else if ("souvenirs".equals(section)) {
            table = "souvenirs"; columns = "name=?,destination=?,notes=?";
            values = new Object[] {required(request, "name"), required(request, "destination"), value(request, "notes", "")};
        } else {
            throw new IllegalArgumentException("This record cannot be edited.");
        }
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + table + " SET " + columns + " WHERE id=? AND trip_id=?")) {
            int index = bind(statement, values, 1);
            statement.setInt(index++, id);
            statement.setInt(index, tripId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Record not found for this trip.");
            }
        }
    }

    private void deleteRecord(HttpServletRequest request, int userId) throws Exception {
        int tripId = authorizedTripId(request, userId);
        String section = safeSection(request.getParameter("section"));
        String table = tableFor(section);
        int id = parseInteger(request.getParameter("id"), -1);
        String fileName = null;
        if ("memories".equals(section)) {
            try (Connection connection = Db.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT file_name FROM travel_memories WHERE id=? AND trip_id=?")) {
                statement.setInt(1, id);
                statement.setInt(2, tripId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) fileName = result.getString(1);
                }
            }
        }
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE id=? AND trip_id=?")) {
            statement.setInt(1, id);
            statement.setInt(2, tripId);
            statement.executeUpdate();
        }
        if (fileName != null) {
            Files.deleteIfExists(Db.getUploadDirectory().resolve(fileName));
        }
    }

    private void toggleRecord(HttpServletRequest request, int userId) throws Exception {
        int tripId = authorizedTripId(request, userId);
        String section = safeSection(request.getParameter("section"));
        String table = "packing".equals(section) ? "packing_items"
                : "shopping".equals(section) ? "shopping_list"
                : "home".equals(section) ? "home_checklist" : null;
        String column = "packing".equals(section) ? "packed"
                : "shopping".equals(section) ? "purchased"
                : "home".equals(section) ? "completed" : null;
        if (table == null) throw new IllegalArgumentException("Invalid checklist.");
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + table + " SET " + column + "=CASE " + column
                                + " WHEN 0 THEN 1 ELSE 0 END WHERE id=? AND trip_id=?")) {
            statement.setInt(1, parseInteger(request.getParameter("id"), -1));
            statement.setInt(2, tripId);
            statement.executeUpdate();
        }
    }

    private void uploadMemory(HttpServletRequest request, int userId) throws Exception {
        int tripId = authorizedTripId(request, userId);
        Part photo = request.getPart("photo");
        String caption = value(request, "caption", "");
        String contentType = photo.getContentType();
        String submittedName = Path.of(photo.getSubmittedFileName() == null
                ? "" : photo.getSubmittedFileName()).getFileName().toString();
        String extension = submittedName.contains(".")
                ? submittedName.substring(submittedName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        byte[] signature;
        try (java.io.InputStream input = photo.getInputStream()) {
            signature = input.readNBytes(12);
        }
        boolean validSignature = isSignature(signature, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})
                || isSignature(signature, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})
                || isSignature(signature, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                || isSignature(signature, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                || (signature.length >= 12
                && new String(signature, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(signature, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"));
        if (photo.getSize() == 0 || photo.getSize() > 5 * 1024 * 1024
                || !("image/jpeg".equals(contentType) || "image/png".equals(contentType)
                || "image/gif".equals(contentType) || "image/webp".equals(contentType))
                || !("jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension)
                || "gif".equals(extension) || "webp".equals(extension)) || !validSignature) {
            throw new IllegalArgumentException("Choose a JPG, PNG, GIF or WEBP photo smaller than 5 MB.");
        }
        String savedName = UUID.randomUUID() + "." + extension;
        Path savedPath = Db.getUploadDirectory().resolve(savedName).normalize();
        if (!savedPath.getParent().equals(Db.getUploadDirectory().toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Invalid upload name.");
        }
        photo.write(savedPath.toString());
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO travel_memories(trip_id,file_name,caption) VALUES(?,?,?)")) {
            statement.setInt(1, tripId);
            statement.setString(2, savedName);
            statement.setString(3, caption);
            statement.executeUpdate();
        } catch (Exception exception) {
            Files.deleteIfExists(savedPath);
            throw exception;
        }
    }

    private boolean isSignature(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) return false;
        }
        return true;
    }

    private int authorizedTripId(HttpServletRequest request, int userId) throws Exception {
        int tripId = parseInteger(request.getParameter("tripId"), -1);
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM trips WHERE id=? AND user_id=?")) {
            statement.setInt(1, tripId);
            statement.setInt(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Select one of your own trips first.");
            }
        }
        return tripId;
    }

    private Map<String, Object> tripById(int userId, int tripId) throws Exception {
        List<Map<String, Object>> result = query("SELECT * FROM trips WHERE id=? AND user_id=?", tripId, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    private List<Map<String, Object>> query(String sql, Object... values) throws SQLException, IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values, 1);
            try (ResultSet result = statement.executeQuery()) {
                int columns = result.getMetaData().getColumnCount();
                while (result.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        row.put(result.getMetaData().getColumnLabel(i), result.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private int count(String sql, int tripId) throws SQLException, IOException {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tripId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values, 1);
            statement.executeUpdate();
        }
    }

    private int bind(PreparedStatement statement, Object[] values, int start) throws SQLException {
        int index = start;
        for (Object value : values) {
            statement.setObject(index++, value);
        }
        return index;
    }

    private String tableFor(String section) {
        if ("packing".equals(section)) return "packing_items";
        if ("itinerary".equals(section)) return "itinerary";
        if ("shopping".equals(section)) return "shopping_list";
        if ("home".equals(section)) return "home_checklist";
        if ("souvenirs".equals(section)) return "souvenirs";
        if ("memories".equals(section)) return "travel_memories";
        throw new IllegalArgumentException("Invalid trip section.");
    }

    private String safeSection(String section) {
        if ("trips".equals(section) || "packing".equals(section) || "itinerary".equals(section)
                || "shopping".equals(section) || "home".equals(section) || "souvenirs".equals(section)
                || "memories".equals(section)) return section;
        return "dashboard";
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (isBlank(value)) throw new IllegalArgumentException("Please complete all required fields.");
        return value.trim();
    }

    private String value(HttpServletRequest request, String name, String fallback) {
        String value = request.getParameter(name);
        return value == null ? fallback : value.trim();
    }

    private int positiveInteger(HttpServletRequest request, String name) {
        int value = parseInteger(request.getParameter(name), 0);
        if (value < 1) throw new IllegalArgumentException("Quantity must be at least one.");
        return value;
    }

    private double nonNegativeDouble(HttpServletRequest request, String name) {
        try {
            String submittedValue = request.getParameter(name);
            if (submittedValue == null || submittedValue.trim().isEmpty()) {
                throw new NumberFormatException();
            }
            BigDecimal decimalValue = new BigDecimal(submittedValue.trim());
            double value = decimalValue.doubleValue();
            if (decimalValue.signum() < 0)
                throw new IllegalArgumentException("Weight must be zero or more.");
            if (!Double.isFinite(value) || (decimalValue.signum() > 0 && value == 0))
                throw new IllegalArgumentException("Enter a valid item weight.");
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid item weight.");
        }
    }

    private int parseInteger(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception exception) { return fallback; }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
