package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlaylistSongRepositoryCustom {
    Page<PlaylistSong> searchSongs(Genre genre, Pageable pageable);
    Page<PlaylistSong> searchSongsForAdmin(Genre genre, Boolean isDeleted, Pageable pageable);
    Page<PlaylistSong> searchSongsByTrackId(String trackId, Pageable pageable);
    Page<PlaylistSong> searchSongsWithWeight(String keyword, Pageable pageable);
}
