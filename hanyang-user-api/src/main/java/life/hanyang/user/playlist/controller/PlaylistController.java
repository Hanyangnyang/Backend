package life.hanyang.user.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Operation(
            summary = "곡 추천 및 등록",
            description = "Spotify 곡 정보(trackId, title, artist, albumArtUrl)와 1~3개의 장르 태그 및 추천 코멘트를 입력하여 플레이리스트에 등록합니다.\n\n" +
                    "• **장르 종류**: KPOP(K-POP), BAND(밴드), ROCK(락), R_AND_B(R&B), HIPHOP(힙합), INDIE(인디), BALLAD(발라드), POP(POP), JPOP(J-POP), OTHER(기타)\n" +
                    "• **장르 선택 수**: 최소 1개 ~ 최대 3개\n" +
                    "• **등록 제한**: 1일 최대 3곡 / 최근 7일 내 동일 곡 중복 추천 불가\n" +
                    "• **등록자 IP**: 클라이언트 헤더를 통해 백엔드에서 자동으로 수집/기록됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistSongResponse>> createSong(
            @Valid @RequestBody PlaylistSongCreateRequest requestDto,
            HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);
        PlaylistSongResponse response = playlistService.createSong(requestDto, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "곡 작성 전 사용자 기기 상태 조회 (등록 제한 사전 확인)",
            description = "사용자가 곡 등록 화면에 진입할 때 오늘 남은 등록 가능 횟수 및 최근 7일 내 이미 추천한 곡 목록을 조회합니다.\n\n" +
                    "• **canCreate**: 오늘 추가 등록 가능 여부 (오늘 등록 수 < 3)\n" +
                    "• **dailyCount**: 오늘 이미 등록한 곡 수 (0~3)\n" +
                    "• **remainingCount**: 오늘 남은 등록 가능 횟수\n" +
                    "• **recentTrackIdsIn7Days**: 최근 7일 이내에 이미 추천한 Spotify 트랙 ID 목록 (검색 시 중복 선택 방지용)"
    )
    @GetMapping("/creation-status")
    public ResponseEntity<ApiResponse<PlaylistCreationStatusResponse>> getCreationStatus(
            @Parameter(description = "기기 식별자 ID (UUID)", required = true)
            @RequestParam UUID deviceId
    ) {
        PlaylistCreationStatusResponse response = playlistService.getCreationStatus(deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "피드 곡 목록 조회",
            description = "등록된 곡 목록을 최신순으로 페이징 조회합니다.\n\n" +
                    "• **genre**: 특정 장르만 필터링 (KPOP, BAND, ROCK, R_AND_B, HIPHOP, INDIE, BALLAD, POP, JPOP, OTHER). 미입력 시 전체 장르 조회\n" +
                    "• **deviceId**: 현재 기기 식별자 ID 전달 시 내가 누른 좋아요 여부(`isLiked: true/false`)를 계산하여 반환\n" +
                    "• **page/size**: 0부터 시작하는 페이지 번호와 페이지당 개수 (기본값: size=20)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getFeedSongs(
            @Parameter(description = "장르 필터 (미입력 시 전체 조회, 예: KPOP, INDIE, ROCK 등)")
            @RequestParam(required = false) Genre genre,
            @Parameter(description = "현재 로그인 기기 식별자 ID (좋아요 누름 여부 isLiked 계산용)")
            @RequestParam(required = false) UUID deviceId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistService.getFeedSongs(genre, pageable, deviceId);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(
            summary = "내가 작성한 추천글 목록 조회",
            description = "사용자 기기(deviceId)가 등록한 추천글 목록을 페이징 조회합니다.\n\n" +
                    "• **deviceId**: 현재 기기 식별자 ID (필수)\n" +
                    "• **direction**: 작성일시(createdAt) 기준 정렬 방향 (`DESC`: 최신순, `ASC`: 오래된순 / 기본값: `DESC`)\n" +
                    "• **page**: 0부터 시작하는 페이지 번호 (기본값: 0)\n" +
                    "• **size**: 페이지당 개수 (기본값: 20)"
    )
    @GetMapping("/my-songs")
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getMySongs(
            @Parameter(description = "기기 식별자 ID (UUID)", required = true)
            @RequestParam UUID deviceId,
            @Parameter(description = "정렬 방향 (DESC: 최신순, ASC: 오래된순)", example = "DESC")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));
        Page<PlaylistSongResponse> songs = playlistService.getMySongs(deviceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(
            summary = "추천글 가중치 통합 검색 (제목/가수/코멘트)",
            description = "검색 키워드로 추천글을 검색합니다. 곡 제목(100점) > 가수명(80점) > 코멘트 내용(20점) 가중치 점수 및 하트 수 순으로 정렬됩니다.\n\n" +
                    "• **keyword**: 검색어 (곡명, 가수명, 코멘트 내용)\n" +
                    "• **deviceId**: 현재 기기 ID 전달 시 각 글의 `isLiked: true/false` 반환\n" +
                    "• **page/size**: 페이징 정보 (기본값: size=20)"
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> searchSongs(
            @Parameter(description = "검색 키워드 (곡명, 가수명, 코멘트 본문)", required = true)
            @RequestParam String keyword,
            @Parameter(description = "현재 로그인 기기 식별자 ID (좋아요 누름 여부 isLiked 계산용)")
            @RequestParam(required = false) UUID deviceId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistService.searchSongsWithWeight(keyword, pageable, deviceId);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(
            summary = "음원 트랙 목록 검색 (검색 화면 상단 트랙 섹션용)",
            description = "검색 키워드로 등록된 음원 마스터(Spotify 곡) 목록을 검색합니다. 곡명 또는 가수명이 일치하는 트랙과 해당 음원에 달린 총 추천글 수/하트 총합을 반환합니다.\n\n" +
                    "• **keyword**: 검색어 (곡명, 가수명)\n" +
                    "• **page/size**: 페이징 정보 (기본값: size=10)"
    )
    @GetMapping("/tracks/search")
    public ResponseEntity<ApiResponse<Page<PlaylistTrackSearchResponse>>> searchTracks(
            @Parameter(description = "검색 키워드 (곡명, 가수명)", required = true)
            @RequestParam String keyword,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PlaylistTrackSearchResponse> tracks = playlistService.searchTracks(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(tracks));
    }

    @Operation(
            summary = "특정 곡의 추천글 모아보기 (상세 조회)",
            description = "특정 음원(trackId)의 메타데이터 및 해당 곡에 학생들이 작성한 추천글 목록을 인기순(기본값) 또는 최신순으로 페이징 조회합니다.\n\n" +
                    "• **deviceId**: 현재 기기 식별자 ID 전달 시 각 글의 `isLiked: true/false` 반환\n" +
                    "• **sort**: 인기순(기본값: `heartCount,desc`) / 최신순(`createdAt,desc`)\n" +
                    "• **page/size**: 0부터 시작하는 페이지 번호와 페이지당 개수 (기본값: size=20)"
    )
    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<ApiResponse<PlaylistTrackDetailResponse>> getTrackDetailAndSongs(
            @Parameter(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
            @PathVariable String trackId,
            @Parameter(description = "현재 로그인 기기 식별자 ID (좋아요 누름 여부 isLiked 계산용)")
            @RequestParam(required = false) UUID deviceId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        PlaylistTrackDetailResponse response = playlistService.getTrackDetailAndSongs(trackId, pageable, deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "음원 재생수 기록 (인기 차트 집계용)",
            description = "사용자가 특정 음원의 재생 버튼(▶️)을 누를 때 호출하여 오늘 일자별 재생수를 1 증가시킵니다."
    )
    @PostMapping("/tracks/{trackId}/play")
    public ResponseEntity<ApiResponse<Void>> recordTrackPlay(
            @Parameter(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
            @PathVariable String trackId
    ) {
        playlistService.recordTrackPlay(trackId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "인기 차트 순위 조회 (실시간 급상승 / 주간 / 월간)",
            description = "한양대 에리카 인기 음악 차트 순위를 조회합니다.\n\n" +
                    "• **type**: `RISING`(실시간 급상승 - 직전 정각 기준 24시간 + 3시간 부스터, 기본값), `WEEKLY`(주간 차트 - 지난주 월~일), `MONTHLY`(월간 차트 - 지난달 1일~말일)"
    )
    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<PlaylistChartResponse>> getChart(
            @Parameter(description = "차트 유형 (RISING, WEEKLY, MONTHLY)", example = "RISING")
            @RequestParam(required = false, defaultValue = "RISING") ChartType type
    ) {
        PlaylistChartResponse response = playlistService.getChart(type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "추천글 단건 상세 조회",
            description = "특정 추천글 ID(UUID)로 상세 정보를 단건 조회합니다. 딥링크, SNS 공유, 알림 연동에 사용됩니다.\n\n" +
                    "• **deviceId**: 현재 기기 식별자 ID 전달 시 내가 누른 좋아요 여부(`isLiked: true/false`)를 계산하여 반환"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistSongResponse>> getSong(
            @PathVariable UUID id,
            @Parameter(description = "현재 로그인 기기 식별자 ID (좋아요 누름 여부 isLiked 계산용)")
            @RequestParam(required = false) UUID deviceId
    ) {
        PlaylistSongResponse response = playlistService.getSong(id, deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "좋아요 토글",
            description = "좋아요를 등록하거나 취소합니다. 동시성 제어 및 원자적 카운트 증감이 적용됩니다.\n\n" +
                    "• 이미 좋아요를 누른 상태 ➡️ 취소 처리 (`isLiked: false`, `heartCount` -1)\n" +
                    "• 아직 누르지 않은 상태 ➡️ 등록 처리 (`isLiked: true`, `heartCount` +1)"
    )
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<PlaylistLikeToggleResponse>> toggleLike(
            @PathVariable UUID id,
            @Valid @RequestBody PlaylistLikeToggleRequest request
    ) {
        PlaylistLikeToggleResponse response = playlistService.toggleLike(id, request.deviceId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "이모지 리액션 토글 (9종)",
            description = "특정 추천글에 9대 이모지 리액션(LOVE, EMOTIONAL, BITTERSWEET, COOL, FIRE, ROCK, DANCE, THUMBS_UP, BEER)을 남기거나 취소합니다.\n\n" +
                    "• 이미 누른 이모지 ➡️ 취소 처리 (`isReacted: false`)\n" +
                    "• 아직 누르지 않은 이모지 ➡️ 추가 처리 (`isReacted: true`)\n" +
                    "• 응답으로 해당 곡의 9개 이모지 전체 최신 카운트 목록을 반환합니다."
    )
    @PostMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<PlaylistReactionToggleResponse>> toggleReaction(
            @PathVariable UUID id,
            @Valid @RequestBody PlaylistReactionToggleRequest request
    ) {
        PlaylistReactionToggleResponse response = playlistService.toggleReaction(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내가 좋아요 누른 곡 목록 조회",
            description = "사용자가 좋아요를 누른 곡 목록을 최신순으로 페이징 조회합니다."
    )
    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<Page<PlaylistSongResponse>>> getLikedSongs(
            @Parameter(description = "기기 식별자 ID (UUID)", required = true)
            @RequestParam UUID deviceId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PlaylistSongResponse> songs = playlistService.getLikedSongs(deviceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(songs));
    }

    @Operation(
            summary = "곡 신고하기",
            description = "부적절한 내용이나 비속어가 포함된 곡을 운영자에게 신고 접수합니다."
    )
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
