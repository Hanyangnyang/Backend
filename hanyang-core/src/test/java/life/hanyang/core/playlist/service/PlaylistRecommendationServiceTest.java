package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import life.hanyang.core.playlist.repository.*;
import life.hanyang.core.playlist.repository.PlaylistRecommendationRepository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static life.hanyang.core.playlist.dto.PlaylistRecommendationResponse.Source.*;

class PlaylistRecommendationServiceTest {
    private final PlaylistRecommendationRepository repository = mock(PlaylistRecommendationRepository.class);
    private final PlaylistTrackRepository tracks = mock(PlaylistTrackRepository.class);
    private final PlaylistTrackArtistRepository links = mock(PlaylistTrackArtistRepository.class);
    private final PlaylistChartRepository charts = mock(PlaylistChartRepository.class);
    private final PlaylistRecommendationService service = new PlaylistRecommendationService(repository, tracks, links, charts);
    private final UUID device = UUID.randomUUID();
    private final Map<String, PlaylistTrack> catalog = new HashMap<>();
    private final List<PlaylistTrackArtist> allLinks = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        when(repository.findSignals(eq(device), any(), any(), any(), any())).thenReturn(List.of());
        when(repository.findArtists(anyList())).thenReturn(List.of());
        when(repository.findGenres(anyList())).thenReturn(List.of());
        when(repository.findInterestCandidates(anyList())).thenReturn(List.of());
        when(repository.findDiscoveryCandidates(eq(device), anyList(), anySet())).thenReturn(List.of());
        when(charts.findLatestOverallChartByChartType(ChartType.WEEKLY)).thenReturn(List.of());
        when(tracks.findAllById(any())).thenAnswer(call -> {
            List<PlaylistTrack> result = new ArrayList<>();
            for (String id : (Iterable<String>) call.getArgument(0)) if (catalog.containsKey(id)) result.add(catalog.get(id));
            return result;
        });
        when(links.findWithArtistsByTrackIds(anyList())).thenAnswer(call -> {
            List<String> ids = call.getArgument(0);
            return allLinks.stream().filter(link -> ids.contains(link.getTrack().getTrackId()))
                    .sorted(Comparator.comparing((PlaylistTrackArtist link) -> link.getTrack().getTrackId())
                            .thenComparingInt(PlaylistTrackArtist::getArtistOrder)).toList();
        });
    }

    @Test
    void splitsCollaborationScoresAndAvoidsDuplicateArtistsAndTracks() {
        UUID a = id(1), b = id(2), c = id(3);
        track("shared", a, b); track("b-alternative", b); track("solo", c);
        when(repository.findSignals(eq(device), any(), any(), any(), any()))
                .thenReturn(List.of(new Signal("shared", 0, 6), new Signal("solo", 0, 4)));
        when(repository.findArtists(anyList())).thenReturn(List.of(
                new TrackArtist("shared", a), new TrackArtist("shared", b), new TrackArtist("solo", c)));
        when(repository.findGenres(anyList())).thenReturn(List.of(new TrackGenre("shared", "BAND")));
        when(repository.findInterestCandidates(anyList())).thenReturn(List.of(
                new Candidate(a, "shared"), new Candidate(b, "shared"), new Candidate(b, "b-alternative"), new Candidate(c, "solo")));
        var result = service.getRecommendations(device);
        assertThat(result.items()).extracting(item -> item.artist().id()).containsExactly(c, a, b);
        assertThat(result.items()).extracting(item -> item.track().trackId()).containsExactly("solo", "shared", "b-alternative");
        assertThat(result.items().get(1).track().artists()).extracting(artist -> artist.id()).containsExactly(a, b);
        assertThat(result.items()).extracting(PlaylistRecommendationResponse.Item::source).containsOnly(INTEREST);
        verify(repository).findDiscoveryCandidates(device, List.of("BAND"), Set.of(a, b, c));
    }

    @Test
    void givesEveryCoartistFullPlayScoreButStillSplitsLikesAndPosts() {
        UUID a = id(1), b = id(2), c = id(3), d = id(4);
        track("shared", a, b); track("b-alternative", b); track("mid", c); track("low", d);
        when(repository.findSignals(eq(device), any(), any(), any(), any())).thenReturn(List.of(
                new Signal("shared", 4, 6), new Signal("mid", 0, 8), new Signal("low", 0, 6)));
        when(repository.findArtists(anyList())).thenReturn(List.of(new TrackArtist("shared", a),
                new TrackArtist("shared", b), new TrackArtist("mid", c), new TrackArtist("low", d)));
        when(repository.findInterestCandidates(anyList())).thenReturn(List.of(new Candidate(a, "shared"),
                new Candidate(b, "shared"), new Candidate(b, "b-alternative"),
                new Candidate(c, "mid"), new Candidate(d, "low")));
        // C=8, A=B=4+6/2=7, D=6. Splitting plays or duplicating preference scores changes this order.
        assertThat(service.getRecommendations(device).items()).extracting(item -> item.artist().id())
                .containsExactly(c, a, b, d);
    }

    @Test
    void selectsTwoInterestsAndThreeDiscoveriesWithoutRankingByPostCount() {
        List<Candidate> interest = new ArrayList<>(), discovery = new ArrayList<>();
        List<Signal> signals = new ArrayList<>(); List<TrackArtist> historyArtists = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            track("t" + i, id(i));
            if (i <= 3) {
                interest.add(new Candidate(id(i), "t" + i)); signals.add(new Signal("t" + i, 0, 8 - i));
                historyArtists.add(new TrackArtist("t" + i, id(i)));
            } else discovery.add(new Candidate(id(i), "t" + i));
        }
        when(repository.findSignals(eq(device), any(), any(), any(), any())).thenReturn(signals);
        when(repository.findArtists(anyList())).thenReturn(historyArtists);
        when(repository.findInterestCandidates(anyList())).thenReturn(interest);
        when(repository.findDiscoveryCandidates(eq(device), anyList(), anySet())).thenReturn(discovery);
        var result = service.getRecommendations(device);
        assertThat(result.items()).hasSize(5);
        assertThat(result.items().stream().filter(item -> item.source() == INTEREST)).hasSize(2);
        assertThat(result.items().stream().filter(item -> item.source() == DISCOVERY)).hasSize(3);
        assertThat(result.items()).extracting(item -> item.artist().id()).doesNotHaveDuplicates();
        verifyNoInteractions(charts);
    }

    @Test
    void fillsMissingSlotsFromWeeklyInRankOrderAndSkipsRepeatedCollaborationTracks() {
        var shared = track("weekly-shared", id(1), id(2));
        var repeatedArtist = track("weekly-repeat", id(1));
        var alternative = track("weekly-alternative", id(2));
        var third = track("weekly-third", id(3));
        when(charts.findLatestOverallChartByChartType(ChartType.WEEKLY)).thenReturn(List.of(
                chart(1, shared), chart(2, repeatedArtist), chart(3, alternative), chart(4, third)));
        var result = service.getRecommendations(device);
        assertThat(result.items()).hasSize(3);
        assertThat(result.items()).extracting(item -> item.track().trackId())
                .containsExactly("weekly-shared", "weekly-alternative", "weekly-third");
        assertThat(result.items()).extracting(item -> item.artist().id()).containsExactly(id(1), id(2), id(3));
        assertThat(result.items()).extracting(PlaylistRecommendationResponse.Item::source).containsOnly(WEEKLY_CHART);
    }

    @Test
    void scarcePersonalCandidatesAreFilledByWeeklyWithoutDuplicates() {
        var familiar = track("familiar", id(1));
        var discovery = track("discovery", id(2));
        var weeklyThree = track("weekly-3", id(3));
        var weeklyFour = track("weekly-4", id(4));
        var weeklyFive = track("weekly-5", id(5));
        when(repository.findSignals(eq(device), any(), any(), any(), any())).thenReturn(List.of(new Signal("familiar", 0, 6)));
        when(repository.findArtists(anyList())).thenReturn(List.of(new TrackArtist("familiar", id(1))));
        when(repository.findInterestCandidates(anyList())).thenReturn(List.of(new Candidate(id(1), "familiar")));
        when(repository.findDiscoveryCandidates(eq(device), anyList(), anySet())).thenReturn(List.of(new Candidate(id(2), "discovery")));
        when(charts.findLatestOverallChartByChartType(ChartType.WEEKLY)).thenReturn(List.of(
                chart(1, familiar), chart(2, discovery), chart(3, weeklyThree), chart(4, weeklyFour), chart(5, weeklyFive)));
        var result = service.getRecommendations(device);
        assertThat(result.items()).extracting(PlaylistRecommendationResponse.Item::source)
                .containsExactly(INTEREST, DISCOVERY, WEEKLY_CHART, WEEKLY_CHART, WEEKLY_CHART);
        assertThat(result.items()).extracting(item -> item.track().trackId())
                .containsExactly("familiar", "discovery", "weekly-3", "weekly-4", "weekly-5");
    }

    @Test
    void oneInterestAllowsFourDiscoveryCards() {
        track("familiar", id(1));
        when(repository.findSignals(eq(device), any(), any(), any(), any())).thenReturn(List.of(new Signal("familiar", 0, 6)));
        when(repository.findArtists(anyList())).thenReturn(List.of(new TrackArtist("familiar", id(1))));
        when(repository.findInterestCandidates(anyList())).thenReturn(List.of(new Candidate(id(1), "familiar")));
        List<Candidate> discovery = new ArrayList<>();
        for (int i = 2; i <= 5; i++) { track("new-" + i, id(i)); discovery.add(new Candidate(id(i), "new-" + i)); }
        when(repository.findDiscoveryCandidates(eq(device), anyList(), anySet())).thenReturn(discovery);
        var result = service.getRecommendations(device);
        assertThat(result.items()).hasSize(5);
        assertThat(result.items().stream().filter(item -> item.source() == INTEREST)).hasSize(1);
        assertThat(result.items().stream().filter(item -> item.source() == DISCOVERY)).hasSize(4);
        verifyNoInteractions(charts);
    }

    @Test
    void emptySystemReturnsEmptyAndNullDeviceIsRejected() {
        assertThat(service.getRecommendations(device).items()).isEmpty();
        verifyNoInteractions(tracks, links);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getRecommendations(null))
                .isInstanceOf(life.hanyang.core.global.exception.BusinessException.class);
    }

    private UUID id(int value) { return new UUID(0, value); }

    private PlaylistTrack track(String trackId, UUID... artistIds) {
        var track = PlaylistTrack.builder().trackId(trackId).title(trackId).artist("legacy").build();
        for (int i = 0; i < artistIds.length; i++) {
            PlaylistArtist artist = org.springframework.beans.BeanUtils.instantiateClass(PlaylistArtist.class);
            ReflectionTestUtils.setField(artist, "id", artistIds[i]);
            ReflectionTestUtils.setField(artist, "name", "artist " + artistIds[i]);
            ReflectionTestUtils.setField(artist, "spotifyArtistId", "spotify-" + artistIds[i]);
            var link = new PlaylistTrackArtist(track, artist, i);
            track.getArtistLinks().add(link); allLinks.add(link);
        }
        catalog.put(trackId, track);
        return track;
    }

    private PlaylistChart chart(int rank, PlaylistTrack track) {
        return PlaylistChart.builder().chartType(ChartType.WEEKLY).rank(rank).track(track).build();
    }
}
