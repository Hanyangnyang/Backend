package life.hanyang.core.academic.service;

import life.hanyang.core.academic.domain.AcademicPeriod;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;
import life.hanyang.core.academic.dto.*;
import life.hanyang.core.academic.repository.AcademicPeriodRepository;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final HolidayService holidayService;
    private final TransactionCacheEvictor transactionCacheEvictor;

    /**
     * 통합 운영 상태 조회 (달력 + 학사 + 셔틀 매트릭스 결합)
     */
    @Cacheable(cacheNames = "academicOperationStatus", key = "#date.toString()")
    public UnifiedOperationStatusResponse getUnifiedOperationStatus(LocalDate date) {
        // 1. 달력 및 휴일 정보 조회 (Holiday 도메인)
        DateInfoResponse holidayInfo = holidayService.getDateInfo(date);
        DayType calDayType = holidayInfo.dayType();
        boolean isHoliday = (calDayType == DayType.HOLIDAY);
        String holidayName = holidayInfo.name();

        // 2. 학사 일정 정보 조회 (AcademicPeriod 도메인 - 시작일 기준 최신 학기 무결성 조회)
        Optional<AcademicPeriod> periodOpt = academicPeriodRepository
                .findFirstByStartDateLessThanEqualOrderByStartDateDesc(date);

        Integer year = periodOpt.map(AcademicPeriod::getYear).orElse(date.getYear());
        AcademicSemester semester = periodOpt.map(AcademicPeriod::getSemester).orElse(AcademicSemester.FIRST);
        AcademicPeriodType periodType = periodOpt.map(AcademicPeriod::getPeriodType).orElse(AcademicPeriodType.SEMESTER);
        String title = periodOpt.map(AcademicPeriod::getName).orElse(null);

        // 3. 셔틀 운행 기준 도출
        boolean isOperating = (calDayType != DayType.NO_OPERATION);
        DayType shuttleDayType = (calDayType == DayType.HOLIDAY || calDayType == DayType.WEEKEND)
                ? DayType.WEEKEND
                : DayType.WEEKDAY;
        String noOperationReason = (calDayType == DayType.NO_OPERATION) ? holidayName : null;

        return new UnifiedOperationStatusResponse(
                date,
                new UnifiedOperationStatusResponse.CalendarStatus(calDayType, isHoliday, holidayName),
                new UnifiedOperationStatusResponse.AcademicStatus(year, semester, periodType, title),
                new UnifiedOperationStatusResponse.ShuttleStatus(isOperating, periodType, shuttleDayType, noOperationReason)
        );
    }

    /**
     * 학사 일정 목록 조회 (연도 필터)
     */
    @Cacheable(cacheNames = "academicPeriods", key = "#year != null ? #year : 'all'")
    public List<AcademicPeriodResponse> getPeriods(Integer year) {
        List<AcademicPeriod> periods = (year != null)
                ? academicPeriodRepository.findByYearOrderByStartDateAsc(year)
                : academicPeriodRepository.findAll(Sort.by(Sort.Direction.ASC, "startDate"));

        return periods.stream()
                .map(AcademicPeriodResponse::from)
                .toList();
    }

    /**
     * 학사 일정 단건 상세 조회
     */
    public AcademicPeriodResponse getPeriod(Long id) {
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학사 일정입니다. id: " + id));
        return AcademicPeriodResponse.from(period);
    }

    /**
     * 학사 일정 신규 등록
     */
    @Transactional
    public AcademicPeriodResponse createPeriod(AcademicPeriodCreateRequest request) {
        if (academicPeriodRepository.existsByYearAndSemesterAndPeriodType(
                request.year(), request.semester(), request.periodType()
        )) {
            throw new BusinessException("해당 연도/학기에 이미 동일한 유형의 학사 일정이 존재합니다.", ErrorCode.DUPLICATE_RESOURCE);
        }

        AcademicPeriod saved = academicPeriodRepository.save(request.toEntity());
        evictAcademicCaches();
        return AcademicPeriodResponse.from(saved);
    }

    /**
     * 학사 일정 수정
     */
    @Transactional
    public AcademicPeriodResponse updatePeriod(Long id, AcademicPeriodUpdateRequest request) {
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학사 일정입니다. id: " + id));

        period.update(
                request.year(),
                request.semester(),
                request.periodType(),
                request.name(),
                request.startDate(),
                request.endDate()
        );

        evictAcademicCaches();
        return AcademicPeriodResponse.from(period);
    }

    /**
     * 학사 일정 삭제
     */
    @Transactional
    public void deletePeriod(Long id) {
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 학사 일정입니다. id: " + id));

        academicPeriodRepository.delete(period);
        evictAcademicCaches();
    }

    private void evictAcademicCaches() {
        transactionCacheEvictor.evictCacheAfterCommit("academicOperationStatus");
        transactionCacheEvictor.evictCacheAfterCommit("academicPeriods");
    }
}
