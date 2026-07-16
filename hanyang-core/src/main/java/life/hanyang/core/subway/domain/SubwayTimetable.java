package life.hanyang.core.subway.domain;

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
@Table(name = "subway_timetable")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubwayTimetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "subway_station", nullable = false)
    private SubwayStation subwayStation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "line", nullable = false)
    private SubwayLine subwayLine;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "direction", nullable = false)
    private SubwayDirection direction;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "day_type", nullable = false)
    private SubwayDayType subwayDayType;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "destination", nullable = false)
    private String destination;

    @Column(name = "train_no", nullable = false)
    private String trainNo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    @UpdateTimestamp
    private Instant createdAt;

    @Builder
    public SubwayTimetable(SubwayStation subwayStation, SubwayLine subwayLine, SubwayDirection direction, SubwayDayType subwayDayType, LocalTime time, String destination, String trainNo) {
        this.subwayStation = subwayStation;
        this.subwayLine = subwayLine;
        this.direction = direction;
        this.subwayDayType = subwayDayType;
        this.time = time;
        this.destination = destination;
        this.trainNo = trainNo;
    }
}
