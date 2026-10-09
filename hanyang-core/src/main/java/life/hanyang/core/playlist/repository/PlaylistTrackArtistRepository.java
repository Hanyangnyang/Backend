package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrackArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaylistTrackArtistRepository extends JpaRepository<PlaylistTrackArtist, UUID> {
    boolean existsByTrackTrackId(String trackId);
}
