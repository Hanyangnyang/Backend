package life.hanyang.core.academic.domain;

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
@Table(
        name = "academic_periods",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_academic_period", columnNames = {"year", "semester", "period_type"})
        },
        indexes = {
                @Index(name = "idx_academic_periods_lookup", columnList = "start_date, end_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester", nullable = false, length = 20)
    private AcademicSemester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private AcademicPeriodType periodType;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public AcademicPeriod(Integer year, AcademicSemester semester, AcademicPeriodType periodType, String name, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이거나 같아야 합니다.");
        }
        this.year = year;
        this.semester = semester;
        this.periodType = periodType;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(Integer year, AcademicSemester semester, AcademicPeriodType periodType, String name, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이거나 같아야 합니다.");
        }
        if (year != null) this.year = year;
        if (semester != null) this.semester = semester;
        if (periodType != null) this.periodType = periodType;
        if (name != null && !name.isBlank()) this.name = name;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
    }
}
