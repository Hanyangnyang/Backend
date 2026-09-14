package life.hanyang.core.global.exception;

import life.hanyang.core.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

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
}
