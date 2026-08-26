package life.hanyang.core.holiday.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.holiday.client.PublicHolidayApiClient;
import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.domain.Holiday;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.dto.HolidayCreateRequest;
import life.hanyang.core.holiday.dto.HolidayResponse;
import life.hanyang.core.holiday.dto.HolidayUpdateRequest;
import life.hanyang.core.holiday.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final PublicHolidayApiClient publicHolidayApiClient;
    private final TransactionCacheEvictor transactionCacheEvictor;

    /**
     * 날짜 상태 판별 (단건)
     */
    @Cacheable(cacheNames = "holidayDateInfo", key = "#date.toString()")
    public DateInfoResponse getDateInfo(LocalDate date) {
        // 1. DB 등록 확인 (HOLIDAY 또는 NO_OPERATION)
        Optional<Holiday> holidayOpt = holidayRepository.findByDate(date);
        if (holidayOpt.isPresent()) {
            Holiday holiday = holidayOpt.get();
            return DateInfoResponse.of(date, holiday.getDayType(), holiday.getName());
        }

        // 2. DB에 없으면 요일 기준 판별 (WEEKEND 또는 WEEKDAY)
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return DateInfoResponse.of(date, DayType.WEEKEND, null);
        }

        return DateInfoResponse.of(date, DayType.WEEKDAY, null);
    }

    /**
     * 공휴일 목록 조회 (연도 / 월)
     */
    @Cacheable(cacheNames = "holidays", key = "#year + '_' + (#month != null ? #month : 'all')")
    public List<HolidayResponse> getHolidays(int year, Integer month) {
        LocalDate start = (month != null) ? LocalDate.of(year, month, 1) : LocalDate.of(year, 1, 1);
        LocalDate end = (month != null) ? start.plusMonths(1).minusDays(1) : LocalDate.of(year, 12, 31);

        return holidayRepository.findByDateBetweenOrderByDateAsc(start, end)
                .stream()
                .map(HolidayResponse::from)
                .toList();
    }

    /**
     * 공휴일 수동 등록
     */
    @Transactional
    public HolidayResponse createHoliday(HolidayCreateRequest request) {
        if (holidayRepository.existsByDate(request.date())) {
            throw new BusinessException("해당 날짜에 이미 등록된 일정이 존재합니다.", ErrorCode.DUPLICATE_RESOURCE);
        }
        Holiday holiday = Holiday.builder()
                .date(request.date())
                .name(request.name())
                .dayType(request.dayType() != null ? request.dayType() : DayType.HOLIDAY)
                .build();
        Holiday saved = holidayRepository.save(holiday);
        evictHolidayCaches();
        return HolidayResponse.from(saved);
    }

    /**
     * 공휴일 수정 (HOLIDAY <-> NO_OPERATION 변경 및 이름 수정)
     */
    @Transactional
    public HolidayResponse updateHoliday(Long id, HolidayUpdateRequest request) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 공휴일 리소스가 존재하지 않습니다. id: " + id, ErrorCode.ENTITY_NOT_FOUND));

        holiday.update(request.name(), request.dayType());
        evictHolidayCaches();
        return HolidayResponse.from(holiday);
    }

    /**
     * 공휴일 삭제
     */
    @Transactional
    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new BusinessException("해당 공휴일 리소스가 존재하지 않습니다. id: " + id, ErrorCode.ENTITY_NOT_FOUND);
        }
        holidayRepository.deleteById(id);
        evictHolidayCaches();
    }

    /**
     * 공공데이터포털 공휴일 동기화 (신규는 HOLIDAY로 등록, 이미 관리자가 NO_OPERATION 등으로 수정한 항목은 dayType 유지)
     */
    @Transactional
    public int syncHolidays(int year) {
        List<PublicHolidayApiClient.PublicHolidayItem> items = publicHolidayApiClient.fetchHolidays(year);
        int count = 0;
        for (PublicHolidayApiClient.PublicHolidayItem item : items) {
            Optional<Holiday> existing = holidayRepository.findByDate(item.date());
            if (existing.isPresent()) {
                // 이미 존재할 경우 이름만 동기화하고, 관리자가 설정한 dayType(예: NO_OPERATION)은 보존
                existing.get().update(item.name(), existing.get().getDayType());
            } else {
                // 신규 등록은 기본적으로 HOLIDAY로 저장
                holidayRepository.save(Holiday.builder()
                        .date(item.date())
                        .name(item.name())
                        .dayType(DayType.HOLIDAY)
                        .build());
            }
            count++;
        }
        evictHolidayCaches();
        log.info("공휴일 동기화 완료 - 연도: {}, 총 건수: {}", year, count);
        return count;
    }

    private void evictHolidayCaches() {
        transactionCacheEvictor.evictCacheAfterCommit("holidayDateInfo");
        transactionCacheEvictor.evictCacheAfterCommit("holidays");
    }
}
