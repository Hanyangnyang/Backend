package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlaylistSearchEventRepository extends JpaRepository<PlaylistSearchEvent, UUID> { }
