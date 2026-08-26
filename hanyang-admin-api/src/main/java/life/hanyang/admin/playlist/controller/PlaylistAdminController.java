package life.hanyang.admin.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.ReportStatus;
import life.hanyang.core.playlist.dto.PlaylistReportProcessRequest;
import life.hanyang.core.playlist.dto.PlaylistSongReportResponse;
import life.hanyang.core.playlist.dto.PlaylistSongResponse;
import life.hanyang.core.playlist.service.PlaylistAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/playlist")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 에리카 플레이리스트 관리 API", description = "등록된 곡 및 접수된 신고를 관리자 권한으로 조회하고 처리합니다.")
public class PlaylistAdminController {

    private final PlaylistAdminService playlistAdminService;

    @Operation(summary = "곡 목록 조회 (장르, 삭제 여부 필터)", description = "플레이리스트 전체 곡을 조회합니다. 장르 및 소프트 삭제 여부로 필터링할 수 있습니다.")
    @GetMapping("/songs")
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getSongs(
            @Parameter(description = "장르 필터")
            @RequestParam(required = false) Genre genre,
            @Parameter(description = "삭제 여부 (true: 삭제된 곡만, false: 활성 곡만, 미입력: 전체)")
            @RequestParam(required = false) Boolean isDeleted,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistAdminService.getSongsForAdmin(genre, isDeleted, pageable);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(summary = "곡 강제 삭제 (소프트 딜리트)", description = "관리자 권한으로 부적절한 곡을 플레이리스트에서 삭제(숨김) 처리합니다.")
    @DeleteMapping("/songs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSong(@PathVariable UUID id) {
        playlistAdminService.deleteSongByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "신고 접수 목록 조회 (처리 상태별 필터)", description = "접수된 곡 신고 목록을 상태별(PENDING, REVIEWED, DISMISSED)로 페이징 조회합니다.")
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<PlaylistSongReportResponse>>> getReports(
            @Parameter(description = "신고 처리 상태")
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PlaylistSongReportResponse> reports = playlistAdminService.getReports(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "신고 처리 (상태 변경 및 관리자 메모)", description = "신고 건을 검토 완료(REVIEWED) 또는 기각(DISMISSED) 처리하고 관리자 메모를 작성합니다.")
    @PatchMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<PlaylistSongReportResponse>> processReport(
            @PathVariable UUID id,
            @Valid @RequestBody PlaylistReportProcessRequest request
    ) {
        PlaylistSongReportResponse response = playlistAdminService.processReport(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
