package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.llm.gemini.GeminiApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaylistModerationServiceTest {

    @Mock
    private GeminiApiClient geminiApiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PlaylistModerationService playlistModerationService;

    @Test
    @DisplayName("코멘트가 비어있으면 검열을 즉시 통과한다")
    void validateSongContent_PassesImmediately_WhenCommentEmpty() {
        boolean result = playlistModerationService.validateSongContent("Ditto", "NewJeans", "");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상적인 코멘트는 AI 검열을 통과하고 true를 반환한다")
    void validateSongContent_Passes_WhenClean() {
        // given
        String geminiResponse = "{\"inappropriate\": false, \"reason\": \"\"}";
        given(geminiApiClient.generateContent(anyString())).willReturn(geminiResponse);

        // when
        boolean result = playlistModerationService.validateSongContent("Ditto", "NewJeans", "과제할 때 들으면 좋아요");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("부적절한 표현 감지 시 BusinessException 예외가 발생한다")
    void validateSongContent_ThrowsException_WhenInappropriate() {
        // given
        String geminiResponse = "{\"inappropriate\": true, \"reason\": \"비속어 및 저격성 발언 포함\"}";
        given(geminiApiClient.generateContent(anyString())).willReturn(geminiResponse);

        // when & then
        assertThatThrownBy(() -> playlistModerationService.validateSongContent("노래", "가수", "부적절한 코멘트"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("비속어 및 저격성 발언 포함");
    }

    @Test
    @DisplayName("Gemini API 장애 발생 시 Fail-Open으로 예외 없이 false를 반환한다")
    void validateSongContent_FailsOpen_WhenGeminiErrors() {
        // given
        given(geminiApiClient.generateContent(anyString())).willThrow(new RuntimeException("Gemini Timeout"));

        // when
        boolean result = playlistModerationService.validateSongContent("Ditto", "NewJeans", "코멘트");

        // then
        assertThat(result).isFalse();
    }
}
