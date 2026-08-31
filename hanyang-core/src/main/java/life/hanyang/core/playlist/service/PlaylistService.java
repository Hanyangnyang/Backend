package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.PlaylistChartRepository;
import life.hanyang.core.playlist.repository.PlaylistSongLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReactionRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackHourlyPlayRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM.dd HH:00").withZone(KST);
    public static final int DAILY_MAX_CREATE_LIMIT = 3;

    private final PlaylistTrackRepository playlistTrackRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongLikeRepository playlistSongLikeRepository;
    private final PlaylistSongReactionRepository playlistSongReactionRepository;
    private final PlaylistSongReportRepository playlistSongReportRepository;
    private final PlaylistModerationService playlistModerationService;
    private final PlaylistTrackHourlyPlayRepository playlistTrackHourlyPlayRepository;
    private final PlaylistChartRepository playlistChartRepository;

    /**
     * 1. 곡 추천/등록
     */
    @Transactional
    public PlaylistSongResponse createSong(PlaylistSongCreateRequest request, String clientIp) {
        // 1-1. 장르 개수(1~3개) 2차 방어 검증 (비용 0원)
        if (request.genres() == null || request.genres().isEmpty() || request.genres().size() > 3) {
            throw new BusinessException("장르는 최소 1개에서 최대 3개까지 선택해야 합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1-2. 오늘(00:00~23:59:59 KST) 등록 횟수 3곡 제한 검증 (비용 0원)
        Instant startOfToday = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        long todayCount = playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(request.deviceId(), startOfToday);
        if (todayCount >= DAILY_MAX_CREATE_LIMIT) {
            throw new BusinessException("오늘 추천 가능한 곡 수(최대 3곡)를 초과했습니다.", ErrorCode.PLAYLIST_DAILY_LIMIT_EXCEEDED);
        }

        // 1-3. 최근 7일(요일 기준) 동일 곡 중복 추천 검증 (비용 0원)
        Instant startOf7DaysAgo = LocalDate.now(KST).minusDays(6).atStartOfDay(KST).toInstant();
        boolean alreadyCreatedIn7Days = playlistSongRepository.existsByDeviceIdAndTrackTrackIdAndCreatedAtAfterAndDeletedAtIsNull(
                request.deviceId(), request.trackId(), startOf7DaysAgo
        );
        if (alreadyCreatedIn7Days) {
            throw new BusinessException("최근 7일 이내에 이미 추천한 곡입니다. 다른 곡을 추천해 주세요.", ErrorCode.PLAYLIST_DUPLICATE_SONG_IN_WEEK);
        }

        // 1-4. 위의 모든 검증 통과 시에만 AI 실시간 코멘트/세로드립 검열 수행 (과금 방어)
        boolean isAiModerated = playlistModerationService.validateSongContent(
                request.title(),
                request.artist(),
                request.comment()
        );

        // 1-4. 음원 마스터(PlaylistTrack) 조회 또는 신규 생성
        PlaylistTrack track = playlistTrackRepository.findById(request.trackId())
                .orElseGet(() -> playlistTrackRepository.save(
                        PlaylistTrack.builder()
                                .trackId(request.trackId())
                                .title(request.title())
                                .artist(request.artist())
                                .albumArtUrl(request.albumArtUrl())
                                .build()
                ));

        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .comment(request.comment())
                .deviceId(request.deviceId())
                .ipAddress(clientIp != null ? clientIp : "UNKNOWN")
                .genres(request.genres())
                .isAiModerated(isAiModerated)
                .build();

        PlaylistSong saved = playlistSongRepository.save(song);
        return PlaylistSongResponse.of(saved, false);
    }

    /**
     * 1-1. 곡 작성 전 사용자 상태 조회 (오늘 등록 잔여 횟수 + 최근 7일 추천 트랙 ID 목록)
     */
    public PlaylistCreationStatusResponse getCreationStatus(UUID deviceId) {
        Instant startOfToday = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        long todayCount = playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(deviceId, startOfToday);

        Instant startOf7DaysAgo = LocalDate.now(KST).minusDays(6).atStartOfDay(KST).toInstant();
        Set<String> recentTrackIds = playlistSongRepository.findRecentTrackIdsByDeviceIdAndCreatedAtAfter(deviceId, startOf7DaysAgo);

        return PlaylistCreationStatusResponse.of(todayCount, DAILY_MAX_CREATE_LIMIT, recentTrackIds);
    }

    /**
     * 2. 피드 목록 조회 (장르 선택 필터, 최신순 페이징, isLiked 배치 조회)
     */
    public Page<PlaylistSongResponse> getFeedSongs(Genre genre, Pageable pageable, UUID currentDeviceId) {
        Page<PlaylistSong> songPage = playlistSongRepository.searchSongs(genre, pageable);
        List<PlaylistSong> songs = songPage.getContent();

        if (songs.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, songPage.getTotalElements());
        }

        List<UUID> songIds = songs.stream().map(PlaylistSong::getId).toList();

        // isLiked N+1 방지를 위한 1번의 Batch IN 쿼리
        Set<UUID> likedSongIds = (currentDeviceId != null)
                ? playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(currentDeviceId, songIds)
                : Collections.emptySet();

        // Reactions N+1 방지를 위한 2번의 Batch IN 쿼리 (카운트 + 내 반응)
        Map<UUID, List<PlaylistReactionItemResponse>> reactionMap = buildBatchReactionMap(songIds, currentDeviceId);

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(
                        song,
                        likedSongIds.contains(song.getId()),
                        reactionMap.getOrDefault(song.getId(), Collections.emptyList())
                ))
                .toList();

        return new PageImpl<>(responses, pageable, songPage.getTotalElements());
    }

    /**
     * 2-1. 특정 추천글 단건 상세 조회 (딥링크/공유/알림 연동용)
     */
    public PlaylistSongResponse getSong(UUID songId, UUID currentDeviceId) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 추천글입니다. id: " + songId));

        boolean isLiked = (currentDeviceId != null) &&
                playlistSongLikeRepository.existsBySongIdAndDeviceId(songId, currentDeviceId);

        List<PlaylistReactionItemResponse> reactions = buildSingleReactionList(songId, currentDeviceId);

        return PlaylistSongResponse.of(song, isLiked, reactions);
    }

    /**
     * 2-2. 추천글 통합 가중치 검색 (곡 제목 100점 > 가수명 80점 > 코멘트 내용 20점)
     */
    public Page<PlaylistSongResponse> searchSongsWithWeight(String keyword, Pageable pageable, UUID currentDeviceId) {
        Page<PlaylistSong> songPage = playlistSongRepository.searchSongsWithWeight(keyword, pageable);
        List<PlaylistSong> songs = songPage.getContent();

        if (songs.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, songPage.getTotalElements());
        }

        List<UUID> songIds = songs.stream().map(PlaylistSong::getId).toList();

        Set<UUID> likedSongIds = (currentDeviceId != null)
                ? playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(currentDeviceId, songIds)
                : Collections.emptySet();

        Map<UUID, List<PlaylistReactionItemResponse>> reactionMap = buildBatchReactionMap(songIds, currentDeviceId);

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(
                        song,
                        likedSongIds.contains(song.getId()),
                        reactionMap.getOrDefault(song.getId(), Collections.emptyList())
                ))
                .toList();

        return new PageImpl<>(responses, pageable, songPage.getTotalElements());
    }

    /**
     * 2-3. 내가 작성한 추천글 목록 조회 (최신순 페이징, N+1 방지 배치 로딩)
     */
    public Page<PlaylistSongResponse> getMySongs(UUID deviceId, Pageable pageable) {
        Page<PlaylistSong> songPage = playlistSongRepository.searchMySongs(deviceId, pageable);
        List<PlaylistSong> songs = songPage.getContent();

        if (songs.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, songPage.getTotalElements());
        }

        List<UUID> songIds = songs.stream().map(PlaylistSong::getId).toList();

        // isLiked 배치 조회
        Set<UUID> likedSongIds = (deviceId != null)
                ? playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(deviceId, songIds)
                : Collections.emptySet();

        // Reactions 배치 조회
        Map<UUID, List<PlaylistReactionItemResponse>> reactionMap = buildBatchReactionMap(songIds, deviceId);

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(
                        song,
                        likedSongIds.contains(song.getId()),
                        reactionMap.getOrDefault(song.getId(), Collections.emptyList())
                ))
                .toList();

        return new PageImpl<>(responses, pageable, songPage.getTotalElements());
    }

    /**
     * 2-4. 음원 트랙 목록 검색 (곡명/가수명 일치 트랙 목록 반환)
     */
    public Page<PlaylistTrackSearchResponse> searchTracks(String keyword, Pageable pageable) {
        return playlistTrackRepository.searchTracks(keyword, pageable);
    }

    /**
     * 3. 특정 곡(트랙)의 상세 정보 및 추천글 페이징 조회 (베스트 하트순/최신순)
     */
    public PlaylistTrackDetailResponse getTrackDetailAndSongs(String trackId, Pageable pageable, UUID currentDeviceId) {
        PlaylistTrack track = playlistTrackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 음원 트랙입니다. trackId: " + trackId));

        Page<PlaylistSong> songPage = playlistSongRepository.searchSongsByTrackId(trackId, pageable);
        List<PlaylistSong> songs = songPage.getContent();

        List<UUID> songIds = songs.stream().map(PlaylistSong::getId).toList();

        // isLiked Batch IN 쿼리 판별
        Set<UUID> likedSongIds = (currentDeviceId != null && !songs.isEmpty())
                ? playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(currentDeviceId, songIds)
                : Collections.emptySet();

        // Reactions Batch IN 쿼리 판별
        Map<UUID, List<PlaylistReactionItemResponse>> reactionMap = buildBatchReactionMap(songIds, currentDeviceId);

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(
                        song,
                        likedSongIds.contains(song.getId()),
                        reactionMap.getOrDefault(song.getId(), Collections.emptyList())
                ))
                .toList();

        Page<PlaylistSongResponse> responsePage = new PageImpl<>(responses, pageable, songPage.getTotalElements());
        long totalHeartCount = playlistSongRepository.sumHeartCountByTrackId(trackId);

        return PlaylistTrackDetailResponse.of(track, songPage.getTotalElements(), totalHeartCount, responsePage);
    }

    /**
     * 4. 좋아요 토글 (동시성 제어 및 원자적 카운트 증감)
     */
    @Transactional
    public PlaylistLikeToggleResponse toggleLike(UUID songId, UUID deviceId) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 곡입니다. id: " + songId));

        Optional<PlaylistSongLike> existingLike = playlistSongLikeRepository.findBySongIdAndDeviceId(songId, deviceId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // [좋아요 취소]
            playlistSongLikeRepository.delete(existingLike.get());
            playlistSongRepository.decrementHeartCount(songId);
            isLiked = false;
        } else {
            // [좋아요 등록]
            try {
                PlaylistSongLike newLike = PlaylistSongLike.builder()
                        .song(song)
                        .deviceId(deviceId)
                        .build();
                playlistSongLikeRepository.save(newLike);
                playlistSongRepository.incrementHeartCount(songId);
                isLiked = true;
            } catch (DataIntegrityViolationException e) {
                log.warn("[PlaylistLike] 중복 좋아요 요청 감지 (동시성 방어됨) - songId: {}, deviceId: {}", songId, deviceId);
                isLiked = true;
            }
        }

        Integer currentHeartCount = playlistSongRepository.getHeartCount(songId).orElse(0);
        return new PlaylistLikeToggleResponse(isLiked, currentHeartCount);
    }

    /**
     * 4-1. 이모지 리액션 토글 (10종 이모지 동시성 제어 및 최신 반응 요약 반환)
     */
    @Transactional
    public PlaylistReactionToggleResponse toggleReaction(UUID songId, PlaylistReactionToggleRequest request) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 곡입니다. id: " + songId));

        Optional<PlaylistSongReaction> existing = playlistSongReactionRepository
                .findBySongIdAndDeviceIdAndReactionType(songId, request.deviceId(), request.reactionType());

        boolean isReacted;
        if (existing.isPresent()) {
            // [리액션 취소]
            playlistSongReactionRepository.deleteBySongIdAndDeviceIdAndReactionType(
                    songId, request.deviceId(), request.reactionType()
            );
            isReacted = false;
        } else {
            // [리액션 추가]
            try {
                PlaylistSongReaction newReaction = PlaylistSongReaction.builder()
                        .song(song)
                        .deviceId(request.deviceId())
                        .reactionType(request.reactionType())
                        .build();
                playlistSongReactionRepository.save(newReaction);
                isReacted = true;
            } catch (DataIntegrityViolationException e) {
                log.warn("[PlaylistReaction] 중복 리액션 동시성 방어 - songId: {}, deviceId: {}, type: {}",
                        songId, request.deviceId(), request.reactionType());
                isReacted = true;
            }
        }

        List<PlaylistReactionItemResponse> reactions = buildSingleReactionList(songId, request.deviceId());
        return PlaylistReactionToggleResponse.of(songId, request.reactionType(), isReacted, reactions);
    }

    /**
     * 5. 내가 좋아요 누른 곡 목록 조회
     */
    public Page<PlaylistSongResponse> getLikedSongs(UUID deviceId, Pageable pageable) {
        Page<PlaylistSong> likedSongs = playlistSongLikeRepository.findLikedSongsByDeviceId(deviceId, pageable);
        List<PlaylistSong> songs = likedSongs.getContent();
        List<UUID> songIds = songs.stream().map(PlaylistSong::getId).toList();

        Map<UUID, List<PlaylistReactionItemResponse>> reactionMap = buildBatchReactionMap(songIds, deviceId);

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(
                        song,
                        true,
                        reactionMap.getOrDefault(song.getId(), Collections.emptyList())
                ))
                .toList();

        return new PageImpl<>(responses, pageable, likedSongs.getTotalElements());
    }

    private List<PlaylistReactionItemResponse> buildSingleReactionList(UUID songId, UUID deviceId) {
        List<Object[]> counts = playlistSongReactionRepository.countReactionsBySongId(songId);
        Map<ReactionType, Long> countMap = new EnumMap<>(ReactionType.class);
        for (Object[] row : counts) {
            ReactionType type = (ReactionType) row[0];
            Long cnt = ((Number) row[1]).longValue();
            countMap.put(type, cnt);
        }

        Set<ReactionType> userReactions = (deviceId != null)
                ? playlistSongReactionRepository.findUserReactionTypesByDeviceIdAndSongId(deviceId, songId)
                : Collections.emptySet();

        return Arrays.stream(ReactionType.values())
                .map(type -> PlaylistReactionItemResponse.of(
                        type,
                        countMap.getOrDefault(type, 0L),
                        userReactions.contains(type)
                ))
                .toList();
    }

    private Map<UUID, List<PlaylistReactionItemResponse>> buildBatchReactionMap(List<UUID> songIds, UUID deviceId) {
        if (songIds == null || songIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Batch IN Query 1: 전체 이모지 카운트 집계
        List<Object[]> counts = playlistSongReactionRepository.countReactionsBySongIdIn(songIds);
        Map<UUID, Map<ReactionType, Long>> songCountMap = new HashMap<>();
        for (Object[] row : counts) {
            UUID sId = (UUID) row[0];
            ReactionType type = (ReactionType) row[1];
            Long cnt = ((Number) row[2]).longValue();
            songCountMap.computeIfAbsent(sId, k -> new EnumMap<>(ReactionType.class)).put(type, cnt);
        }

        // 2. Batch IN Query 2: 현재 기기가 누른 이모지 집계
        Map<UUID, Set<ReactionType>> userReactionMap = new HashMap<>();
        if (deviceId != null) {
            List<Object[]> userRows = playlistSongReactionRepository.findUserReactionsByDeviceIdAndSongIdIn(deviceId, songIds);
            for (Object[] row : userRows) {
                UUID sId = (UUID) row[0];
                ReactionType type = (ReactionType) row[1];
                userReactionMap.computeIfAbsent(sId, k -> EnumSet.noneOf(ReactionType.class)).add(type);
            }
        }

        // 3. 메모리에서 10대 이모지 합성 (O(1))
        Map<UUID, List<PlaylistReactionItemResponse>> resultMap = new HashMap<>();
        for (UUID sId : songIds) {
            Map<ReactionType, Long> countsForSong = songCountMap.getOrDefault(sId, Collections.emptyMap());
            Set<ReactionType> userReactionsForSong = userReactionMap.getOrDefault(sId, Collections.emptySet());

            List<PlaylistReactionItemResponse> list = Arrays.stream(ReactionType.values())
                    .map(type -> PlaylistReactionItemResponse.of(
                            type,
                            countsForSong.getOrDefault(type, 0L),
                            userReactionsForSong.contains(type)
                    ))
                    .toList();
            resultMap.put(sId, list);
        }

        return resultMap;
    }

    /**
     * 6. 곡 신고하기
     */
    @Transactional
    public PlaylistSongReportResponse reportSong(UUID songId, PlaylistSongReportRequest request) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 곡입니다. id: " + songId));

        PlaylistSongReport report = PlaylistSongReport.builder()
                .song(song)
                .reporterDeviceId(request.reporterDeviceId())
                .reason(request.reason())
                .build();

        PlaylistSongReport saved = playlistSongReportRepository.save(report);
        return PlaylistSongReportResponse.from(saved);
    }

    /**
     * 7. 음원 재생수 카운트 1 증가 (원자적 1시간 단위 Upsert)
     */
    @Transactional
    public void recordTrackPlay(String trackId) {
        if (!playlistTrackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("존재하지 않는 음원 트랙입니다. trackId: " + trackId);
        }

        Instant currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        playlistTrackHourlyPlayRepository.upsertHourlyPlayCount(trackId, currentHour);
        log.debug("[PlaylistPlay] 음원 재생수 기록 완료 - trackId: {}, playHour: {}", trackId, currentHour);
    }

    /**
     * 8. 인기 차트 순위 조회 (Redis 캐시 우선 조회 ➡️ DB 스냅샷 ➡️ 비어있을 시 즉시 계산 폴백)
     */
    @Cacheable(cacheNames = "playlistChart", key = "#type != null ? #type : T(life.hanyang.core.playlist.domain.ChartType).RISING")
    public PlaylistChartResponse getChart(ChartType type) {
        ChartType chartType = (type != null) ? type : ChartType.RISING;

        // 1. DB 스냅샷 테이블에서 최신 차트 목록 조회
        List<PlaylistChart> latestChart = playlistChartRepository.findLatestChartByChartType(chartType);
        if (!latestChart.isEmpty()) {
            PlaylistChart first = latestChart.get(0);
            List<PlaylistChartItemResponse> items = latestChart.stream()
                    .map(c -> new PlaylistChartItemResponse(
                            c.getRank(),
                            c.getTrack().getTrackId(),
                            c.getTrack().getTitle(),
                            c.getTrack().getArtist(),
                            c.getTrack().getAlbumArtUrl()
                    ))
                    .toList();

            String displayTitle = formatDisplayTitle(chartType, first.getSnapshotTime(), first.getStartPeriod());
            return PlaylistChartResponse.of(
                    chartType,
                    first.getSnapshotTime(),
                    first.getStartPeriod(),
                    first.getEndPeriod(),
                    displayTitle,
                    items
            );
        }

        // 2. 만약 DB에도 스냅샷이 없다면 (최초 가동 폴백) 즉시 계산 및 영구 저장
        log.info("[PlaylistChart] 저장된 차트 스냅샷이 없어 즉시 계산 및 저장을 수행합니다: {}", chartType);
        return calculateAndSaveChart(chartType, Instant.now());
    }

    /**
     * 9. 특정 시점 기준 차트 1~100위 계산, DB 영구 저장 및 Redis 캐시 갱신 (@CachePut)
     */
    @Transactional
    @CachePut(cacheNames = "playlistChart", key = "#chartType")
    public PlaylistChartResponse calculateAndSaveChart(ChartType chartType, Instant targetTime) {
        Instant now = (targetTime != null) ? targetTime : Instant.now();
        ZonedDateTime nowKst = now.atZone(KST);

        Instant snapshotTime;
        Instant startPeriod;
        Instant endPeriod;
        String displayTitle;

        switch (chartType) {
            case RISING -> {
                // 직전 정각 기준 24시간 풀 + 3시간 부스터
                ZonedDateTime targetHourKst = nowKst.truncatedTo(ChronoUnit.HOURS);
                snapshotTime = targetHourKst.toInstant();
                endPeriod = snapshotTime;
                startPeriod = targetHourKst.minusHours(24).toInstant();
                Instant h3 = targetHourKst.minusHours(3).toInstant();
                displayTitle = TIME_FMT.format(targetHourKst) + " 기준 실시간 급상승";

                List<Object[]> rows = playlistTrackHourlyPlayRepository.findRisingChartRaw(startPeriod, h3, endPeriod, 100);
                return saveAndBuildResponse(ChartType.RISING, snapshotTime, startPeriod, endPeriod, displayTitle, rows);
            }
            case WEEKLY -> {
                // 직전 월요일 자정 기준 지난주 월(00:00) ~ 일(23:59:59.999)
                LocalDate today = nowKst.toLocalDate();
                LocalDate lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate prevMonday = lastMonday.minusWeeks(1);

                snapshotTime = lastMonday.atStartOfDay(KST).toInstant();
                startPeriod = prevMonday.atStartOfDay(KST).toInstant();
                endPeriod = lastMonday.atStartOfDay(KST).toInstant();

                int weekOfMonth = prevMonday.get(WeekFields.of(Locale.KOREA).weekOfMonth());
                displayTitle = prevMonday.getMonthValue() + "월 " + weekOfMonth + "주차 주간 차트";

                List<Object[]> rows = playlistTrackHourlyPlayRepository.findWeeklyChartRaw(startPeriod, endPeriod, 100);
                return saveAndBuildResponse(ChartType.WEEKLY, snapshotTime, startPeriod, endPeriod, displayTitle, rows);
            }
            case MONTHLY -> {
                // 이번 달 1일 자정 기준 지난달 1일(00:00) ~ 말일(23:59:59.999)
                LocalDate today = nowKst.toLocalDate();
                LocalDate firstDayOfThisMonth = today.withDayOfMonth(1);
                LocalDate firstDayOfPrevMonth = firstDayOfThisMonth.minusMonths(1);

                snapshotTime = firstDayOfThisMonth.atStartOfDay(KST).toInstant();
                startPeriod = firstDayOfPrevMonth.atStartOfDay(KST).toInstant();
                endPeriod = firstDayOfThisMonth.atStartOfDay(KST).toInstant();

                displayTitle = firstDayOfPrevMonth.getYear() + "년 " + firstDayOfPrevMonth.getMonthValue() + "월 월간 차트";

                List<Object[]> rows = playlistTrackHourlyPlayRepository.findMonthlyChartRaw(startPeriod, endPeriod, 100);
                return saveAndBuildResponse(ChartType.MONTHLY, snapshotTime, startPeriod, endPeriod, displayTitle, rows);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 차트 유형입니다: " + chartType);
        }
    }

    private PlaylistChartResponse saveAndBuildResponse(
            ChartType chartType,
            Instant snapshotTime,
            Instant startPeriod,
            Instant endPeriod,
            String displayTitle,
            List<Object[]> rows
    ) {
        List<PlaylistChartItemResponse> items = new ArrayList<>(rows.size());
        List<PlaylistChart> entities = new ArrayList<>(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String trackId = (String) row[0];
            String title = (String) row[1];
            String artist = (String) row[2];
            String albumArtUrl = (String) row[3];
            Long score = (row.length > 4 && row[4] instanceof Number n) ? n.longValue() : 0L;
            int rank = i + 1;

            items.add(new PlaylistChartItemResponse(rank, trackId, title, artist, albumArtUrl));

            PlaylistTrack trackRef = playlistTrackRepository.getReferenceById(trackId);
            entities.add(PlaylistChart.builder()
                    .chartType(chartType)
                    .snapshotTime(snapshotTime)
                    .startPeriod(startPeriod)
                    .endPeriod(endPeriod)
                    .rank(rank)
                    .track(trackRef)
                    .totalScore(score)
                    .build());
        }

        if (!entities.isEmpty()) {
            playlistChartRepository.saveAll(entities);
        }

        log.info("[PlaylistChart] 차트 스냅샷 생성 및 캐싱 완료 - type: {}, snapshotTime: {}, totalTracks: {}",
                chartType, snapshotTime, items.size());

        return PlaylistChartResponse.of(chartType, snapshotTime, startPeriod, endPeriod, displayTitle, items);
    }

    private String formatDisplayTitle(ChartType chartType, Instant snapshotTime, Instant startPeriod) {
        ZonedDateTime snapKst = snapshotTime.atZone(KST);
        ZonedDateTime startKst = startPeriod.atZone(KST);

        return switch (chartType) {
            case RISING -> TIME_FMT.format(snapKst) + " 기준 실시간 급상승";
            case WEEKLY -> {
                int weekOfMonth = startKst.get(WeekFields.of(Locale.KOREA).weekOfMonth());
                yield startKst.getMonthValue() + "월 " + weekOfMonth + "주차 주간 차트";
            }
            case MONTHLY -> startKst.getYear() + "년 " + startKst.getMonthValue() + "월 월간 차트";
        };
    }
}
