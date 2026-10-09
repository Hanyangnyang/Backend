package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "playlist_track_artists", uniqueConstraints = {
        @UniqueConstraint(name = "uk_playlist_track_artists_pair", columnNames = {"track_id", "artist_id"}),
        @UniqueConstraint(name = "uk_playlist_track_artists_order", columnNames = {"track_id", "artist_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrackArtist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private PlaylistTrack track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private PlaylistArtist artist;

    @Column(name = "artist_order", nullable = false)
    private int artistOrder;

    public PlaylistTrackArtist(PlaylistTrack track, PlaylistArtist artist, int artistOrder) {
        this.track = track;
        this.artist = artist;
        this.artistOrder = artistOrder;
    }
}
