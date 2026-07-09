package life.hanyang.admin.partnership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.partnership.dto.MerchantRequest;
import life.hanyang.core.partnership.dto.MerchantResponse;
import life.hanyang.core.partnership.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/admin/merchant")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 제휴 업체 관리 API")
@RestController
public class MerchantAdminController {
    private final MerchantService merchantService;

    @Operation(summary = "업체를 모두 불러옵니다")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> getAllMerchants(){
        List<MerchantResponse> data = merchantService.getAllMerchants();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "업체 정보를 추가합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createMerchants(@RequestBody List<MerchantRequest> requests){
        merchantService.createMerchants(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "업체 정보를 변경합니다.")
    @PutMapping()
    public ResponseEntity<ApiResponse<MerchantResponse>> updateMerchants(Long merchantId, @RequestBody MerchantRequest request){
        MerchantResponse merchantResponse = merchantService.updateMerchant(merchantId, request);
        return ResponseEntity.ok(ApiResponse.success(merchantResponse));
    }

    @Operation(summary = "등록된 업체 정보와 제휴 정보를 삭제합니다.")
    @DeleteMapping()
    public ResponseEntity<ApiResponse<Void>> deleteMerchants(
            @RequestParam("merchantIds") List<Long> merchantIds){
        merchantService.deleteMerchants(merchantIds);
        return ResponseEntity.ok(ApiResponse.success());
    }
}