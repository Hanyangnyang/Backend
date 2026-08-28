package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.llm.gemini.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistModerationService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    private static final String MODERATION_PROMPT = """
            당신은 대한민국 대학생 커뮤니티의 최고 수준 유해 콘텐츠 검열관입니다.
            사용자가 등록하려는 음악 추천 정보를 다각도로 정밀 분석하여, 아래 유해 유형 및 '교묘한 우회 수법'이 포함되어 있는지 판별하세요.

            [검사 대상 정보]
            - 곡 제목: "%s"
            - 가수명: "%s"
            - 작성 코멘트: "%s"

            [중점 검열 대상 및 우회 패턴]
            1. 비속어, 욕설, 성희롱, 음란성, 특정인 비방/저격, 혐오/차별 표현
            2. 특정 인물(정치인, 고인 등) 비하, 조롱, 밈(meme)
            3. 🎵 [이어읽기 우회]: '곡 제목' 또는 '가수명'과 '코멘트'를 연결하여 읽었을 때 비하/욕설이 되는 경우
            4. 🪜 [세로드립/대각선 우회]: 코멘트의 줄바꿈 첫 글자(앞글자) 또는 특정 규칙으로 읽었을 때 은밀한 비하/욕설이 되는 경우
            5. 🔤 [초성/자모 분리/은어 우회]: 자음(초성), 숫자/특수문자 변형으로 유해 표현을 숨긴 경우

            반드시 다음 JSON 단 하나로만 응답하세요:
            {"inappropriate": true 또는 false, "reason": "부적절한 구체적 사유 (정상인 경우 빈 문자열)"}
            """;

    /**
     * 곡 정보 및 코멘트 유해성 다각도 검열
     *
     * @return boolean 검열 정상 통과 여부 (정상 통과: true, API 오류로 인한 Fail-Open 통과: false)
     */
    public boolean validateSongContent(String title, String artist, String comment) {
        if (comment == null || comment.isBlank()) {
            return true; // 코멘트 미작성 시 검열 통과
        }

        try {
            String prompt = String.format(MODERATION_PROMPT, escape(title), escape(artist), escape(comment));
            String responseText = geminiApiClient.generateContent(prompt);

            JsonNode root = objectMapper.readTree(extractJson(responseText));
            boolean inappropriate = root.path("inappropriate").asBoolean(false);

            if (inappropriate) {
                String reason = root.path("reason").asText("부적절한 표현이 감지되었습니다.");
                log.warn("[PlaylistModeration] 🚨 유해 코멘트 차단 감지 - title: '{}', artist: '{}', comment: '{}', reason: '{}'",
                        title, artist, comment, reason);
                throw new BusinessException(reason, ErrorCode.PLAYLIST_INAPPROPRIATE_COMMENT);
            }

            log.debug("[PlaylistModeration] ✅ 코멘트 검열 통과 - title: '{}', artist: '{}'", title, artist);
            return true;
        } catch (BusinessException e) {
            throw e; // 차단 예외는 그대로 클라이언트에게 400 Bad Request 전달
        } catch (Exception e) {
            // 💡 Fail-Open: 구글 API 장애/타임아웃 시 로그만 남기고 정상 등록 진행 (isAiModerated=false로 추적)
            log.warn("[PlaylistModeration] ⚠️ Gemini API 검열 오류 발생 (Fail-Open 자동 통과) - title: '{}', artist: '{}', comment: '{}', error: {}",
                    title, artist, comment, e.getMessage());
            return false;
        }
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", " ");
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && start < end) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
