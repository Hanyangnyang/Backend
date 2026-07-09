package life.hanyang.core.gym.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "gym_period",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gym_period_year_semester_type",
                        columnNames = {"year", "semester", "period_type"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GymPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "semester", nullable = false)
    private GymSemesterNo semester;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "period_type", nullable = false)
    private GymPeriodType periodType;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ColumnDefault("false")
    @Column(name = "active_weekend", nullable = false)
    private Boolean activeWeekend = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Builder
    public GymPeriod(Integer year, GymSemesterNo semester, GymPeriodType periodType, String title, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, Boolean activeWeekend) {
        this.year = year;
        this.semester = semester;
        this.periodType = periodType;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activeWeekend = activeWeekend;
    }
}