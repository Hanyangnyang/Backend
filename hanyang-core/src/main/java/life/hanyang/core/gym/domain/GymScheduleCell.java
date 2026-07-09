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
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "gym_schedule_cell")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GymScheduleCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private GymPeriod gymPeriod;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "day_of_week", nullable = false)
    private GymDayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Builder
    public GymScheduleCell(GymPeriod gymPeriod, GymDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer classId, String className) {
        this.gymPeriod = gymPeriod;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.classId = classId;
        this.className = className;
    }

    public void updateSchedule(GymDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer classId, String className) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.classId = classId;
        this.className = className;
    }
}