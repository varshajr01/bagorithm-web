# Bagorithm Web

Bagorithm Web is a separate Java 17/Spring Boot application. It uses its own PostgreSQL database and never connects to or modifies the desktop Swing application's SQLite database.

## Requirements

- JDK 17
- Maven 3.8+
- PostgreSQL 14+ for local development

## Local development

1. Create a local database and login:

   ```sql
   CREATE ROLE bagorithm LOGIN PASSWORD 'choose-a-local-password';
   CREATE DATABASE bagorithm_web OWNER bagorithm;
   ```

2. Set connection values in PowerShell:

   ```powershell
   $env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/bagorithm_web'
   $env:DATABASE_USERNAME = 'bagorithm'
   $env:DATABASE_PASSWORD = 'choose-a-local-password'
   $env:REMEMBER_ME_KEY = 'use-a-long-random-local-secret'
   $env:UPLOAD_DIR = 'D:\bagorithm-web-uploads'
   ```

3. From this directory, run:

   ```powershell
   mvn clean test
   mvn spring-boot:run
   ```

   Open `http://localhost:8080`. Flyway applies the SQL migrations on startup. V2 adds packing-item selection without deleting existing rows.

## Deploy to Render

Deployment is not performed by this project. The following steps create a new Render service and database; review Render's current plan prices before confirming resources. The included [`render.yaml`](./render.yaml) is a Render Blueprint that configures a Docker web service, managed PostgreSQL, a 1 GB persistent disk for uploads, a production profile, and a generated remember-me key.

### 1. Put the web project in a Git repository

Push `bagorithm-web` to a Git provider connected to Render. You can push this folder as its own repository or use the existing repository and set the service's root directory to `bagorithm-web`. The Java Swing source and SQLite file are not needed by this service.

### 2. Create the Blueprint resources

In the Render Dashboard, choose **New → Blueprint**, connect the repository, and select the branch containing `render.yaml`. Review the proposed resources before applying:

- **Web service:** `bagorithm-web`, built with the included Dockerfile on the Starter plan.
- **PostgreSQL:** `bagorithm-db`, PostgreSQL 16 on the Basic 256 MB plan. The Blueprint pins 16 to match the PostgreSQL support range of the currently pinned Flyway version.
- **Persistent disk:** mounted at `/var/data`, used for uploaded photos.

The web service and database are configured in the same region. Render's Blueprint supplies the managed database's internal connection string as `DATABASE_URL`; the application converts Render's `postgres://`/`postgresql://` URI to a JDBC URL at startup and reads its credentials from that URI. No database password is stored in this repository.

Review current plan availability, resource limits, and pricing in Render before creating paid resources. The database and persistent disk are important for retaining production records and photos; do not remove or recreate them to troubleshoot deployments.

### 3. Confirm environment variables and storage

The Blueprint sets these values:

| Variable / setting | Value source | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Blueprint: `prod` | Enables production security, caching, proxy, and migration settings. |
| `DATABASE_URL` | Render database `connectionString` | Internal PostgreSQL URI, including credentials; provided by the Blueprint, not committed. |
| `REMEMBER_ME_KEY` | Render generates a secret on initial creation | Stable signing key for Remember Me cookies. Keep it private and do not rotate casually; rotation invalidates existing Remember Me cookies. |
| `UPLOAD_DIR` | Blueprint: `/var/data/uploads` | Location for souvenir and memory image files on the persistent disk. |
| `PORT` | Render sets it for the web service | Spring Boot listens on Render's assigned port. |
| `server.servlet.session.cookie.secure` | Production profile: `true` | Sends session cookies only over HTTPS. |

