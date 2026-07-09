package life.hanyang.user.banner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.banner.dto.BannerUserResponse;
import life.hanyang.core.banner.service.BannerService;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Tag(name = "배너 API", description = "배너와 관련된 기능을 제공합니다.")
public class BannerController {
    private final BannerService bannerService;

    @Operation(summary = "활성화된(유효한) 배너를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerUserResponse>>> getAllBanners() {
        List<BannerUserResponse> data = bannerService.getActiveBanners();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
