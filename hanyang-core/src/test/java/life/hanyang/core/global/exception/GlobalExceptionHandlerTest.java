package life.hanyang.core.global.exception;

import life.hanyang.core.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingRequestParameterReturnsBadRequestWithoutInternalServerError() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("deviceId", "UUID");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMissingServletRequestParameterException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getError().getMessage()).contains("deviceId");
    }

    @Test
    void unreadableRequestBodyReturnsBadRequestWithoutInternalServerError() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Cannot deserialize value of type BannerPlacement from String POPUP",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleHttpMessageNotReadableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getError().getMessage()).isEqualTo("요청 본문 형식 또는 입력값이 올바르지 않습니다.");
    }

    @Test
    void disconnectedClientDoesNotCreateAnotherErrorResponse() {
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
                "ServletOutputStream failed to write: Broken pipe",
                new IOException("Broken pipe")
        );

        assertThatCode(() -> handler.handleAsyncRequestNotUsableException(exception))
                .doesNotThrowAnyException();
    }
}
