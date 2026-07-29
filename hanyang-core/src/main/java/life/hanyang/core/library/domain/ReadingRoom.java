package life.hanyang.core.library.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ReadingRoom {
    FIRST_READING_ROOM("제1열람실", 61),
    SECOND_READING_ROOM("제2열람실", 63),
    HOLMZ("노상일 HOLMZ", 132),
    QUIET_ROOM("집중열람실", 131);

    private final String name;
    private final int externalId;

    public static ReadingRoom fromExternalId(int externalId) {
        return Arrays.stream(values())
                .filter(room -> room.externalId == externalId)
                .findFirst()
                .orElse(null);
    }
}
