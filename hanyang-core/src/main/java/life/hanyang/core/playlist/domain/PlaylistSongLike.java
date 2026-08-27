package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "playlist_song_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_playlist_song_likes_song_device", columnNames = {"song_id", "device_id"})
        },
        indexes = {
                @Index(name = "idx_playlist_song_likes_device_created", columnList = "device_id, created_at DESC"),
                @Index(name = "idx_playlist_song_likes_created_at", columnList = "created_at DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSongLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private PlaylistSong song;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public PlaylistSongLike(PlaylistSong song, UUID deviceId) {
        this.song = song;
        this.deviceId = deviceId;
    }
}
