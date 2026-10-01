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
    image_url TEXT ARRAY NOT NULL DEFAULT CAST(ARRAY[] AS TEXT ARRAY),
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_campus_parking_lots_campus ON campus_parking_lots(campus);

COMMIT;
