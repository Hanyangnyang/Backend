package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSongReaction;
import life.hanyang.core.playlist.domain.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlaylistSongReactionRepository extends JpaRepository<PlaylistSongReaction, UUID> {

    Optional<PlaylistSongReaction> findBySongIdAndDeviceIdAndReactionType(
            UUID songId,
            UUID deviceId,
            ReactionType reactionType
    );

    @Modifying
    @Query("DELETE FROM PlaylistSongReaction r WHERE r.song.id = :songId AND r.deviceId = :deviceId AND r.reactionType = :reactionType")
    void deleteBySongIdAndDeviceIdAndReactionType(
            @Param("songId") UUID songId,
            @Param("deviceId") UUID deviceId,
            @Param("reactionType") ReactionType reactionType
    );

    /**
     * 특정 곡의 10대 이모지별 누적 개수 집계
     */
    @Query("SELECT r.reactionType, COUNT(r.id) FROM PlaylistSongReaction r WHERE r.song.id = :songId GROUP BY r.reactionType")
    List<Object[]> countReactionsBySongId(@Param("songId") UUID songId);

    /**
     * 특정 기기가 특정 곡에 누른 이모지 목록 조회
     */
    @Query("SELECT r.reactionType FROM PlaylistSongReaction r WHERE r.deviceId = :deviceId AND r.song.id = :songId")
    Set<ReactionType> findUserReactionTypesByDeviceIdAndSongId(
            @Param("deviceId") UUID deviceId,
            @Param("songId") UUID songId
    );

    /**
     * N+1 방지: 여러 곡의 이모지별 누적 개수를 1번의 쿼리로 일괄 집계 ([0]: songId, [1]: reactionType, [2]: count)
     */
    @Query("SELECT r.song.id, r.reactionType, COUNT(r.id) FROM PlaylistSongReaction r WHERE r.song.id IN :songIds GROUP BY r.song.id, r.reactionType")
    List<Object[]> countReactionsBySongIdIn(@Param("songIds") List<UUID> songIds);

    /**
     * N+1 방지: 여러 곡에 대해 특정 기기가 누른 이모지 목록을 1번의 쿼리로 일괄 조회 ([0]: songId, [1]: reactionType)
     */
    @Query("SELECT r.song.id, r.reactionType FROM PlaylistSongReaction r WHERE r.deviceId = :deviceId AND r.song.id IN :songIds")
    List<Object[]> findUserReactionsByDeviceIdAndSongIdIn(
            @Param("deviceId") UUID deviceId,
            @Param("songIds") List<UUID> songIds
    );
}
