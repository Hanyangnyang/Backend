package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "playlist_artists", uniqueConstraints =
        @UniqueConstraint(name = "uk_playlist_artists_spotify_id", columnNames = "spotify_artist_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistArtist {
    @Id
    private UUID id;

    @Column(name = "spotify_artist_id", nullable = false, length = 22)
    private String spotifyArtistId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
