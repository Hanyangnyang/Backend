package life.hanyang.core.weather.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "weather_briefing",
        indexes = {
                @Index(name = "idx_location_forecast_at", columnList = "location, forecast_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String location;

    @Column(name = "briefing_text", nullable = false, columnDefinition = "TEXT")
    private String briefingText;

    @Column(name = "forecast_at", nullable = false)
    private LocalDateTime forecastAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public WeatherBriefing(String location, String briefingText, LocalDateTime forecastAt) {
        this.location = location;
        this.briefingText = briefingText;
        this.forecastAt = forecastAt;
    }
}
