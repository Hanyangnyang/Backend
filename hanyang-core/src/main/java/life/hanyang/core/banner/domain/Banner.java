package life.hanyang.core.banner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Entity
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", nullable = false, length = 20)
    private BannerPlacement placement;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "click_url", columnDefinition = "TEXT")
    private String clickUrl;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Banner(String imageUrl, BannerPlacement placement, String altText, String clickUrl, Integer displayOrder, Boolean isActive) {
        this.imageUrl = imageUrl;
        this.placement = placement == null ? BannerPlacement.BANNER : placement;
        this.altText = altText;
        this.clickUrl = clickUrl;
        this.displayOrder = displayOrder;
        this.isActive = isActive == null || isActive;
    }

    public void update(BannerPlacement placement, String altText, String clickUrl, Integer displayOrder, Boolean isActive) {
        if (placement != null) {
            this.placement = placement;
        }
        if (altText != null) {
            this.altText = altText;
        }
        if (clickUrl != null) {
            this.clickUrl = clickUrl;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void changeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("배너 이미지 URL은 비어 있을 수 없습니다.");
        }
        this.imageUrl = imageUrl;
    }
}
