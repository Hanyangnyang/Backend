package life.hanyang.core.gym.dto;

import life.hanyang.core.gym.domain.GymDayOfWeek;
import life.hanyang.core.gym.domain.GymScheduleCell;
import java.time.LocalTime;

public record GymScheduleCellResponse (
    Long id, // 소문자 id로 수정하여 자바 명명 규칙 준수
    GymDayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Integer classId,
    String className
) {
    public static GymScheduleCellResponse from(GymScheduleCell gymScheduleCell) {
        return new GymScheduleCellResponse(
                gymScheduleCell.getId(),
                gymScheduleCell.getDayOfWeek(),
                gymScheduleCell.getStartTime(),
                gymScheduleCell.getEndTime(),
                gymScheduleCell.getClassId(),
                gymScheduleCell.getClassName()
        );
    }
}
