package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.dto.PlaylistTrackSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlaylistTrackRepositoryCustom {
    Page<PlaylistTrackSearchResponse> searchTracks(String keyword, Pageable pageable);
}
