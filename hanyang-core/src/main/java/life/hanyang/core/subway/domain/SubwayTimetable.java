package life.hanyang.core.subway.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Entity
@Table(
    name = "subway_timetable",
    indexes = @Index(name = "idx_subway_timetable_station_line_direction_day_type", columnList = "subway_station, line, direction, day_type")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubwayTimetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subway_station", nullable = false, length = 50)
    private SubwayStation subwayStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "line", nullable = false, length = 30)
    private SubwayLine subwayLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 30)
    private SubwayDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 30)
    private SubwayDayType subwayDayType;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "destination", nullable = false)
    private String destination;

    @Column(name = "train_no", nullable = false)
    private String trainNo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
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
