package life.hanyang.core.partnership.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "partnership")
@NoArgsConstructor
public class Partnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partnership_id")
    private Long partnershipId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Department department;

    @Column(name = "benefit", nullable = false, columnDefinition = "TEXT")
    private String benefit;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "photo_order")
    private Integer photoOrder;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Builder
    public Partnership(Merchant merchant, Department department, String benefit, String conditions, String sourceUrl, Integer photoOrder, LocalDate startDate, LocalDate endDate, Boolean isActive) {
        this.merchant = merchant;
        this.department = department;
        this.benefit = benefit;
        this.conditions = conditions;
        this.sourceUrl = sourceUrl;
        this.photoOrder = photoOrder;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    public void update(Department department, String benefit, String conditions, String sourceUrl, Integer photoOrder, LocalDate startDate, LocalDate endDate, Boolean isActive) {
        if (department != null) {
            this.department = department;
        }
        if (benefit != null && !benefit.isBlank()) {
            this.benefit = benefit;
        }
        if (conditions != null && !conditions.isBlank()) {
            this.conditions = conditions;
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            this.sourceUrl = sourceUrl;
        }
        if (photoOrder != null) {
            this.photoOrder = photoOrder;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void changeMerchant(Merchant newMerchant) {
        if (this.merchant != null) {
            this.merchant.getPartnerships().remove(this);
        }
        this.merchant = newMerchant;
        if (newMerchant != null) {
            newMerchant.getPartnerships().add(this);
        }
    }
}
