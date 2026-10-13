package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "playlist_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistComment {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID songId;
    @Column(nullable = false)
    private UUID deviceId;
    private Integer anonymousNumber;
    @Column(nullable = false, length = 1000)
    private String content;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant deletedAt;

    public PlaylistComment(UUID songId, UUID deviceId, Integer anonymousNumber, String content) {
        this.id = UUID.randomUUID();
        this.songId = songId;
        this.deviceId = deviceId;
        this.anonymousNumber = anonymousNumber;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public void delete() {
        deletedAt = Instant.now();
        content = "";
    }
}
