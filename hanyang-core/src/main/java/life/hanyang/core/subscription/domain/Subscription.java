package life.hanyang.core.subscription.domain;

import jakarta.persistence.*;
import life.hanyang.core.device.domain.Device;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String params;

    @Column(nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    public Subscription(Device device, String topic, String params, boolean isActive) {
        this.device = device;
        this.topic = topic;
        this.params = params;
        this.isActive = isActive;
    }
}
