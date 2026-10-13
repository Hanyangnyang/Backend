package life.hanyang.user.playlist.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.auth.SupabaseUserResolver;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.service.PlaylistCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playlist/songs/{songId}/comments")
@Tag(name = "플레이리스트 댓글 API")
public class PlaylistCommentController {
    private final PlaylistCommentService comments;
    private final SupabaseUserResolver users;

    @GetMapping
    @Operation(summary = "추천글 댓글 조회", description = "작성 시간 오름차순. 인증 토큰이 있으면 isMine을 계산하며, 없으면 false입니다. 글쓴이는 POST_AUTHOR, 나머지는 ANONYMOUS와 고정 anonymousNumber로 구분합니다.")
    public ApiResponse<Page<PlaylistCommentResponse>> list(@PathVariable UUID songId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ApiResponse.success(comments.list(songId, users.optional(authorization),
                PageRequest.of(page, size, Sort.by("createdAt", "id"))));
    }

    @PostMapping
    @Operation(summary = "댓글 작성", description = "Supabase Bearer 토큰 필수. 내용은 1~200자, 기기당 1분에 최대 5개. 본인 추천글의 댓글은 알림을 보내지 않습니다.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaylistCommentResponse> create(@PathVariable UUID songId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody PlaylistCommentRequest request) {
        return ApiResponse.success(comments.create(songId, users.required(authorization), request));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "본인 댓글 삭제", description = "Supabase Bearer 토큰 필수. 익명 번호 배정 이력은 보존하며 미발송 알림은 취소합니다.")
    public ApiResponse<Void> delete(@PathVariable UUID songId, @PathVariable UUID commentId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        comments.delete(songId, commentId, users.required(authorization));
        return ApiResponse.success(null);
    }
}
