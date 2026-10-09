package life.hanyang.core.global.llm.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiApiResponseTest {
    @Test
    void readsUsageWithoutChangingGeneratedText() throws Exception {
        GeminiApiResponse response = new ObjectMapper().readValue("""
                {"candidates":[{"content":{"parts":[{"text":" ok "}]}}],
                 "usageMetadata":{"promptTokenCount":100,"candidatesTokenCount":10,
                   "thoughtsTokenCount":5,"cachedContentTokenCount":20,"totalTokenCount":115}}
                """, GeminiApiResponse.class);
        assertThat(response.getGeneratedText()).isEqualTo("ok");
        assertThat(response.usageMetadata().promptTokenCount()).isEqualTo(100);
        assertThat(response.usageMetadata().totalTokenCount()).isEqualTo(115);
    }

    @Test
    void acceptsResponseWithoutUsage() throws Exception {
        GeminiApiResponse response = new ObjectMapper().readValue("""
                {"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}
                """, GeminiApiResponse.class);
        assertThat(response.getGeneratedText()).isEqualTo("ok");
        assertThat(response.usageMetadata()).isNull();
    }
}
