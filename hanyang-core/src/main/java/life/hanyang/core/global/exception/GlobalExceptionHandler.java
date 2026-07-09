package life.hanyang.core.global.exception;

import life.hanyang.core.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 유효성 검증 실패(IllegalArgumentException) 발생 시 처리
     * 예: 시간 중복, 운영시간 초과 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad Request Exception: ", e);
        ApiResponse<Void> response = ApiResponse.fail("BAD_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 파일 업로드 용량 제한(5MB) 초과 시 처리
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("File Size Limit Exceeded: ", e);
        ApiResponse<Void> response = ApiResponse.fail("FILE_SIZE_LIMIT_EXCEEDED", "업로드 가능한 최대 파일 용량(5MB)을 초과했습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 그 외 서버 내부에서 처리되지 않은 예상치 못한 예외(Exception) 발생 시 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 서버 콘솔에 상세한 에러 로그(StackTrace)를 강제로 남깁니다.
        log.error("Internal Server Error: ", e);
        
        // 보안을 위해 실제 에러 메시지(e.getMessage())를 노출하지 않고 공통 메시지를 보냅니다.
        ApiResponse<Void> response = ApiResponse.fail("INTERNAL_SERVER_ERROR", "서버 내부 에러가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
