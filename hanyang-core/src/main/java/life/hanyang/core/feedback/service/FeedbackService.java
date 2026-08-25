package life.hanyang.core.feedback.service;

import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;
import life.hanyang.core.feedback.dto.FeedbackCreateRequest;
import life.hanyang.core.feedback.dto.FeedbackResponse;
import life.hanyang.core.feedback.dto.FeedbackStatusUpdateRequest;
import life.hanyang.core.feedback.repository.FeedbackRepository;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackResponse createFeedback(FeedbackCreateRequest request) {
        Feedback feedback = Feedback.builder()
                .userId(request.userId())
                .category(request.category())
                .feedbackType(request.feedbackType())
                .content(request.content())
                .targetId(request.targetId())
                .platform(request.platform())
                .appVersion(request.appVersion())
                .contact(request.contact())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("새로운 피드백 접수 - ID: {}, 카테고리: {}, 유형: {}", saved.getId(), saved.getCategory(), saved.getFeedbackType());
        return FeedbackResponse.from(saved);
    }

    public List<FeedbackResponse> getFeedbacks(
            FeedbackCategory category,
            FeedbackType feedbackType,
            FeedbackStatus status
    ) {
        return feedbackRepository.searchFeedbacks(category, feedbackType, status)
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    public FeedbackResponse getFeedback(UUID id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 피드백을 찾을 수 없습니다. id: " + id, ErrorCode.ENTITY_NOT_FOUND));
        return FeedbackResponse.from(feedback);
    }

    @Transactional
    public FeedbackResponse updateStatus(UUID id, FeedbackStatusUpdateRequest request) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 피드백을 찾을 수 없습니다. id: " + id, ErrorCode.ENTITY_NOT_FOUND));

        feedback.updateStatus(request.status(), request.adminMemo());
        return FeedbackResponse.from(feedback);
    }

    @Transactional
    public void deleteFeedback(UUID id) {
        if (!feedbackRepository.existsById(id)) {
            throw new BusinessException("해당 피드백을 찾을 수 없습니다. id: " + id, ErrorCode.ENTITY_NOT_FOUND);
        }
        feedbackRepository.deleteById(id);
    }
}
