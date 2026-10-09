-- Phase 1: additive schema only. Keep playlist_tracks.artist until backfill and query conversion are verified.
-- Apply to the approved target PostgreSQL database BEFORE deploying the new entities (ddl-auto: validate).
BEGIN;

CREATE TABLE playlist_artists (
    id UUID PRIMARY KEY,
    spotify_artist_id VARCHAR(22) NOT NULL,
    name TEXT NOT NULL,
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_playlist_artists_spotify_id UNIQUE (spotify_artist_id),
    CONSTRAINT ck_playlist_artists_spotify_id CHECK (spotify_artist_id ~ '^[A-Za-z0-9]{22}$'),
    CONSTRAINT ck_playlist_artists_name CHECK (length(trim(name)) > 0)
);

CREATE TABLE playlist_track_artists (
    id UUID PRIMARY KEY,
    track_id VARCHAR(255) NOT NULL REFERENCES playlist_tracks(track_id),
    artist_id UUID NOT NULL REFERENCES playlist_artists(id),
    artist_order INTEGER NOT NULL CHECK (artist_order >= 0),
    CONSTRAINT uk_playlist_track_artists_pair UNIQUE (track_id, artist_id),
    CONSTRAINT uk_playlist_track_artists_order UNIQUE (track_id, artist_order)
);

CREATE INDEX idx_playlist_track_artists_artist ON playlist_track_artists (artist_id, track_id);

COMMIT;