For manual service creation, use the same environment variables. In Render's **Environment** page, obtain `DATABASE_URL` from the linked database's **Internal Database URL** (or connect the database using Render's Blueprint reference). Keep this value private because it contains database credentials. Set `SPRING_PROFILES_ACTIVE=prod`, set `UPLOAD_DIR=/var/data/uploads`, and generate `REMEMBER_ME_KEY` in Render's environment UI. Attach a persistent disk at `/var/data`. Render terminates public TLS and forwards requests to the container; the production profile enables forwarded-header handling and secure cookies.

For local development with a JDBC URL, use `DATABASE_USERNAME` and `DATABASE_PASSWORD`. These are optional when using Render's URI-style `DATABASE_URL`, because the URI includes credentials. Never commit secrets, paste them into logs, or add a production `.env` file to Git.

### 4. Deploy and verify

After confirming the Blueprint, let Render build and start the service. Check the service's **Events** and **Logs** for a successful image build, database connection, completed Flyway migrations, and a healthy `/welcome` check. Render provides the HTTPS service URL in the Dashboard after creation; this README does not claim that a service has been deployed or reserve a URL.

Open the Render-provided HTTPS URL and register a test account. Create a trip, add a souvenir or travel memory photo, and verify that the photo remains available after a service restart. Confirm database backups and disk persistence are enabled and understood before inviting users.

### Required application configuration

| Name | Required | Where to get/set it |
|---|---:|---|
| `DATABASE_URL` | Yes | Render PostgreSQL internal connection string, or local JDBC URL. On Render, reference the database from the Blueprint; do not copy credentials into source. |
| `DATABASE_USERNAME` | Local JDBC only | The PostgreSQL role created locally. Render's connection URI already contains its username. |
| `DATABASE_PASSWORD` | Local JDBC only | The local PostgreSQL role password. Render's connection URI already contains its password. |
| `SPRING_PROFILES_ACTIVE` | Production | Set to `prod` in Render. The profile enforces secure cookies and production migration behavior. |
| `REMEMBER_ME_KEY` | Production | Generate a long random secret in Render's Environment settings. Startup fails under the production profile if it is missing. |
| `UPLOAD_DIR` | Production | `/var/data/uploads`; place this beneath the mounted persistent disk. |
| `PORT` | Render supplies | The platform-assigned HTTP port; the Docker entry point and Spring configuration use it. |
| `COOKIE_SECURE` | Optional override | The `prod` profile sets secure cookies to `true`; do not set it to `false` on a public HTTPS deployment. |
| `spring.flyway.*` | Defaults in production profile | Flyway is enabled, validates before migrating, disallows `clean`, and disallows out-of-order migrations. |

### Database migrations and rollout safety

Flyway is enabled for the web application and applies versioned files under `src/main/resources/db/migration` at startup. Production settings validate the migration history, disable Flyway clean, and reject out-of-order migrations. Treat applied migrations as immutable: add a new forward-only migration for schema changes. Back up the Render database before a release that changes the schema, and test that release against a separate database first. Do not edit or remove existing migration files to change a deployed schema.

## Build and run a production artifact locally

Run the same Java 17 package build used to validate the application:

```text
mvn clean verify
```

The included Dockerfile builds the executable JAR and runs it on Java 17. To run the packaged artifact locally, set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `REMEMBER_ME_KEY`, and `UPLOAD_DIR`, then run:

```text
java -jar target/bagorithm-web-1.0.0.jar
```

For local runs, leave the `prod` profile inactive unless you have set a persistent `REMEMBER_ME_KEY` and HTTPS-aware proxy configuration.

## Features

- BCrypt account registration, login, Remember Me, and logout.
- Private trip records scoped to the signed-in account.
- Destination, weather, duration, and purpose-based packing suggestions.
- Packing list editing, packed/selected status, item weight and quantity editing, current total, capacity and remaining-capacity summary.
- Previous trips and countdowns based on departure dates.
- Itinerary, shopping, souvenir, home checklist, and travel memory pages scoped to each trip.
- Souvenir and travel-memory photo uploads, stored under `UPLOAD_DIR` and served only to authenticated owners.
- Responsive Thymeleaf pages, locally bundled travel photos, Bootstrap WebJar, and custom CSS.

Images are limited to JPEG, PNG, or GIF, up to 5 MB each, and decoded before storage. On Render, the disk mounted at `/var/data` preserves files across service deploys and restarts. A disk is local to its service instance and is not shared across multiple web instances; configure a shared object-storage service before scaling horizontally.

## Original desktop data

The desktop SQLite file is intentionally not migrated or modified by this web app. Its trip records have no owner relationship, and its password hashes are not BCrypt hashes. Automatically assigning those trips or converting credentials could expose data or change desktop login semantics. Create separate web accounts and trips in PostgreSQL.
