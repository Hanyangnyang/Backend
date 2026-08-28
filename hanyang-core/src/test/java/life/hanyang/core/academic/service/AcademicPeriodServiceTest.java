package life.hanyang.core.academic.service;

import life.hanyang.core.academic.domain.AcademicPeriod;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;
import life.hanyang.core.academic.dto.AcademicPeriodCreateRequest;
import life.hanyang.core.academic.dto.AcademicPeriodResponse;
import life.hanyang.core.academic.dto.AcademicPeriodUpdateRequest;
import life.hanyang.core.academic.dto.UnifiedOperationStatusResponse;
import life.hanyang.core.academic.repository.AcademicPeriodRepository;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.service.HolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private HolidayService holidayService;

    @Mock
    private TransactionCacheEvictor transactionCacheEvictor;

    @InjectMocks
    private AcademicPeriodService academicPeriodService;

    @Test
    @DisplayName("통합 운영 상태 조회 성공 - 평일 및 방학중")
    void getUnifiedOperationStatus_Weekday_Vacation() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 8, 28);
        DateInfoResponse holidayInfo = DateInfoResponse.of(targetDate, DayType.WEEKDAY, null);
        given(holidayService.getDateInfo(targetDate)).willReturn(holidayInfo);

        AcademicPeriod period = AcademicPeriod.builder()
                .year(2026)
                .semester(AcademicSemester.FIRST)
                .periodType(AcademicPeriodType.VACATION)
                .name("26년 여름방학")
                .startDate(LocalDate.of(2026, 6, 24))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();
        given(academicPeriodRepository.findFirstByStartDateLessThanEqualOrderByStartDateDesc(targetDate))
                .willReturn(Optional.of(period));

        // when
        UnifiedOperationStatusResponse response = academicPeriodService.getUnifiedOperationStatus(targetDate);

        // then
        assertThat(response.date()).isEqualTo(targetDate);
        assertThat(response.calendar().dayType()).isEqualTo(DayType.WEEKDAY);
        assertThat(response.calendar().isHoliday()).isFalse();
        assertThat(response.academic().periodType()).isEqualTo(AcademicPeriodType.VACATION);
        assertThat(response.academic().title()).isEqualTo("26년 여름방학");
        assertThat(response.shuttle().isOperating()).isTrue();
        assertThat(response.shuttle().periodType()).isEqualTo(AcademicPeriodType.VACATION);
        assertThat(response.shuttle().dayType()).isEqualTo(DayType.WEEKDAY);
        assertThat(response.shuttle().noOperationReason()).isNull();
    }

    @Test
    @DisplayName("통합 운영 상태 조회 성공 - 공휴일인 경우 셔틀은 주말시간표 운행")
    void getUnifiedOperationStatus_Holiday_ShuttleWeekend() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 5);
        DateInfoResponse holidayInfo = DateInfoResponse.of(targetDate, DayType.HOLIDAY, "어린이날");
        given(holidayService.getDateInfo(targetDate)).willReturn(holidayInfo);

        AcademicPeriod period = AcademicPeriod.builder()
                .year(2026)
                .semester(AcademicSemester.FIRST)
                .periodType(AcademicPeriodType.SEMESTER)
                .name("26년 1학기")
                .startDate(LocalDate.of(2026, 3, 3))
                .endDate(LocalDate.of(2026, 6, 23))
                .build();
        given(academicPeriodRepository.findFirstByStartDateLessThanEqualOrderByStartDateDesc(targetDate))
                .willReturn(Optional.of(period));

        // when
        UnifiedOperationStatusResponse response = academicPeriodService.getUnifiedOperationStatus(targetDate);

        // then
        assertThat(response.calendar().dayType()).isEqualTo(DayType.HOLIDAY);
        assertThat(response.calendar().isHoliday()).isTrue();
        assertThat(response.calendar().holidayName()).isEqualTo("어린이날");
        assertThat(response.shuttle().isOperating()).isTrue();
        assertThat(response.shuttle().dayType()).isEqualTo(DayType.WEEKEND); // 💡 공휴일은 주말 시간표
    }

    @Test
    @DisplayName("통합 운영 상태 조회 성공 - 미운행일(NO_OPERATION)인 경우 셔틀 운행 중단")
    void getUnifiedOperationStatus_NoOperation() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 1, 1);
        DateInfoResponse holidayInfo = DateInfoResponse.of(targetDate, DayType.NO_OPERATION, "신정 셔틀 미운행");
        given(holidayService.getDateInfo(targetDate)).willReturn(holidayInfo);

        given(academicPeriodRepository.findFirstByStartDateLessThanEqualOrderByStartDateDesc(targetDate))
                .willReturn(Optional.empty());

        // when
        UnifiedOperationStatusResponse response = academicPeriodService.getUnifiedOperationStatus(targetDate);

        // then
        assertThat(response.calendar().dayType()).isEqualTo(DayType.NO_OPERATION);
        assertThat(response.shuttle().isOperating()).isFalse();
        assertThat(response.shuttle().noOperationReason()).isEqualTo("신정 셔틀 미운행");
    }

    @Test
    @DisplayName("학사 일정 등록 성공")
    void createPeriod_Success() {
        // given
        AcademicPeriodCreateRequest request = new AcademicPeriodCreateRequest(
                2026,
                AcademicSemester.FIRST,
                AcademicPeriodType.SEMESTER,
                "2026학년도 1학기",
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 6, 23)
        );
        given(academicPeriodRepository.existsByYearAndSemesterAndPeriodType(2026, AcademicSemester.FIRST, AcademicPeriodType.SEMESTER))
                .willReturn(false);
        AcademicPeriod saved = request.toEntity();
        given(academicPeriodRepository.save(any(AcademicPeriod.class))).willReturn(saved);

        // when
        AcademicPeriodResponse response = academicPeriodService.createPeriod(request);

        // then
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.semester()).isEqualTo(AcademicSemester.FIRST);
        assertThat(response.periodType()).isEqualTo(AcademicPeriodType.SEMESTER);
        verify(transactionCacheEvictor).evictCacheAfterCommit("academicOperationStatus");
        verify(transactionCacheEvictor).evictCacheAfterCommit("academicPeriods");
    }

    @Test
    @DisplayName("학사 일정 중복 등록 시 예외 발생")
    void createPeriod_Duplicate_ThrowsException() {
        // given
        AcademicPeriodCreateRequest request = new AcademicPeriodCreateRequest(
                2026,
                AcademicSemester.FIRST,
                AcademicPeriodType.SEMESTER,
                "2026학년도 1학기",
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 6, 23)
        );
        given(academicPeriodRepository.existsByYearAndSemesterAndPeriodType(2026, AcademicSemester.FIRST, AcademicPeriodType.SEMESTER))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> academicPeriodService.createPeriod(request))
                .isInstanceOf(BusinessException.class);
    }
}
