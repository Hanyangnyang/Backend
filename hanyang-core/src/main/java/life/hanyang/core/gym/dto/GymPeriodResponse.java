package life.hanyang.core.gym.dto;

import life.hanyang.core.gym.domain.GymPeriodType;
import life.hanyang.core.gym.domain.GymSemesterNo;
import life.hanyang.core.gym.domain.GymPeriod;
import life.hanyang.core.gym.domain.GymScheduleCell;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GymPeriodResponse(
        Long id,
        Integer year,
        GymSemesterNo semester,
        GymPeriodType periodType,
        String title,
        LocalDate start_date,
        LocalDate end_date,
        LocalTime start_time,
        LocalTime end_time,
        boolean active_weekend,
        Instant timeStamp,
        List<GymScheduleCellResponse> schedules // 👈 자식 시간표 리스트 필드 추가
) {
    // 💡 용도 1: 단순히 학기 기본 정보만 가볍게 내려줄 때 사용
    public GymPeriodResponse(GymPeriod gymPeriod) {
        this(gymPeriod, List.of());
    }

    // 💡 용도 2: 학기 정보와 해당 학기의 전체 시간표를 세트로 묶어서 내려줄 때 사용
    public GymPeriodResponse(GymPeriod gymPeriod, List<GymScheduleCell> cells) {
        this(
                gymPeriod.getId(),
                gymPeriod.getYear(),
                gymPeriod.getSemester(),
                gymPeriod.getPeriodType(),
                gymPeriod.getTitle(),
                gymPeriod.getStartDate(),
                gymPeriod.getEndDate(),
                gymPeriod.getStartTime(),
                gymPeriod.getEndTime(),
                gymPeriod.getActiveWeekend(),
                gymPeriod.getUpdatedAt(),
                cells.stream().map(GymScheduleCellResponse::from).toList() // 👈 GymScheduleCellResponse::from 작동!
        );
    }
}
