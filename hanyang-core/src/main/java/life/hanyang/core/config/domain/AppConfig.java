package life.hanyang.core.config.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "app_config")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "current_period_override", columnDefinition = "TEXT")
    private String currentPeriodOverride;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "period_schedule", columnDefinition = "jsonb")
    private String periodSchedule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_holidays", columnDefinition = "jsonb")
    private String customHolidays;

    @Column(name = "force_weekend")
    private Boolean forceWeekend;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "no_operation_days", columnDefinition = "jsonb")
    private String noOperationDays;

    @Column(name = "force_no_operation")
    private Boolean forceNoOperation;

    @Builder
    public AppConfig(String currentPeriodOverride, String periodSchedule, String customHolidays,
                     Boolean forceWeekend, String noOperationDays, Boolean forceNoOperation) {
        this.currentPeriodOverride = currentPeriodOverride;
        this.periodSchedule = periodSchedule;
        this.customHolidays = customHolidays;
        this.forceWeekend = forceWeekend;
        this.noOperationDays = noOperationDays;
        this.forceNoOperation = forceNoOperation;
    }
}
