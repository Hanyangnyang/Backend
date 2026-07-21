package life.hanyang.user.subway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.subway.dto.SubwaySearchRequest;
import life.hanyang.core.subway.dto.SubwayTimetableResponse;
import life.hanyang.core.subway.service.SubwayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "지하철 API", description = "지하철 관련 정보를 제공합니다")
public class SubwayController {
    private final SubwayService subwayService;

    public SubwayController(SubwayService subwayService) {
        this.subwayService = subwayService;
    }

    @GetMapping("/api/v1/subway")
    @Operation(summary= "지하철 시간표 정보를 조회합니다. 입력되지 않은 필드는 값을 모두 불러옵니다.")
    public ResponseEntity<ApiResponse<List<SubwayTimetableResponse>>> getTimetable(
            SubwaySearchRequest request
    ) {
        List<SubwayTimetableResponse> result = subwayService.getTimetable(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
