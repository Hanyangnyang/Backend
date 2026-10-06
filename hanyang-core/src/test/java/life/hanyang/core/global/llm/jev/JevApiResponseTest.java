package life.hanyang.core.global.llm.jev;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JevApiResponseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsOfficialResponseShape() throws Exception {
        JevApiResponse response = mapper.readValue("""
                {"answers":{"registration_allowed":{"type":"noul","noul":0.51}}}
                """, JevApiResponse.class);
        assertThat(response.registrationAllowedProbability()).isEqualTo(0.51);
    }

    @Test
    void rejectsMalformedAnswers() throws Exception {
        for (String json : new String[]{"{}", "{\"answers\":{}}",
                "{\"answers\":{\"registration_allowed\":{\"type\":\"noul\"}}}",
                "{\"answers\":{\"registration_allowed\":{\"type\":\"noul\",\"noul\":1.1}}}",
                "{\"answers\":{\"registration_allowed\":{\"type\":\"choice\",\"noul\":0.99}}}"}) {
            JevApiResponse response = mapper.readValue(json, JevApiResponse.class);
            assertThatThrownBy(response::registrationAllowedProbability)
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
