package life.hanyang.admin.partnership.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.partnership.dto.MerchantCreateWithPartnershipsRequest;
import life.hanyang.core.partnership.dto.PartnershipDetailDto;
import life.hanyang.core.partnership.dto.PartnershipUpdateDto;
import life.hanyang.core.partnership.service.PartnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/v1/admin/partnership")
@RestController
@Tag(name = "(관리자용) 제휴 정보 관리 API", description = "제휴 정보를 세부적으로 관리합니다.")
@RequiredArgsConstructor
public class PartnershipAdminController {
    private final PartnershipService partnershipService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "전체 업체·제휴 정보를 JSON 파일로 내보냅니다.",
            description = "비활성·만료 데이터와 대표 메뉴도 포함합니다. reset-reload에 다시 입력 가능한 snake_case JSON 배열입니다. 성공 응답은 ApiResponse로 감싸지 않습니다. ID와 수정시각은 포함하지 않습니다.")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportData() throws JsonProcessingException {
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(partnershipService.exportMerchants());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("merchants-partnerships.json").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(json);
    }

    @Operation(summary = "기존의 제휴 정보를 전부 삭제 하고, 입력한 JSON 파일에 있는 정보를 추가합니다.")
    @PostMapping(value = "reset-reload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> resetAndLoadData(@RequestParam("file") MultipartFile file) {
        List<MerchantCreateWithPartnershipsRequest> requests;
        try {
            requests = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<MerchantCreateWithPartnershipsRequest>>() {}
            );
        } catch (IOException e) {
            throw new BusinessException("JSON 파일 파싱 중 에러가 발생했습니다: " + e.getMessage(), ErrorCode.INVALID_INPUT_VALUE);
        }

        partnershipService.resetAndLoadPartnerships(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
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
