BEGIN;

CREATE TABLE campus_parking_lots (
    id VARCHAR(100) PRIMARY KEY,
    name TEXT NOT NULL,
    campus VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity INTEGER CHECK (capacity >= 0),
    address TEXT,
    description TEXT,
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_campus_parking_lots_campus ON campus_parking_lots(campus);

CREATE TABLE campus_parking_lots_image_url (
    owner_id VARCHAR(100) NOT NULL REFERENCES campus_parking_lots(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (owner_id, sort_order)
);

COMMIT;
