package life.hanyang.core.bus.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter
@Entity
@Table(name = "bus_arrival_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusArrivalLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stationId;

    @Column(nullable = false)
    private String routeName;

    @Column(nullable = false)
    private String plateNo;

    @Column(nullable = false)
    private Instant arrivedAt;

    @Builder
    public BusArrivalLog(String stationId, String routeName, String plateNo, Instant arrivedAt) {
        this.stationId = stationId;
        this.routeName = routeName;
        this.plateNo = plateNo;
        this.arrivedAt = arrivedAt;
    }
}
