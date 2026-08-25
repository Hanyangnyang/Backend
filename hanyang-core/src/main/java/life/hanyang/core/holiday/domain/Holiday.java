package life.hanyang.core.holiday.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holiday_date", columnList = "holiday_date", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private DayType dayType = DayType.HOLIDAY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Holiday(LocalDate date, String name, DayType dayType) {
        this.date = date;
        this.name = name;
        this.dayType = (dayType != null) ? dayType : DayType.HOLIDAY;
    }

    public void update(String name, DayType dayType) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (dayType != null) {
            this.dayType = dayType;
        }
    }
}
