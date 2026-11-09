package com.bagorithm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private static final Path DATA_DIRECTORY = Paths.get(
            System.getenv().getOrDefault("BAGORITHM_DATA_DIR",
                    Paths.get(System.getProperty("user.home"), ".bagorithm").toString()));

    private Db() {
    }

    public static void initialize() throws Exception {
        Files.createDirectories(DATA_DIRECTORY);
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, password_salt TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS trips ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, destination TEXT NOT NULL, departure_date TEXT NOT NULL, return_date TEXT NOT NULL, "
                    + "travellers INTEGER NOT NULL, purpose TEXT NOT NULL, weather TEXT NOT NULL, max_weight REAL NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS packing_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "category TEXT NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 1, weight REAL NOT NULL DEFAULT 0, "
                    + "packed INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS itinerary ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "plan_date TEXT NOT NULL, activity TEXT NOT NULL, plan_time TEXT NOT NULL, location TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS shopping_list ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 1, purchased INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS home_checklist ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "task TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS souvenirs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "name TEXT NOT NULL, destination TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '')");
            statement.execute("CREATE TABLE IF NOT EXISTS travel_memories ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE, "
                    + "file_name TEXT NOT NULL, caption TEXT NOT NULL DEFAULT '', uploaded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public static Connection getConnection() throws SQLException, IOException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATA_DIRECTORY.resolve("bagorithm.db"));
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static Path getUploadDirectory() throws IOException {
        Path directory = DATA_DIRECTORY.resolve("uploads");
        Files.createDirectories(directory);
        return directory;
    }
}
