package life.hanyang.core.printer.service;

import jakarta.persistence.OptimisticLockException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.sql.SQLException;

@Slf4j
@Order(0)
@RestControllerAdvice(basePackages = {"life.hanyang.user.printer", "life.hanyang.admin.printer"})
public class PrinterExceptionHandler {
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(Exception exception) {
        return ResponseEntity.status(409).body(ApiResponse.fail(ErrorCode.PRINTER_CONFLICT.getCode(),
                "다른 요청에서 정보를 수정했습니다. 최신 정보를 확인해주세요."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException exception) {
        String state = sqlState(exception);
        if ("23505".equals(state)) {
            return ResponseEntity.status(409).body(ApiResponse.fail(ErrorCode.DUPLICATE_RESOURCE.getCode(),
                    "이미 등록된 정보입니다."));
        }
        if ("23503".equals(state)) {
            return ResponseEntity.status(409).body(ApiResponse.fail(ErrorCode.PRINTER_CONFLICT.getCode(),
                    "참조 정보가 변경되었습니다. 최신 정보를 확인해주세요."));
        }
        if ("23502".equals(state) || "23514".equals(state) || (state != null && state.startsWith("22"))) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(),
                    "필수값 또는 입력값의 범위를 확인해주세요."));
        }
        // Database exception messages may contain submitted data. Log metadata, not SQL/bind values.
        log.error("Unexpected printer database integrity error: sqlState={}, causeType={}",
                state, exception.getMostSpecificCause().getClass().getName());
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
