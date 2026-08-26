package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.domain.ReportStatus;
import life.hanyang.core.playlist.dto.PlaylistReportProcessRequest;
import life.hanyang.core.playlist.dto.PlaylistSongReportResponse;
import life.hanyang.core.playlist.dto.PlaylistSongResponse;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistAdminService {

    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongReportRepository playlistSongReportRepository;

    /**
     * 1. 관리자 곡 목록 조회 (장르 필터, 삭제 여부 필터, 최신순 페이징)
     */
    public Page<PlaylistSongResponse> getSongsForAdmin(Genre genre, Boolean isDeleted, Pageable pageable) {
        Page<PlaylistSong> songs = playlistSongRepository.searchSongsForAdmin(genre, isDeleted, pageable);
        List<PlaylistSongResponse> responses = songs.getContent().stream()
                .map(song -> PlaylistSongResponse.of(song, false))
                .toList();

        return new PageImpl<>(responses, pageable, songs.getTotalElements());
    }

    /**
     * 2. 관리자 권한 강제 삭제 (소프트 딜리트)
     */
    @Transactional
    public void deleteSongByAdmin(UUID songId) {
        PlaylistSong song = playlistSongRepository.findById(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 곡입니다. id: " + songId));

        song.softDelete();
        log.info("[PlaylistAdmin] 관리자에 의한 곡 삭제 완료 - songId: {}, title: {}", songId, song.getTitle());
    }

    /**
     * 3. 신고 접수 목록 조회 (상태 필터, 최신순 페이징)
     */
    public Page<PlaylistSongReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<PlaylistSongReport> reports = (status != null)
                ? playlistSongReportRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : playlistSongReportRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PlaylistSongReportResponse> responses = reports.getContent().stream()
                .map(PlaylistSongReportResponse::from)
                .toList();

        return new PageImpl<>(responses, pageable, reports.getTotalElements());
    }

    /**
     * 4. 신고 처리 (상태 변경 및 조치 메모 기록)
     */
    @Transactional
    public PlaylistSongReportResponse processReport(UUID reportId, PlaylistReportProcessRequest request) {
        PlaylistSongReport report = playlistSongReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 신고 ID입니다: " + reportId));

        report.process(request.status(), request.adminMemo());
        log.info("[PlaylistAdmin] 신고 처리 완료 - reportId: {}, status: {}", reportId, request.status());

        return PlaylistSongReportResponse.from(report);
    }
}
