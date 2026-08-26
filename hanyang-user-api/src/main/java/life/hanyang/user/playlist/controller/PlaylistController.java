package life.hanyang.user.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlist/songs")
@RequiredArgsConstructor
@Tag(name = "에리카 플레이리스트 API", description = "사용자 곡 추천, 피드 조회, 좋아요 토글, 신고 기능을 제공합니다.")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(summary = "곡 추천 및 등록", description = "Spotify 곡 정보와 1~3개의 장르 태그 및 코멘트를 입력하여 플레이리스트에 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistSongResponse>> createSong(
            @Valid @RequestBody PlaylistSongCreateRequest requestDto,
            HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);
        PlaylistSongResponse response = playlistService.createSong(requestDto, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "피드 곡 목록 조회", description = "최신순으로 등록된 곡 목록을 페이징 조회합니다. 장르 필터링 및 현재 사용자 좋아요(isLiked) 여부를 지원합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getFeedSongs(
            @Parameter(description = "장르 필터 (미입력 시 전체 조회)")
            @RequestParam(required = false) Genre genre,
            @Parameter(description = "현재 로그인 사용자 ID (좋아요 누름 여부 계산용)")
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistService.getFeedSongs(genre, pageable, userId);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }


    @Operation(summary = "좋아요 토글", description = "좋아요를 등록하거나 취소합니다. 동시성 제어 및 원자적 카운트 증감이 적용됩니다.")
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<PlaylistLikeToggleResponse>> toggleLike(
            @PathVariable UUID id,
            @Valid @RequestBody PlaylistLikeToggleRequest request
    ) {
        PlaylistLikeToggleResponse response = playlistService.toggleLike(id, request.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내가 좋아요 누른 곡 목록 조회", description = "사용자가 좋아요를 누른 곡 목록을 최신순으로 페이징 조회합니다.")
    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getLikedSongs(
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistService.getLikedSongs(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(summary = "곡 신고하기", description = "부적절하거나 문제가 있는 곡을 신고 접수합니다.")
    @PostMapping("/{id}/reports")
    public ResponseEntity<ApiResponse<PlaylistSongReportResponse>> reportSong(
            @PathVariable UUID id,
            @Valid @RequestBody PlaylistSongReportRequest request
    ) {
        PlaylistSongReportResponse response = playlistService.reportSong(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
