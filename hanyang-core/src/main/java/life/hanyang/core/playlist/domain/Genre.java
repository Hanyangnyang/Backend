package life.hanyang.core.playlist.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Genre {
    KPOP("K-POP"),
    BAND("밴드"),
    ROCK("락"),
    R_AND_B("R&B"),
    HIPHOP("힙합"),
    INDIE("인디"),
    BALLAD("발라드"),
    POP("POP"),
    JPOP("J-POP"),
    OTHER("기타");

    private final String description;
}
