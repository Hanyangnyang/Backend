package life.hanyang.core.campusmap.service;

import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.sql.SQLException;

@Slf4j
@Order(0)
@RestControllerAdvice(basePackages = {"life.hanyang.admin.campusmap", "life.hanyang.user.campusmap"})
public class CampusMapExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingFile() {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(),
                "file 필드에 JSON 파일을 업로드해주세요."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrityViolation(DataIntegrityViolationException exception) {
        String state = sqlState(exception);
        if ("23505".equals(state)) {
            return ResponseEntity.status(ErrorCode.DUPLICATE_RESOURCE.getStatus())
                    .body(ApiResponse.fail(ErrorCode.DUPLICATE_RESOURCE.getCode(),
                            "동일한 ID 또는 캠퍼스·건물 번호가 이미 등록되어 있습니다."));
        }
        if ("23502".equals(state) || "23503".equals(state) || "23514".equals(state)
                || (state != null && state.startsWith("22"))) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(),
                    "필수값, 참조 대상 또는 입력값의 범위를 확인해주세요."));
        }
        log.error("Unexpected campus map database integrity error", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    private static String sqlState(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                for (SQLException next = sql; next != null; next = next.getNextException()) {
                    if (next.getSQLState() != null) return next.getSQLState();
                }
            }
        }
        return null;
    }
}
