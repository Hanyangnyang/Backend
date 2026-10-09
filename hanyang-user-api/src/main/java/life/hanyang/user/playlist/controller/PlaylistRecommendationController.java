package life.hanyang.user.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import life.hanyang.core.playlist.service.PlaylistRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlist/recommendations")
@RequiredArgsConstructor
@Tag(name = "플레이리스트 개인화 추천 API")
public class PlaylistRecommendationController {
    private final PlaylistRecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "기기별 가수와 대표곡 추천", description = "최근 30일의 재생·좋아요·작성 기록과 서비스 장르로 " +
            "관심 가수 최대 2명, 최근 이력에 없는 가수 최대 3명을 추천합니다. 부족한 자리는 서로 보충하고 최신 종합 주간차트로 채웁니다. " +
            "가수와 대표곡은 중복되지 않으며 0~5개를 반환합니다. 결과는 기기별 5분 캐시되어 활동이 즉시 반영되지 않을 수 있습니다. " +
            "복수 가수 모두에게 재생 점수 전체를 반영하며 좋아요·작성 점수는 나눠 반영합니다. track.artists에는 참여 순서대로 전체 가수를 제공합니다.")
    public ResponseEntity<ApiResponse<PlaylistRecommendationResponse>> getRecommendations(
            @Parameter(description = "현재 기기 UUID", required = true) @RequestParam UUID deviceId) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getRecommendations(deviceId)));
    }
}
