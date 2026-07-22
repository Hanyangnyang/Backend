package life.hanyang.core.device.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "devices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fcmToken;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public Device(String fcmToken, String platform, LocalDateTime lastActiveAt) {
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.lastActiveAt = lastActiveAt;
    }
}
