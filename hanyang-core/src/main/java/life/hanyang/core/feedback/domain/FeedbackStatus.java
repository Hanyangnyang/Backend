package life.hanyang.core.feedback.domain;

public enum FeedbackStatus {
    PENDING,       // ⏳ 접수됨 (확인 전)
    IN_PROGRESS,   // 🔍 확인/처리 중
    RESOLVED,      // ✅ 해결/반영 완료
    DISMISSED      // ❌ 보류/참고
}
