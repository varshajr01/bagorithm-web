CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    destination VARCHAR(160) NOT NULL,
    departure_date DATE,
    return_date DATE,
    duration_days INTEGER NOT NULL DEFAULT 1 CHECK (duration_days > 0),
    weather VARCHAR(40) NOT NULL,
    purpose VARCHAR(80) NOT NULL,
    max_capacity_kg NUMERIC(8,2) NOT NULL CHECK (max_capacity_kg > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX trips_user_departure_idx ON trips(user_id, departure_date);

CREATE TABLE packing_items (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_weight_grams NUMERIC(10,2) NOT NULL CHECK (unit_weight_grams > 0),
    packed BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (trip_id, name)
);

CREATE TABLE itinerary_activities (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    activity_name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    planned_date DATE NOT NULL,
    planned_time TIME NOT NULL,
    location VARCHAR(200) NOT NULL DEFAULT '',
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('High','Medium','Low')),
    completed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX itinerary_trip_date_idx ON itinerary_activities(trip_id, planned_date, planned_time);

CREATE TABLE shopping_items (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    item_name VARCHAR(160) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT '',
    estimated_price NUMERIC(12,2) NOT NULL CHECK (estimated_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    purchased BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE souvenirs (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    collected_place VARCHAR(200) NOT NULL DEFAULT '',
    collection_date DATE NOT NULL,
    photo_path TEXT
);

CREATE TABLE home_checklist_tasks (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    task_name VARCHAR(200) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (trip_id, task_name)
);

CREATE TABLE travel_memories (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    experience TEXT NOT NULL DEFAULT '',
    favourite_places TEXT NOT NULL DEFAULT '',
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE travel_memory_photos (
    id BIGSERIAL PRIMARY KEY,
    memory_id BIGINT NOT NULL REFERENCES travel_memories(id) ON DELETE CASCADE,
    photo_path TEXT NOT NULL
);
