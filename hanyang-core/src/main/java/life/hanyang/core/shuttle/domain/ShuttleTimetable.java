package life.hanyang.core.shuttle.domain;

import jakarta.persistence.*;
import jakarta.persistence.Table;
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
@Table(
    name = "shuttle_timetable",
    indexes = @Index(name = "idx_shuttle_timetable_period_day_type_route_departure_time", columnList = "period, day_type, route, departure_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShuttleTimetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "route", nullable = false)
    private ShuttleRoute shuttleRoute;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "period", nullable = false)
    private ShuttlePeriod shuttlePeriod;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "day_type", nullable = false)
    private ShuttleDayType shuttleDayType;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Builder
    public ShuttleTimetable(ShuttleRoute shuttleRoute, ShuttlePeriod shuttlePeriod, ShuttleDayType shuttleDayType, LocalTime departureTime) {
        this.shuttleRoute = shuttleRoute;
        this.shuttlePeriod = shuttlePeriod;
        this.shuttleDayType = shuttleDayType;
        this.departureTime = departureTime;
    }

    public void update(ShuttleRoute shuttleRoute, ShuttlePeriod shuttlePeriod, ShuttleDayType shuttleDayType, LocalTime departureTime) {
        this.shuttleRoute = shuttleRoute;
        this.shuttlePeriod = shuttlePeriod;
        this.shuttleDayType = shuttleDayType;
        this.departureTime = departureTime;
    }
}