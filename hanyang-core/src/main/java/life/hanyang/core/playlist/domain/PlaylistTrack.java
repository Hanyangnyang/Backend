package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "track", fetch = FetchType.LAZY)
    @OrderBy("artistOrder ASC")
    @BatchSize(size = 100)
    private List<PlaylistTrackArtist> artistLinks = new ArrayList<>();

    @Column(name = "album_art_url")
    private String albumArtUrl;

    @ColumnDefault("0")
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

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

    public String getArtist() {
        // Keep the legacy value until asynchronous sync/backfill has connected this track.
        if (artistLinks.isEmpty()) return artist;
        return artistLinks.stream()
                .sorted(Comparator.comparingInt(PlaylistTrackArtist::getArtistOrder))
                .map(link -> link.getArtist().getName())
                .collect(Collectors.joining(", "));
    }
}
