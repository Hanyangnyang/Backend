package life.hanyang.core.shuttle.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShuttleTimetableRequest {
    private String route;
    private String period;
    private String dayType;
    private String dep;
}
