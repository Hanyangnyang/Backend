package life.hanyang.core.gym.dto;

import life.hanyang.core.gym.domain.GymDayOfWeek;
import java.time.LocalTime;

public record GymScheduleUpdateDto(
    Long id, // 수정할 대상 시간표 셀 ID
    GymDayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Integer classId,
    String className
) {}
