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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "playlist_songs", indexes = {
        @Index(name = "idx_playlist_songs_created_at", columnList = "created_at"),
        @Index(name = "idx_playlist_songs_heart_count", columnList = "heart_count"),
        @Index(name = "idx_playlist_songs_device_created", columnList = "device_id, created_at"),
        @Index(name = "idx_playlist_songs_track_id", columnList = "track_id"),
        @Index(name = "idx_playlist_songs_deleted_at", columnList = "deleted_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSong {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "album_art_url")
    private String albumArtUrl;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @ColumnDefault("0")
    @Column(name = "heart_count", nullable = false)
    private Integer heartCount = 0;

    @ColumnDefault("0")
    @Column(name = "total_play_count", nullable = false)
    private Integer totalPlayCount = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "playlist_song_genres",
            joinColumns = @JoinColumn(name = "song_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 30)
    private Set<Genre> genres = new HashSet<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PlaylistSong(
            String trackId,
            String title,
            String artist,
            String albumArtUrl,
            String comment,
            UUID deviceId,
            String ipAddress,
            Set<Genre> genres
    ) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.comment = comment;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.heartCount = 0;
        this.totalPlayCount = 0;
        this.genres = (genres != null) ? genres : new HashSet<>();
    }

    public void incrementPlayCount() {
        this.totalPlayCount = (this.totalPlayCount == null ? 0 : this.totalPlayCount) + 1;
    }

    public void incrementHeartCount() {
        this.heartCount = (this.heartCount == null ? 0 : this.heartCount) + 1;
    }

    public void decrementHeartCount() {
        if (this.heartCount != null && this.heartCount > 0) {
            this.heartCount -= 1;
        } else {
            this.heartCount = 0;
        }
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isOwnedBy(UUID deviceId) {
        return this.deviceId != null && this.deviceId.equals(deviceId);
    }
}
