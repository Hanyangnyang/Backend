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
        name = "playlist_song_reactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_playlist_song_reaction", columnNames = {"song_id", "device_id", "reaction_type"})
        },
        indexes = {
                @Index(name = "idx_playlist_song_reactions_lookup", columnList = "song_id, reaction_type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSongReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private PlaylistSong song;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public PlaylistSongReaction(PlaylistSong song, UUID deviceId, ReactionType reactionType) {
        this.song = song;
        this.deviceId = deviceId;
        this.reactionType = reactionType;
    }
}
