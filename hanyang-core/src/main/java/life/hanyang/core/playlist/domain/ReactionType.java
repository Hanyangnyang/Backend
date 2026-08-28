package life.hanyang.core.playlist.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {

    // [1] 얼굴/표정 이모지 (4종)
    LOVE("😍", "반함"),
    EMOTIONAL("🥹", "감동"),
    BITTERSWEET("🥲", "아련"),
    COOL("😎", "힙함"),

    // [2] 텐션/액션 이모지 (4종)
    FIRE("🔥", "불꽃"),
    ROCK("🤘", "락"),
    DANCE("🕺", "댄스"),
    THUMBS_UP("👍", "최고"),

    // [3] 라이프/무드 이모지 (1종)
    BEER("🍻", "맥주");

    private final String emoji;
    private final String description;
}
