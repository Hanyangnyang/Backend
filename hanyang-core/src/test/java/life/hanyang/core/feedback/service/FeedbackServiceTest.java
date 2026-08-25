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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("다양한 카테고리의 피드백을 정상적으로 생성할 수 있다")
    void createFeedback_Success() {
        // given
        UUID userId = UUID.randomUUID();
        FeedbackCreateRequest request = new FeedbackCreateRequest(
                userId,
                FeedbackCategory.SHUTTLE,
                FeedbackType.INACCURACY,
                "순환 셔틀 12:30 출발이 늦습니다.",
                "CIRCULAR",
                "IOS",
                "1.2.0",
                "student@hanyang.ac.kr"
        );

        Feedback saved = Feedback.builder()
                .userId(userId)
                .category(request.category())
                .feedbackType(request.feedbackType())
                .content(request.content())
                .targetId(request.targetId())
                .platform(request.platform())
                .appVersion(request.appVersion())
                .contact(request.contact())
                .build();

        given(feedbackRepository.save(any(Feedback.class))).willReturn(saved);

        // when
        FeedbackResponse response = feedbackService.createFeedback(request);

        // then
        assertThat(response.category()).isEqualTo(FeedbackCategory.SHUTTLE);
        assertThat(response.feedbackType()).isEqualTo(FeedbackType.INACCURACY);
        assertThat(response.content()).isEqualTo("순환 셔틀 12:30 출발이 늦습니다.");
        assertThat(response.status()).isEqualTo(FeedbackStatus.PENDING);
    }

    @Test
    @DisplayName("피드백 목록을 필터링하여 조회할 수 있다")
    void getFeedbacks_Filtered() {
        // given
        Feedback feedback = Feedback.builder()
                .category(FeedbackCategory.PLAYLIST)
                .feedbackType(FeedbackType.FEATURE_REQUEST)
                .content("신청곡 추가해주세요")
                .build();

        given(feedbackRepository.searchFeedbacks(FeedbackCategory.PLAYLIST, FeedbackType.FEATURE_REQUEST, FeedbackStatus.PENDING))
                .willReturn(List.of(feedback));

        // when
        List<FeedbackResponse> result = feedbackService.getFeedbacks(
                FeedbackCategory.PLAYLIST,
                FeedbackType.FEATURE_REQUEST,
                FeedbackStatus.PENDING
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(FeedbackCategory.PLAYLIST);
        assertThat(result.get(0).feedbackType()).isEqualTo(FeedbackType.FEATURE_REQUEST);
    }

    @Test
    @DisplayName("피드백 상태 및 관리자 메모를 정상 수정할 수 있다")
    void updateStatus_Success() {
        // given
        UUID id = UUID.randomUUID();
        Feedback feedback = Feedback.builder()
                .category(FeedbackCategory.CAMPUS_MAP)
                .feedbackType(FeedbackType.BUG_REPORT)
                .content("건물 위치가 잘못 표시됩니다")
                .build();

        given(feedbackRepository.findById(id)).willReturn(Optional.of(feedback));

        FeedbackStatusUpdateRequest request = new FeedbackStatusUpdateRequest(
                FeedbackStatus.RESOLVED,
                "지도 좌표 수정 완료"
        );

        // when
        FeedbackResponse response = feedbackService.updateStatus(id, request);

        // then
        assertThat(response.status()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(response.adminMemo()).isEqualTo("지도 좌표 수정 완료");
    }

    @Test
    @DisplayName("존재하지 않는 피드백 조회 시 예외가 발생한다")
    void getFeedback_NotFound() {
        // given
        UUID id = UUID.randomUUID();
        given(feedbackRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedbackService.getFeedback(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("피드백을 정상 삭제할 수 있다")
    void deleteFeedback_Success() {
        // given
        UUID id = UUID.randomUUID();
        given(feedbackRepository.existsById(id)).willReturn(true);

        // when
        feedbackService.deleteFeedback(id);

        // then
        verify(feedbackRepository).deleteById(id);
    }
}
