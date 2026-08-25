package life.hanyang.core.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "feedbacks", indexes = {
        @Index(name = "idx_feedback_category", columnList = "category"),
        @Index(name = "idx_feedback_status", columnList = "status"),
        @Index(name = "idx_feedback_created_at", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    @ColumnDefault("'GENERAL'")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FeedbackCategory category;

    @ColumnDefault("'GENERAL_OPINION'")
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private FeedbackType feedbackType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "contact", length = 100)
    private String contact;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Feedback(
            UUID userId,
            FeedbackCategory category,
            FeedbackType feedbackType,
            String content,
            String targetId,
            String platform,
            String appVersion,
            String contact
    ) {
        this.userId = userId;
        this.category = category != null ? category : FeedbackCategory.GENERAL;
        this.feedbackType = feedbackType != null ? feedbackType : FeedbackType.GENERAL_OPINION;
        this.content = content;
        this.targetId = targetId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.contact = contact;
        this.status = FeedbackStatus.PENDING;
    }

    public void updateStatus(FeedbackStatus status, String adminMemo) {
        if (status != null) {
            this.status = status;
        }
        if (adminMemo != null) {
            this.adminMemo = adminMemo;
        }
    }
}
