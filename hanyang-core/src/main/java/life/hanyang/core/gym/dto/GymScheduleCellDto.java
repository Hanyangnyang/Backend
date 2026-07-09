package life.hanyang.core.gym.dto;

import life.hanyang.core.gym.domain.GymDayOfWeek;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class GymScheduleCellDto {
    private GymDayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer classId;
    private String className;
}
