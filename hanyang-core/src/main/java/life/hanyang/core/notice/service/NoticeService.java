package life.hanyang.core.notice.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.notice.domain.*;
import life.hanyang.core.notice.dto.NoticeDto.*;
import life.hanyang.core.notice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository notices;
    private final NoticeTargetRepository targets;
    private final NoticeTargetPolicy policy;
    private final Validator validator;

    @Transactional
    public AdminResponse create(Input input) {
        validate(input);
        Notice notice = notices.saveAndFlush(new Notice(input.content(), input.enabled(), input.publishedAt(), input.expiresAt()));
        return new AdminResponse(notice, saveTargets(notice, input.targets()));
    }

    @Transactional
    public AdminResponse update(Long id, Input input) {
        // All mutations lock the same parent before touching targets; concurrent replacement/deletion is serialized.
        Notice notice = findForUpdate(id);
        validate(input);
        notice.update(input.content(), input.enabled(), input.publishedAt(), input.expiresAt());
        targets.deleteForNotice(id);
        List<TargetResponse> savedTargets = saveTargets(notice, input.targets());
        notices.flush();
        return new AdminResponse(notice, savedTargets);
    }

    @Transactional
    public void delete(Long id) {
        findForUpdate(id);
        notices.deleteNotice(id);
    }

    public AdminResponse get(Long id) {
        Notice notice = notices.findById(id).orElseThrow(() -> notFound(id));
        return new AdminResponse(notice, targets.findForNotices(List.of(id)).stream().map(TargetResponse::new).toList());
    }

    public Page<AdminResponse> listAdmin(String pageKey, Boolean enabled, int page, int size) {
        if (pageKey != null) policy.validatePage(pageKey);
        Page<Notice> result = notices.findForAdmin(pageKey, enabled, pagination(page, size));
        if (result.isEmpty()) return result.map(n -> new AdminResponse(n, List.of()));
        List<Long> ids = result.stream().map(Notice::getId).toList();
        Map<Long, List<TargetResponse>> grouped = targets.findForNotices(ids).stream().collect(Collectors.groupingBy(
                t -> t.getNotice().getId(), Collectors.mapping(TargetResponse::new, Collectors.toList())));
        return result.map(n -> new AdminResponse(n, grouped.getOrDefault(n.getId(), List.of())));
    }

    public List<PageOption> options() { return policy.options(); }

    public static Pageable pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page는 0 이상, size는 1~50이어야 하며 조회 범위를 초과할 수 없습니다.");
        }
        return PageRequest.of(page, size);
    }

    private void validate(Input input) {
        if (input == null) throw new IllegalArgumentException("공지 입력은 필수입니다.");
        var violations = validator.validate(input);
        if (!violations.isEmpty()) throw new IllegalArgumentException(violations.stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.joining(" ")));
        policy.validateTargets(input.targets());
    }

    private List<TargetResponse> saveTargets(Notice notice, List<TargetInput> inputs) {
        return targets.saveAllAndFlush(inputs.stream()
                .map(t -> new NoticeTarget(notice, t.page(), t.conditions())).toList())
                .stream().map(TargetResponse::new).toList();
    }

    private Notice findForUpdate(Long id) {
        return notices.findForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private EntityNotFoundException notFound(Long id) {
        return new EntityNotFoundException("해당 공지가 존재하지 않습니다. id: " + id);
    }
}
