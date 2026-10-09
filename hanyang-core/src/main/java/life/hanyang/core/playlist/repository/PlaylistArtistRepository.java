package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PlaylistArtistRepository extends JpaRepository<PlaylistArtist, UUID> {
    Optional<PlaylistArtist> findBySpotifyArtistId(String spotifyArtistId);
    List<PlaylistArtist> findBySpotifyArtistIdIn(List<String> spotifyArtistIds);

    // PostgreSQL upsert prevents duplicate artists across simultaneous track registrations.
    // An empty image response must not erase a previously available artist photo.
    @Modifying
    @Query(value = """
            INSERT INTO playlist_artists (id, spotify_artist_id, name, image_url, created_at, updated_at)
            VALUES (:id, :spotifyArtistId, :name, :imageUrl, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (spotify_artist_id) DO UPDATE
            SET name = EXCLUDED.name,
                image_url = COALESCE(EXCLUDED.image_url, playlist_artists.image_url),
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsert(@Param("id") UUID id, @Param("spotifyArtistId") String spotifyArtistId,
                @Param("name") String name, @Param("imageUrl") String imageUrl);
}
