package life.hanyang.user.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.notice.dto.NoticeDto.UserResponse;
import life.hanyang.core.notice.service.NoticeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Tag(name = "공지 API")
public class NoticeController {
    private final NoticeQueryService notices;

    @GetMapping
    @Operation(summary = "현재 화면에 해당하는 게시 중 공지를 최신순으로 조회합니다.",
            description = "pageKey는 menu/transport/campusMap입니다. 선택 상태는 context[조건명]=값으로 전달합니다. "
                    + "조건 목록은 관리자 target-options에서 확인하며, page는 0부터 시작하고 size는 1~50입니다.")
    public ApiResponse<Page<UserResponse>> list(@RequestParam String pageKey,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam MultiValueMap<String, String> parameters) {
        Map<String, String> context = new HashMap<>();
        parameters.forEach((key, values) -> {
            if (values.size() != 1) throw new IllegalArgumentException("중복된 조회 파라미터입니다: " + key);
            if (Set.of("pageKey", "page", "size").contains(key)) return;
            if (!key.startsWith("context[") || !key.endsWith("]")) {
                throw new IllegalArgumentException("지원하지 않는 조회 파라미터입니다: " + key);
            }
            context.put(key.substring(8, key.length() - 1), values.get(0));
        });
        return ApiResponse.success(notices.list(pageKey, context, page, size));
    }
}
