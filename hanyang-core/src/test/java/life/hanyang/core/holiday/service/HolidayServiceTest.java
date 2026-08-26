package life.hanyang.core.holiday.service;

import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.holiday.client.PublicHolidayApiClient;
import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.domain.Holiday;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.dto.HolidayCreateRequest;
import life.hanyang.core.holiday.dto.HolidayResponse;
import life.hanyang.core.holiday.dto.HolidayUpdateRequest;
import life.hanyang.core.holiday.repository.HolidayRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private PublicHolidayApiClient publicHolidayApiClient;

    @Mock
    private TransactionCacheEvictor transactionCacheEvictor;

    @InjectMocks
    private HolidayService holidayService;

    @Test
    @DisplayName("DB에 등록된 공휴일은 HOLIDAY로 반환된다")
    void getDateInfo_Holiday() {
        // given (2026-10-09 한글날)
        LocalDate date = LocalDate.of(2026, 10, 9);
        Holiday holiday = Holiday.builder()
                .date(date)
                .name("한글날")
                .dayType(DayType.HOLIDAY)
                .build();
        given(holidayRepository.findByDate(date)).willReturn(Optional.of(holiday));

        // when
        DateInfoResponse response = holidayService.getDateInfo(date);

        // then
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.dayType()).isEqualTo(DayType.HOLIDAY);
        assertThat(response.name()).isEqualTo("한글날");
    }

    @Test
    @DisplayName("DB에 등록된 미운행 일정은 NO_OPERATION으로 반환된다")
    void getDateInfo_NoOperation() {
        // given (2026-09-25 추석)
        LocalDate date = LocalDate.of(2026, 9, 25);
        Holiday holiday = Holiday.builder()
                .date(date)
                .name("추석")
                .dayType(DayType.NO_OPERATION)
                .build();
        given(holidayRepository.findByDate(date)).willReturn(Optional.of(holiday));

        // when
        DateInfoResponse response = holidayService.getDateInfo(date);

        // then
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.dayType()).isEqualTo(DayType.NO_OPERATION);
        assertThat(response.name()).isEqualTo("추석");
    }

    @Test
    @DisplayName("DB에 없고 토요일/일요일이면 WEEKEND로 반환된다")
    void getDateInfo_Weekend() {
        // given (2026-08-29 토요일)
        LocalDate saturday = LocalDate.of(2026, 8, 29);
        given(holidayRepository.findByDate(saturday)).willReturn(Optional.empty());

        // when
        DateInfoResponse response = holidayService.getDateInfo(saturday);

        // then
        assertThat(response.date()).isEqualTo(saturday);
        assertThat(response.dayType()).isEqualTo(DayType.WEEKEND);
        assertThat(response.name()).isNull();
    }

    @Test
    @DisplayName("DB에 없고 월~금요일이면 WEEKDAY로 반환된다")
    void getDateInfo_Weekday() {
        // given (2026-08-25 화요일)
        LocalDate tuesday = LocalDate.of(2026, 8, 25);
        given(holidayRepository.findByDate(tuesday)).willReturn(Optional.empty());

        // when
        DateInfoResponse response = holidayService.getDateInfo(tuesday);

        // then
        assertThat(response.date()).isEqualTo(tuesday);
        assertThat(response.dayType()).isEqualTo(DayType.WEEKDAY);
        assertThat(response.name()).isNull();
    }

    @Test
    @DisplayName("공휴일 수정 시 dayType을 NO_OPERATION으로 변경할 수 있다")
    void updateHoliday_ChangeDayType() {
        // given
        Long id = 1L;
        Holiday holiday = Holiday.builder()
                .date(LocalDate.of(2026, 9, 25))
                .name("추석")
                .dayType(DayType.HOLIDAY)
                .build();
        given(holidayRepository.findById(id)).willReturn(Optional.of(holiday));

        // when
        HolidayUpdateRequest request = new HolidayUpdateRequest("추석 연휴(미운행)", DayType.NO_OPERATION);
        HolidayResponse response = holidayService.updateHoliday(id, request);

        // then
        assertThat(response.name()).isEqualTo("추석 연휴(미운행)");
        assertThat(response.dayType()).isEqualTo(DayType.NO_OPERATION);
    }

    @Test
    @DisplayName("공공데이터 동기화 시 이미 존재하는 항목의 dayType(NO_OPERATION 등)은 유지된다")
    void syncHolidays_PreservesCustomDayType() {
        // given
        LocalDate date = LocalDate.of(2026, 9, 25);
        Holiday existing = Holiday.builder()
                .date(date)
                .name("추석")
                .dayType(DayType.NO_OPERATION)
                .build();

        given(publicHolidayApiClient.fetchHolidays(2026))
                .willReturn(List.of(new PublicHolidayApiClient.PublicHolidayItem(date, "추석")));
        given(holidayRepository.findByDate(date)).willReturn(Optional.of(existing));

        // when
        int synced = holidayService.syncHolidays(2026);

        // then
        assertThat(synced).isEqualTo(1);
        assertThat(existing.getDayType()).isEqualTo(DayType.NO_OPERATION);
    }
}
