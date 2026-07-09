package life.hanyang.core.gym.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class GymScheduleCreateRequest {
    private Long periodId;
    private List<GymScheduleCellDto> schedules;
}
