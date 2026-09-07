package life.hanyang.core.feedback.service;

import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class FeedbackWebhookNotifier {

    private final RestClient restClient;
    private final String webhookUrl;

    public FeedbackWebhookNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${feedback.webhook.url:}") String webhookUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
    }

    public void notify(Feedback feedback) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("피드백 웹훅 URL이 설정되지 않아 알림을 건너뜁니다. feedbackId={}", feedback.getId());
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of("content", formatMessage(feedback)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("피드백 웹훅 알림 전송 완료. feedbackId={}", feedback.getId());
        } catch (Exception e) {
            log.error("피드백 웹훅 알림 전송 실패. feedbackId={}, cause={}, message={}",
                    feedback.getId(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String formatMessage(Feedback feedback) {
        return "📮 새로운 피드백이 들어왔어요! 💌\n"
                + "ID: " + feedback.getId() + "\n"
                + "카테고리: " + categoryLabel(feedback.getCategory()) + " (" + feedback.getCategory() + ")\n"
                + "유형: " + typeLabel(feedback.getFeedbackType()) + " (" + feedback.getFeedbackType() + ")\n"
                + "내용: " + feedback.getContent();
    }

    private String categoryLabel(FeedbackCategory category) {
        return switch (category) {
            case SHUTTLE -> "셔틀버스";
            case CITY_BUS -> "일반 시내버스";
            case SUBWAY -> "지하철";
            case CAMPUS_MAP -> "캠퍼스맵";
            case MENU -> "학식 메뉴";
            case GYM -> "헬스장";
            case LIBRARY -> "도서관 좌석";
            case PLAYLIST -> "플레이리스트";
            case WEATHER -> "날씨 & 미세먼지";
            case PARTNERSHIP -> "제휴/가맹점";
            case BANNER -> "배너/이벤트";
            case CLUB -> "동아리";
            case GENERAL -> "앱 전반 / 기타";
        };
    }

    private String typeLabel(FeedbackType type) {
        return switch (type) {
            case BUG_REPORT -> "오류/버그 제보";
            case INACCURACY -> "정보 수정 요청";
            case FEATURE_REQUEST -> "새로운 기능 제안";
            case INQUIRY -> "이용 문의 / 질문";
            case GENERAL_OPINION -> "일반 의견 / 응원 / 기타";
        };
    }
}
