package life.hanyang.user.gym.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.gym.dto.GymPeriodResponse;
import life.hanyang.core.gym.service.GymPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/v1/gym")
@RestController
@RequiredArgsConstructor
@Tag(name = "체대 헬스장 시간표 API", description = "체대 헬스장 시간표 정보를 학기 별로 나눠 수업 시간을 저장합니다.")
public class GymController {
    private final GymPeriodService gymPeriodService;

    @GetMapping("/gym-periods")
    @Operation(summary =  "학기 정보를 조회합니다", description = "체대 헬스장 시간표가 등록될 학기 정보를 리스트 형태로 조회합니다.")
    public ResponseEntity<ApiResponse<List<GymPeriodResponse>>> getAllGymPeriods() {
        List<GymPeriodResponse> data = gymPeriodService.getGymPeriods();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}