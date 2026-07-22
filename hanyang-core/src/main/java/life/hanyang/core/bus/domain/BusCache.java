package life.hanyang.core.bus.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Getter
@Entity
@Table(name = "bus_cache")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusCache {
    @Id
    private String stationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String data;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    public BusCache(String stationId, String data) {
        this.stationId = stationId;
        this.data = data;
    }
}
