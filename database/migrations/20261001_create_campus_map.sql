BEGIN;

CREATE TABLE campus_buildings (
    id VARCHAR(100) PRIMARY KEY,
    building_number VARCHAR(255) NOT NULL,
    name TEXT NOT NULL,
    english_name TEXT,
    campus VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    description TEXT,
    aliases TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    primary_colleges TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    facilities TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    image_url TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180),
    UNIQUE (campus, building_number)
);
CREATE INDEX idx_campus_buildings_campus ON campus_buildings(campus);

CREATE TABLE campus_open_spaces (
    id VARCHAR(100) PRIMARY KEY,
    building_id VARCHAR(100) NOT NULL REFERENCES campus_buildings(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    floor TEXT,
    hint TEXT
);
CREATE INDEX idx_campus_open_spaces_building ON campus_open_spaces(building_id);

CREATE TABLE campus_smoking_areas (
    id VARCHAR(100) PRIMARY KEY,
    name TEXT NOT NULL,
    type VARCHAR(255) NOT NULL,
    campus VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    has_ashtray BOOLEAN NOT NULL,
    description TEXT,
    image_url TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_campus_smoking_areas_campus ON campus_smoking_areas(campus);

COMMIT;
