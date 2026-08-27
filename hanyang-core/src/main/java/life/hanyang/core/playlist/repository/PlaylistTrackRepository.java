package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, String> {
}
