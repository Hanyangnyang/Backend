package life.hanyang.admin.partnership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.partnership.dto.MerchantCreateWithPartnershipsRequest;
import life.hanyang.core.partnership.dto.PartnershipDetailDto;
import life.hanyang.core.partnership.dto.PartnershipUpdateDto;
import life.hanyang.core.partnership.service.PartnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/admin/partnership")
@RestController
@Tag(name = "(관리자용) 제휴 정보 관리 API", description = "제휴 정보를 세부적으로 관리합니다.")
@RequiredArgsConstructor
public class PartnershipAdminController {
    private final PartnershipService partnershipService;

    @Operation(summary = "기존의 제휴 정보를 전부 삭제 하고, 새롭게 데이터를 추가합니다.")
    @PostMapping("reset-reload")
    public ResponseEntity<String> resetAndLoadData(@RequestBody List<MerchantCreateWithPartnershipsRequest> requests) {
        partnershipService.resetAndLoadPartnerships(requests);
        return ResponseEntity.ok("DB 초기화 및 신규 데이터 적재가 완료되었습니다. 개수: " + requests.size());
    }

    @Operation(summary = "제휴 정보를 추가합니다.")
    @PostMapping("/merchant/{merchantId}/partnership")
    public ResponseEntity<ApiResponse<Void>> addPartnership(
            @PathVariable Long merchantId,
            @RequestBody PartnershipDetailDto request
    ) {
        partnershipService.addPartnership(merchantId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "제휴 정보를 제거합니다.")
    @DeleteMapping("/{partnershipId}")
    public ResponseEntity<ApiResponse<Void>> deletePartnership(
            @PathVariable Long partnershipId
    ) {
        partnershipService.deletePartnership(partnershipId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "제휴 정보를 수정합니다.")
    @PatchMapping("/{partnershipId}")
    public ResponseEntity<ApiResponse<Void>> updatePartnership(
            @PathVariable Long partnershipId,
            @RequestBody PartnershipUpdateDto request
            ) {
        partnershipService.updatePartnership(partnershipId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
