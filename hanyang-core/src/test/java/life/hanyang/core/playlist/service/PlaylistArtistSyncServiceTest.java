package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.domain.PlaylistArtist;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.domain.PlaylistTrackArtist;
import life.hanyang.core.playlist.dto.PlaylistArtistBackfillRequest;
import life.hanyang.core.playlist.dto.SpotifyArtistMetadata;
import life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import life.hanyang.core.playlist.repository.PlaylistArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistArtistSyncServiceTest {
    private static final String TRACK = "0000000000000000000001";
    private static final String A = "000000000000000000000A";
    private static final String B = "000000000000000000000B";

    @Mock SpotifyApiClient client;
    @Mock PlaylistTrackRepository tracks;
    @Mock PlaylistArtistRepository artists;
    @Mock PlaylistTrackArtistRepository links;
    @Mock PlatformTransactionManager transactions;
    @Mock TransactionCacheEvictor cacheEvictor;
    private PlaylistArtistSyncService service;

    @BeforeEach
    void setUp() {
        service = new PlaylistArtistSyncService(client, tracks, artists, links, transactions, cacheEvictor);
        lenient().when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @Test
    void savesCompleteOrderedLinksInIndependentTransactionAndLeavesLegacyName() {
        PlaylistTrack track = PlaylistTrack.builder().trackId(TRACK).title("title").artist("legacy").build();
        PlaylistArtist artistA = mock(PlaylistArtist.class);
        PlaylistArtist artistB = mock(PlaylistArtist.class);
        when(client.getTrackArtists(TRACK)).thenReturn(List.of(
                new SpotifyArtistMetadata(B, "B", "photo"), new SpotifyArtistMetadata(A, "A", "")));
        when(tracks.findByIdForArtistSync(TRACK)).thenReturn(Optional.of(track));
        when(artists.findBySpotifyArtistId(A)).thenReturn(Optional.of(artistA));
        when(artists.findBySpotifyArtistId(B)).thenReturn(Optional.of(artistB));

        assertThat(service.syncMissingTrack(TRACK)).isTrue();

        var order = inOrder(client, transactions, tracks, artists);
        order.verify(client).getTrackArtists(TRACK);
        order.verify(transactions).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        order.verify(tracks).findByIdForArtistSync(TRACK);
        order.verify(artists).upsert(any(), eq(A), eq("A"), isNull());
        order.verify(artists).findBySpotifyArtistId(A);
        order.verify(artists).upsert(any(), eq(B), eq("B"), eq("photo"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlaylistTrackArtist>> saved = ArgumentCaptor.forClass(List.class);
        verify(links).saveAllAndFlush(saved.capture());
        assertThat(saved.getValue()).extracting(PlaylistTrackArtist::getArtist).containsExactly(artistB, artistA);
        assertThat(saved.getValue()).extracting(PlaylistTrackArtist::getArtistOrder).containsExactly(0, 1);
        assertThat(track.getArtist()).isEqualTo("legacy");
        verify(transactions).commit(any());
        verify(cacheEvictor).evictCacheAfterCommit("playlistChart");
    }

    @Test
    void linkedTrackSkipsSpotifyAndWritesOnRepeatedRun() {
        when(links.existsByTrackTrackId(TRACK)).thenReturn(true);
        assertThat(service.syncMissingTrack(TRACK)).isFalse();
        verifyNoInteractions(client, artists, transactions);
    }

    @Test
    void concurrentCompletionIsRecheckedAfterTrackLock() {
        when(links.existsByTrackTrackId(TRACK)).thenReturn(false, true);
        when(client.getTrackArtists(TRACK)).thenReturn(List.of(new SpotifyArtistMetadata(A, "A", null)));
        when(tracks.findByIdForArtistSync(TRACK)).thenReturn(Optional.of(mock(PlaylistTrack.class)));
        assertThat(service.syncMissingTrack(TRACK)).isFalse();
        verifyNoInteractions(artists);
        verify(links, never()).saveAllAndFlush(any());
    }

    @Test
    void emptyOrDuplicateMetadataNeverStartsWriteTransaction() {
        when(client.getTrackArtists(TRACK)).thenReturn(List.of(), List.of(
                new SpotifyArtistMetadata(A, "A", null), new SpotifyArtistMetadata(A, "A", null)));
        assertThatThrownBy(() -> service.syncMissingTrack(TRACK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.syncMissingTrack(TRACK)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(artists, transactions);
    }

    @Test
    void linkFailureRollsBackEntireArtistWrite() {
        when(client.getTrackArtists(TRACK)).thenReturn(List.of(new SpotifyArtistMetadata(A, "A", null)));
        when(tracks.findByIdForArtistSync(TRACK)).thenReturn(Optional.of(mock(PlaylistTrack.class)));
        when(artists.findBySpotifyArtistId(A)).thenReturn(Optional.of(mock(PlaylistArtist.class)));
        when(links.saveAllAndFlush(any())).thenThrow(new IllegalStateException("write failed"));
        assertThatThrownBy(() -> service.syncMissingTrack(TRACK)).isInstanceOf(IllegalStateException.class);
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
        verifyNoInteractions(cacheEvictor);
    }

    @Test
    void postCommitSpotifyFailureDoesNotEscapeToRegistration() {
        when(client.getTrackArtists(TRACK)).thenThrow(new SpotifyServiceUnavailableException());
        assertThatCode(() -> service.onTrackRegistered(new PlaylistTrackRegisteredEvent(TRACK))).doesNotThrowAnyException();
        verifyNoInteractions(artists, transactions);
    }

    @Test
    void backfillReturnsFailuresAndAdvancesCursorRatherThanRetryingFirstFailureForever() {
        when(tracks.findTrackIdsWithoutArtists(eq(""), any())).thenReturn(List.of("missing-1", "missing-2"));
        when(client.getTrackArtists(anyString())).thenThrow(new SpotifyServiceUnavailableException());
        var response = service.backfill(new PlaylistArtistBackfillRequest(null, 3));
        assertThat(response.attempted()).isEqualTo(2);
        assertThat(response.failedTrackIds()).containsExactly("missing-1", "missing-2");
        assertThat(response.nextAfterTrackId()).isEqualTo("missing-2");
        assertThat(response.scanComplete()).isTrue();
        assertThat(response.linked()).isZero();
    }

    @Test
    void backfillStopsImmediatelyAtSpotifyRateLimit() {
        when(tracks.findTrackIdsWithoutArtists(eq("cursor"), any())).thenReturn(List.of(TRACK, "next"));
        when(client.getTrackArtists(TRACK)).thenThrow(new SpotifyRateLimitException(17, null));
        var response = service.backfill(new PlaylistArtistBackfillRequest("cursor", 2));
        assertThat(response.attempted()).isEqualTo(1);
        assertThat(response.nextAfterTrackId()).isEqualTo(TRACK);
        assertThat(response.retryAfterSeconds()).isEqualTo(17);
        assertThat(response.scanComplete()).isFalse();
        verify(client, never()).getTrackArtists("next");
    }

    @Test
    void invalidBatchSizeIsRejectedBeforeAnyReadOrWrite() {
        assertThatThrownBy(() -> service.backfill(new PlaylistArtistBackfillRequest(null, 21)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.backfill(new PlaylistArtistBackfillRequest(null, 0)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(tracks, client, artists, links);
    }
}
