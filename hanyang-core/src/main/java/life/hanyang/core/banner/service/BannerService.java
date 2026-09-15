package life.hanyang.core.banner.service;

import life.hanyang.core.banner.domain.Banner;
import life.hanyang.core.banner.domain.BannerPlacement;
import life.hanyang.core.banner.dto.BannerRequest;
import life.hanyang.core.banner.dto.BannerResponse;
import life.hanyang.core.banner.dto.BannerUserResponse;
import life.hanyang.core.banner.dto.BannersUpdateDto;
import life.hanyang.core.banner.repository.BannerRepository;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.storage.StorageService;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {
    private final BannerRepository bannerRepository;
    private final StorageService storageService;
    private final TransactionCacheEvictor transactionCacheEvictor;

    @Transactional
    public BannerResponse createBanner(MultipartFile file, BannerRequest request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("배너 이미지 파일은 필수 업로드 항목입니다.");
        }

        String imageUrl = storageService.uploadFile(file, "banners");

        Banner banner = Banner.builder()
                .imageUrl(imageUrl)
                .placement(request.placement())
                .altText(request.altText())
                .clickUrl(request.clickUrl())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        reorderDisplaySequence();
        transactionCacheEvictor.evictCacheAfterCommit("banner");
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
                    req.placement(),
                    req.altText(),
                    req.clickUrl(),
                    req.displayOrder(),
                    req.isActive()
            );
        }
        reorderDisplaySequence();
        transactionCacheEvictor.evictCacheAfterCommit("banner");
    }

    @Transactional
    public BannerResponse updateBannerImage(Long bannerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("변경할 배너 이미지 파일은 필수입니다.");
        }

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new EntityNotFoundException("해당 배너가 존재하지 않습니다. id: " + bannerId));

        String previousImageUrl = banner.getImageUrl();
        String newImageUrl = storageService.uploadFile(file, "banners");
        banner.changeImageUrl(newImageUrl);

        scheduleReplacedImageCleanup(previousImageUrl, newImageUrl);
        transactionCacheEvictor.evictCacheAfterCommit("banner");
        return new BannerResponse(banner);
    }

    @Transactional
    public void deleteBanner(List<Long> bannerIds) {
        bannerRepository.deleteAllByIdInBatch(bannerIds);
        reorderDisplaySequence();
        transactionCacheEvictor.evictCacheAfterCommit("banner");
    }

    private void reorderDisplaySequence() {
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        for (int i = 0; i < banners.size(); i++) {
            banners.get(i).changeDisplayOrder(i);
        }
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners(){
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        return banners.stream()
                .map(BannerResponse::new)
                .toList();
    }

    @Cacheable(
            cacheNames = "banner",
            key = "#placement == null ? 'active:ALL' : 'active:' + #placement.name()"
    )
    public List<BannerUserResponse> getActiveBanners(BannerPlacement placement){
        if (placement == null) {
            return bannerRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().stream()
                    .map(BannerUserResponse::new)
                    .toList();
        }

        List<Banner> banners = switch (placement) {
            case SPLASH -> bannerRepository.findAllByIsActiveTrueAndPlacementInOrderByDisplayOrderAsc(
                    List.of(BannerPlacement.SPLASH, BannerPlacement.BOTH)
            );
            case BANNER -> bannerRepository.findAllByIsActiveTrueAndPlacementInOrderByDisplayOrderAsc(
                    List.of(BannerPlacement.BANNER, BannerPlacement.BOTH)
            );
            case BOTH -> bannerRepository.findAllByIsActiveTrueAndPlacementInOrderByDisplayOrderAsc(
                    List.of(BannerPlacement.BOTH)
            );
        };
        return banners.stream()
                .map(BannerUserResponse::new)
                .toList();
    }

    private void scheduleReplacedImageCleanup(String previousImageUrl, String newImageUrl) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFileSafely(previousImageUrl);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        deleteFileSafely(newImageUrl);
                    }
                }
            });
            return;
        }

        deleteFileSafely(previousImageUrl);
    }

    private void deleteFileSafely(String imageUrl) {
        try {
            storageService.deleteFile(imageUrl, "banners");
        } catch (Exception e) {
            log.warn("배너 스토리지 파일 삭제 실패: url={}, reason={}", imageUrl, e.getMessage());
        }
    }
}
