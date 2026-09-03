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
@Table(name = "playlist_track_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_playlist_track_likes_track_device", columnNames = {"track_id", "device_id"})
}, indexes = {
        @Index(name = "idx_playlist_track_likes_device_created", columnList = "device_id, created_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrackLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private PlaylistTrack track;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public PlaylistTrackLike(PlaylistTrack track, UUID deviceId) {
        this.track = track;
        this.deviceId = deviceId;
    }
}
