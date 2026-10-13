package life.hanyang.core.auth;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.mockito.Mockito.*;

class SupabaseUserResolverTest {
    private RestClient.Builder mockTransport(RestClient.Builder builder) {
        var wrapper = mock(RestClient.Builder.class);
        when(wrapper.clone()).thenReturn(wrapper);
        when(wrapper.requestFactory(any())).thenReturn(builder);
        return wrapper;
    }
    @Test void verifiesBearerOnAuthServer() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var resolver = new SupabaseUserResolver(mockTransport(builder), "https://test.supabase.co/", "test-key");
        UUID id = UUID.randomUUID();
        server.expect(requestTo("https://test.supabase.co/auth/v1/user"))
                .andExpect(header("apikey", "test-key"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("{\"id\":\"" + id + "\"}", MediaType.APPLICATION_JSON));
        assertThat(resolver.required("Bearer test-token")).isEqualTo(id);
        server.verify();
    }

    @Test void missingTokenCannotBeReplacedWithDeviceId() {
        var resolver = new SupabaseUserResolver(RestClient.builder(), "", "");
        assertThat(resolver.optional(null)).isNull();
        assertThatThrownBy(() -> resolver.required(null)).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test void invalidTokenAndAuthOutageAreDistinct() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var resolver = new SupabaseUserResolver(mockTransport(builder), "https://test.supabase.co", "test-key");
        server.expect(requestTo("https://test.supabase.co/auth/v1/user")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://test.supabase.co/auth/v1/user")).andRespond(withServerError());
        assertThatThrownBy(() -> resolver.required("Bearer bad")).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> resolver.required("Bearer expired")).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PLAYLIST_AUTH_UNAVAILABLE);
        server.verify();
    }
}
