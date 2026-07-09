package life.hanyang.admin.banner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.banner.dto.BannerRequest;
import life.hanyang.core.banner.dto.BannerResponse;
import life.hanyang.core.banner.dto.BannersUpdateDto;
import life.hanyang.core.banner.service.BannerService;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.gym.dto.GymScheduleCellDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 배너 관리 API")
public class BannerAdminController {

    private final BannerService bannerService;

    @Operation(summary = "새로운 배너 이미지를 업로드하고 정보를 추가합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") BannerRequest request
    ) {
        BannerResponse response = bannerService.createBanner(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "여러 배너의 정보를 한꺼번에 변경합니다. (순서 일괄 변경 포함)")
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateBanners(
            @RequestBody List<BannersUpdateDto> requests
    ) {
        bannerService.updateBanners(requests);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "여러 배너를 한꺼번에 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteBanners(
            @RequestParam List<Long> bannerIds
    ) {
        bannerService.deleteBanner(bannerIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "DB에 저장된 모든 배너를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        List<BannerResponse> data = bannerService.getAllBanners();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}