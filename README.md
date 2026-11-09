# BAGORITHM

BAGORITHM is a Java 17 Servlet/JSP web app for organizing trips and preparing destination-based packing lists. It uses JDBC with SQLite and is intended to run on Apache Tomcat 9.

## Run it

1. Install Java 17, Maven and Apache Tomcat 9.
2. From this folder, build the WAR:

   ```text
   mvn clean package
   ```

3. Copy `target/bagorithm.war` into Tomcat 9's `webapps` folder and start Tomcat.
4. Open `http://localhost:8080/bagorithm`.

The database and uploaded photos are created automatically on startup. By default, they are stored in the current Tomcat user's home directory under `.bagorithm` (`bagorithm.db` and `uploads`). Set the `BAGORITHM_DATA_DIR` environment variable before starting Tomcat to use another writable directory.

## Project structure

- `AppServlet` handles sign-in, trips, lists, and photo uploads.
- `Db` creates the SQLite tables and opens JDBC connections.
- `PasswordUtil` hashes account passwords with a per-user salt.
- JSP files render the welcome, sign-in, and signed-in pages; `assets/style.css` provides the travel-themed styling.

Packing recommendations are generated locally from trip duration, destination/weather keywords, and travel purpose. No paid API is needed. A network connection is used to load Bootstrap and fonts from their CDNs.
