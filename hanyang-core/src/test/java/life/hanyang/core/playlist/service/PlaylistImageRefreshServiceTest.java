package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistImageRefreshServiceTest {
    @Mock SpotifyApiClient spotify;
    @Mock PlaylistTrackRepository tracks;
    @Mock PlaylistArtistRepository artists;
    @Mock TransactionCacheEvictor caches;
    @Mock PlatformTransactionManager manager;
    PlaylistImageRefreshService service;

    @BeforeEach void setup() {
        service = new PlaylistImageRefreshService(spotify, tracks, artists, caches, manager);
    }
    private SpotifyTrackSearchResponse track(String id, String url) {
        return new SpotifyTrackSearchResponse(id, "title", "artist", url, 1, List.of());
    }

    @Test void updatesAlbumWithLookaheadCursorAndEvictsCachesOnce() {
        when(tracks.findImageRefreshIds(eq(""), any())).thenReturn(List.of("a", "b", "c"));
        when(spotify.getTrack("a")).thenReturn(track("a", "300px"));
        when(spotify.getTrack("b")).thenReturn(track("b", null));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(tracks.updateImageUrl(eq("a"), eq("300px"), any())).thenReturn(1);
        var result = service.refresh(new PlaylistImageRefreshRequest(PlaylistImageRefreshRequest.Target.TRACK, 2, null, null));
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.nextAfterId()).isEqualTo("b");
        assertThat(result.scanComplete()).isFalse();
        var order = inOrder(spotify, manager, tracks, caches);
        order.verify(spotify).getTrack("a");
        order.verify(manager).getTransaction(any());
        order.verify(tracks).updateImageUrl(eq("a"), eq("300px"), any());
        order.verify(manager).commit(any());
        order.verify(caches).evictCacheAfterCommit("playlistChart");
        verify(caches).evictCacheAfterCommit("playlistRecommendations");
        verify(caches).evictCacheAfterCommit("spotifyTrackSearch");
        verify(spotify, never()).getTrack("c");
        verify(tracks, never()).updateImageUrl(eq("b"), any(), any());
    }

    @Test void rateLimitKeepsCursorAndReportsUnprocessedIds() {
        when(tracks.findImageRefreshIds(eq("start"), any())).thenReturn(List.of("a", "b"));
        when(spotify.getTrack("a")).thenThrow(new SpotifyRateLimitException(30, null));
        var result = service.refresh(new PlaylistImageRefreshRequest(PlaylistImageRefreshRequest.Target.TRACK, 2, "start", null));
        assertThat(result.nextAfterId()).isEqualTo("start");
        assertThat(result.retryAfterSeconds()).isEqualTo(30L);
        assertThat(result.failedIds()).containsExactly("a", "b");
        assertThat(result.scanComplete()).isFalse();
        verifyNoInteractions(manager, caches);
        verify(spotify, never()).getTrack("b");
    }

    @Test void retriesArtistIdsWithoutScanningOrCreatingArtists() {
        when(spotify.getArtist("artist-id")).thenReturn(new SpotifyArtistMetadata("artist-id", "name", "320px"));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(artists.updateImageUrl(eq("artist-id"), eq("320px"), any())).thenReturn(1);
        var result = service.refresh(new PlaylistImageRefreshRequest(PlaylistImageRefreshRequest.Target.ARTIST, 5, null, List.of("artist-id")));
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.nextAfterId()).isNull();
        assertThat(result.scanComplete()).isTrue();
        verify(artists, never()).findImageRefreshIds(any(), any());
        verifyNoInteractions(tracks);
    }

    @Test void storageFailureRollsBackAndContinuesWithoutErasingExistingImages() {
        when(tracks.findImageRefreshIds(eq(""), any())).thenReturn(List.of("a", "b"));
        when(spotify.getTrack("a")).thenReturn(track("a", "300px"));
        when(spotify.getTrack("b")).thenReturn(track("b", ""));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(tracks.updateImageUrl(eq("a"), eq("300px"), any())).thenThrow(new IllegalStateException());
        var result = service.refresh(new PlaylistImageRefreshRequest(PlaylistImageRefreshRequest.Target.TRACK, 5, null, null));
        assertThat(result.failedIds()).containsExactly("a");
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.scanComplete()).isTrue();
        verify(manager).rollback(any());
        verifyNoInteractions(caches);
    }

    @Test void rejectsCursorCombinedWithRetryIds() {
        assertThatThrownBy(() -> service.refresh(new PlaylistImageRefreshRequest(
                PlaylistImageRefreshRequest.Target.ARTIST, 5, "cursor", List.of("id"))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(spotify, tracks, artists, manager, caches);
    }
}
