package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Entity
@Table(name = "playlist_tracks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrack {

    @Id
    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "album_art_url")
    private String albumArtUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PlaylistTrack(String trackId, String title, String artist, String albumArtUrl) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
    }

    public void updateMetadata(String title, String artist, String albumArtUrl) {
        if (title != null) this.title = title;
        if (artist != null) this.artist = artist;
        if (albumArtUrl != null) this.albumArtUrl = albumArtUrl;
    }
}
