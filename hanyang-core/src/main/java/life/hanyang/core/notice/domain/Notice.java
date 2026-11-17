package life.hanyang.core.notice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Getter
@Entity
@Table(name = "notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Notice(String content, boolean enabled, Instant publishedAt, Instant expiresAt) {
        update(content, enabled, publishedAt, expiresAt);
    }

    public void update(String content, boolean enabled, Instant publishedAt, Instant expiresAt) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("공지 내용은 필수입니다.");
        if (publishedAt == null) throw new IllegalArgumentException("게시 시각은 필수입니다.");
        if (expiresAt != null && !expiresAt.isAfter(publishedAt)) {
            throw new IllegalArgumentException("종료 시각은 게시 시각보다 이후여야 합니다.");
        }
        this.content = content.strip();
        this.enabled = enabled;
        this.publishedAt = publishedAt;
        this.expiresAt = expiresAt;
    }
}
