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
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String currentPeriodOverride;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String periodSchedule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String customHolidays;

    private Boolean forceWeekend;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String noOperationDays;

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
