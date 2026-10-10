package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import life.hanyang.core.playlist.repository.PlaylistChartRepository;
import life.hanyang.core.playlist.repository.PlaylistRecommendationRepository;
import life.hanyang.core.playlist.repository.PlaylistRecommendationRepository.Candidate;
import life.hanyang.core.playlist.repository.PlaylistTrackArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static life.hanyang.core.playlist.dto.PlaylistRecommendationResponse.Source.*;

@Service
@RequiredArgsConstructor
public class PlaylistRecommendationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_ITEMS = 5;
    private final PlaylistRecommendationRepository recommendationRepository;
    private final PlaylistTrackRepository trackRepository;
    private final PlaylistTrackArtistRepository linkRepository;
    private final PlaylistChartRepository chartRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "playlistRecommendations", key = "{'v1', #deviceId}", unless = "#result.items().isEmpty()")
    public PlaylistRecommendationResponse getRecommendations(UUID deviceId) {
        if (deviceId == null) {
            throw new BusinessException("기기 식별자 ID는 필수입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDate today = LocalDate.now(KST);
        LocalDate since = today.minusDays(29);
        LocalDate boostedSince = today.minusDays(6);
        var signals = recommendationRepository.findSignals(deviceId, since.atStartOfDay(KST).toInstant(),
                boostedSince.atStartOfDay(KST).toInstant(), since, boostedSince);
        Map<String, PlaylistRecommendationRepository.Signal> signalsByTrack = signals.stream()
                .collect(Collectors.toMap(PlaylistRecommendationRepository.Signal::trackId, signal -> signal));
        Map<String, Double> trackScores = signals.stream().collect(Collectors.toMap(
                PlaylistRecommendationRepository.Signal::trackId, PlaylistRecommendationRepository.Signal::score));
        List<String> historyTracks = new ArrayList<>(trackScores.keySet());
        var artistLinks = recommendationRepository.findArtists(historyTracks);
        Map<String, List<UUID>> artistsByTrack = artistLinks.stream().collect(Collectors.groupingBy(
                PlaylistRecommendationRepository.TrackArtist::trackId,
                Collectors.mapping(PlaylistRecommendationRepository.TrackArtist::artistId, Collectors.toList())));
        Map<UUID, Double> artistScores = new HashMap<>();
        artistsByTrack.forEach((trackId, artists) -> {
            var signal = signalsByTrack.get(trackId);
            // Every participating artist receives the full play score; explicit preferences are shared.
            double share = signal.playScore() + signal.preferenceScore() / artists.size();
            artists.forEach(artist -> artistScores.merge(artist, share, Double::sum));
        });
        List<UUID> interestArtists = artistScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(20).map(Map.Entry::getKey).toList();

        var genresByTrack = recommendationRepository.findGenres(historyTracks).stream().collect(Collectors.groupingBy(
                PlaylistRecommendationRepository.TrackGenre::trackId,
                Collectors.mapping(PlaylistRecommendationRepository.TrackGenre::genre, Collectors.toList())));
        Map<String, Double> genreScores = new HashMap<>();
        genresByTrack.forEach((trackId, genres) -> {
            double share = trackScores.get(trackId) / genres.size();
            genres.forEach(genre -> genreScores.merge(genre, share, Double::sum));
        });
        List<String> genres = genreScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3).map(Map.Entry::getKey).toList();
        Map<UUID, List<Candidate>> interest = group(recommendationRepository.findInterestCandidates(interestArtists));
        // Prefer the artist's known tracks; retain up to five alternatives to avoid duplicate collaborations.
        interest.values().forEach(candidates -> candidates.sort(
                Comparator.comparingDouble((Candidate candidate) -> trackScores.getOrDefault(candidate.trackId(), 0.0)).reversed()));
        List<Selection> selected = new ArrayList<>();
        Set<UUID> selectedArtists = new HashSet<>();
        Set<String> selectedTracks = new HashSet<>();
        select(interestArtists, interest, INTEREST, MAX_ITEMS, selected, selectedArtists, selectedTracks);
        if (selected.size() < MAX_ITEMS) {
            Map<UUID, List<Candidate>> discovery = group(
                    recommendationRepository.findDiscoveryCandidates(deviceId, genres, artistScores.keySet()));
            List<UUID> discoveryArtists = new ArrayList<>(discovery.keySet());
            Collections.shuffle(discoveryArtists);
            select(discoveryArtists, discovery, DISCOVERY, MAX_ITEMS, selected, selectedArtists, selectedTracks);
        }

        if (selected.size() < MAX_ITEMS) {
            // Read the existing latest overall weekly snapshot only. This endpoint never creates a chart.
            var weekly = chartRepository.findLatestOverallChartByChartType(ChartType.WEEKLY);
            var weeklyArtists = loadArtists(weekly.stream().map(entry -> entry.getTrack().getTrackId()).distinct().toList());
            for (var entry : weekly) {
                for (var artist : weeklyArtists.getOrDefault(entry.getTrack().getTrackId(), List.of())) {
                    if (selected.size() == MAX_ITEMS) break;
                    if (selectedArtists.contains(artist.id()) || selectedTracks.contains(entry.getTrack().getTrackId())) continue;
                    selected.add(new Selection(new Candidate(artist.id(), entry.getTrack().getTrackId()), WEEKLY_CHART));
                    selectedArtists.add(artist.id());
                    selectedTracks.add(entry.getTrack().getTrackId());
                    break;
                }
                if (selected.size() == MAX_ITEMS) break;
            }
        }
        if (selected.isEmpty()) return new PlaylistRecommendationResponse(List.of());

        List<String> trackIds = selected.stream().map(item -> item.candidate().trackId()).toList();
        Map<String, PlaylistTrack> tracks = trackRepository.findAllById(trackIds).stream()
                .collect(Collectors.toMap(PlaylistTrack::getTrackId, track -> track));
        Map<String, List<PlaylistArtistResponse>> artists = loadArtists(trackIds);
        List<PlaylistRecommendationResponse.Item> items = new ArrayList<>();
        for (Selection item : selected) {
            PlaylistTrack track = tracks.get(item.candidate().trackId());
            List<PlaylistArtistResponse> trackArtists = artists.getOrDefault(item.candidate().trackId(), List.of());
            var artist = trackArtists.stream().filter(value -> value.id().equals(item.candidate().artistId())).findFirst();
            // Concurrent deletion must not leave an invalid artist or track card in the response.
            if (track == null || artist.isEmpty()) continue;
            items.add(new PlaylistRecommendationResponse.Item(artist.get(),
                    new PlaylistRecommendationResponse.Track(track.getTrackId(), track.getTitle(), track.getAlbumArtUrl(), trackArtists),
                    item.source()));
        }
        return new PlaylistRecommendationResponse(items);
    }

    private Map<String, List<PlaylistArtistResponse>> loadArtists(List<String> trackIds) {
        if (trackIds.isEmpty()) return Map.of();
        return linkRepository.findWithArtistsByTrackIds(trackIds).stream()
                .collect(Collectors.groupingBy(link -> link.getTrack().getTrackId(), LinkedHashMap::new,
                        Collectors.mapping(link -> PlaylistArtistResponse.from(link.getArtist()), Collectors.toList())));
    }

    private Map<UUID, List<Candidate>> group(List<Candidate> candidates) {
        return candidates.stream().collect(Collectors.groupingBy(Candidate::artistId, LinkedHashMap::new, Collectors.toList()));
    }

    private void select(List<UUID> artistIds, Map<UUID, List<Candidate>> candidates,
                        PlaylistRecommendationResponse.Source source, int targetSize,
                        List<Selection> selected, Set<UUID> selectedArtists, Set<String> selectedTracks) {
        for (UUID artistId : artistIds) {
            if (selected.size() >= targetSize) break;
            if (selectedArtists.contains(artistId)) continue;
            candidates.getOrDefault(artistId, List.of()).stream()
                    .filter(candidate -> !selectedTracks.contains(candidate.trackId())).findFirst().ifPresent(candidate -> {
                        selected.add(new Selection(candidate, source));
                        selectedArtists.add(artistId);
                        selectedTracks.add(candidate.trackId());
                    });
        }
    }

    private record Selection(Candidate candidate, PlaylistRecommendationResponse.Source source) { }
}
