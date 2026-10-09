package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrackArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;

public interface PlaylistTrackArtistRepository extends JpaRepository<PlaylistTrackArtist, UUID> {
    boolean existsByTrackTrackId(String trackId);

    @Query("""
            SELECT link FROM PlaylistTrackArtist link JOIN FETCH link.artist
            WHERE link.track.trackId IN :trackIds
            ORDER BY link.track.trackId, link.artistOrder
            """)
    List<PlaylistTrackArtist> findWithArtistsByTrackIds(@Param("trackIds") List<String> trackIds);
}
