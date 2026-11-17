package life.hanyang.core.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.notice.dto.NoticeDto.UserResponse;
import life.hanyang.core.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService {
    private final NoticeRepository notices;
    private final NoticeTargetPolicy policy;
    private final ObjectMapper mapper;

    public Page<UserResponse> list(String pageKey, Map<String, String> context, int page, int size) {
        var pageable = NoticeService.pagination(page, size);
        policy.validateContext(pageKey, context);
        try {
            // Filter in PostgreSQL before LIMIT/OFFSET; never load all notices to match in memory.
            return notices.findVisible(pageKey, mapper.writeValueAsString(context), Instant.now(), pageable)
                    .map(UserResponse::new);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("공지 선택 상태를 변환할 수 없습니다.", e);
        }
    }
}
