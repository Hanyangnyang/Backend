package life.hanyang.core.banner.service;

import life.hanyang.core.banner.domain.Banner;
import life.hanyang.core.banner.domain.BannerPlacement;
import life.hanyang.core.banner.dto.BannerUserResponse;
import life.hanyang.core.banner.repository.BannerRepository;
import life.hanyang.core.global.storage.StorageService;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private TransactionCacheEvictor transactionCacheEvictor;

    @InjectMocks
    private BannerService bannerService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void splashPlacementIncludesSplashAndBothBanners() {
        Banner splash = banner(BannerPlacement.SPLASH, "splash.jpg");
        Banner both = banner(BannerPlacement.BOTH, "both.jpg");
        given(bannerRepository.findAllByIsActiveTrueAndPlacementInOrderByDisplayOrderAsc(
                List.of(BannerPlacement.SPLASH, BannerPlacement.BOTH)
        )).willReturn(List.of(splash, both));

        List<BannerUserResponse> responses = bannerService.getActiveBanners(BannerPlacement.SPLASH);

        assertThat(responses).extracting(BannerUserResponse::placement)
                .containsExactly(BannerPlacement.SPLASH, BannerPlacement.BOTH);
    }

    @Test
    void replacingImageDeletesPreviousFileAfterCommit() {
        Banner banner = banner(BannerPlacement.BANNER, "old.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[]{1});
        given(bannerRepository.findById(1L)).willReturn(Optional.of(banner));
        given(storageService.uploadFile(file, "banners")).willReturn("https://storage.test/banners/new.jpg");
        startTransactionSynchronization();

        bannerService.updateBannerImage(1L, file);

        verify(storageService, never()).deleteFile("https://storage.test/banners/old.jpg", "banners");
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
        );

        assertThat(banner.getImageUrl()).isEqualTo("https://storage.test/banners/new.jpg");
        verify(storageService).deleteFile("https://storage.test/banners/old.jpg", "banners");
        verify(storageService, never()).deleteFile("https://storage.test/banners/new.jpg", "banners");
    }

    @Test
    void replacingImageDeletesNewFileWhenTransactionRollsBack() {
        Banner banner = banner(BannerPlacement.BANNER, "old.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[]{1});
        given(bannerRepository.findById(1L)).willReturn(Optional.of(banner));
        given(storageService.uploadFile(file, "banners")).willReturn("https://storage.test/banners/new.jpg");
        startTransactionSynchronization();

        bannerService.updateBannerImage(1L, file);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        );

        verify(storageService).deleteFile("https://storage.test/banners/new.jpg", "banners");
        verify(storageService, never()).deleteFile("https://storage.test/banners/old.jpg", "banners");
    }

    private void startTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private Banner banner(BannerPlacement placement, String fileName) {
        return Banner.builder()
                .imageUrl("https://storage.test/banners/" + fileName)
                .placement(placement)
                .displayOrder(0)
                .isActive(true)
                .build();
    }
}
