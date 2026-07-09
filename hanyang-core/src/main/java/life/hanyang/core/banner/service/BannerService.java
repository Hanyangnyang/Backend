package life.hanyang.core.banner.service;

import life.hanyang.core.banner.domain.Banner;
import life.hanyang.core.banner.dto.BannerRequest;
import life.hanyang.core.banner.dto.BannerResponse;
import life.hanyang.core.banner.dto.BannerUserResponse;
import life.hanyang.core.banner.dto.BannersUpdateDto;
import life.hanyang.core.banner.repository.BannerRepository;
import life.hanyang.core.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {
    private final BannerRepository bannerRepository;
    private final StorageService storageService;

    @Transactional
    public BannerResponse createBanner(MultipartFile file, BannerRequest request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("배너 이미지 파일은 필수 업로드 항목입니다.");
        }

        String imageUrl = storageService.uploadFile(file, "banners");

        Banner banner = Banner.builder()
                .imageUrl(imageUrl)
                .altText(request.altText())
                .clickUrl(request.clickUrl())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        reorderDisplaySequence();
        return new BannerResponse(savedBanner);
    }

    @Transactional
    public void updateBanners(List<BannersUpdateDto> requests) {
        if (requests.isEmpty()) return;

        List<Long> bannerIds = requests.stream().map(BannersUpdateDto::id).toList();
        List<Banner> bannersToUpdate = bannerRepository.findAllById(bannerIds);
        if (bannersToUpdate.size() != requests.size()) {
            throw new IllegalArgumentException("존재하지 않는 배너 ID가 포함되어 있습니다.");
        }

        Map<Long, Banner> bannerMap = bannersToUpdate.stream()
                .collect(Collectors.toMap(Banner::getId, b -> b));

        for (BannersUpdateDto req : requests) {
            Banner banner = bannerMap.get(req.id());
            banner.update(
                    req.altText(),
                    req.clickUrl(),
                    req.displayOrder(),
                    req.isActive()
            );
        }
        reorderDisplaySequence();
    }

    @Transactional
    public void deleteBanner(List<Long> bannerIds) {
        bannerRepository.deleteAllByIdInBatch(bannerIds);
        reorderDisplaySequence();
    }

    private void reorderDisplaySequence() {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        for (int i = 0; i < banners.size(); i++) {
            banners.get(i).changeDisplayOrder(i);
        }
    }

    @Transactional
    public List<BannerResponse> getAllBanners(){
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        return banners.stream()
                .map(BannerResponse::new)
                .toList();
    }

    @Transactional
    public List<BannerUserResponse> getActiveBanners(){
        List<Banner> banners = bannerRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        return banners.stream()
                .map(BannerUserResponse::new)
                .toList();
    }
}
