package life.hanyang.user.partnership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.partnership.dto.PartnershipDetailResponse;
import life.hanyang.core.partnership.service.PartnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/v1/partnership")
@RequiredArgsConstructor
@Tag(name = "제휴 API", description = "제휴 정보를 조회합니다.")
@RestController
public class PartnershipController {
    private final PartnershipService partnershipService;

    @Operation(summary = "이용가능한 제휴 정보를 모두 조회합니다.")
    @GetMapping("partnership-available")
    public ResponseEntity<ApiResponse<List<PartnershipDetailResponse>>> getAvailablePartnerships(){
        List<PartnershipDetailResponse> data = partnershipService.getAvailablePartnerships();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}