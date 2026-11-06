package life.hanyang.core.printer.domain;

import jakarta.persistence.*;
import life.hanyang.core.campusmap.domain.CampusBuilding;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Printer {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private CampusBuilding building;
    @Column(nullable = false, columnDefinition = "text") private String detailLocation;
    @Column(length = 100) private String name;
    @Column(columnDefinition = "text") private String price;
    @Column(columnDefinition = "text") private String paymentMethod;
    @Column(columnDefinition = "text") private String notes;
    @Column(nullable = false) private boolean broken;
    @Column(nullable = false) private boolean removed;
    @Column(nullable = false) private boolean deleted;
    @Column(nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    public Printer(CampusBuilding building, String detailLocation, String name, String price,
                   String paymentMethod, String notes, boolean broken, boolean removed) {
        this.id = UUID.randomUUID();
        update(building, detailLocation, name, price, paymentMethod, notes, broken, removed);
    }

    public void update(CampusBuilding building, String detailLocation, String name, String price,
                       String paymentMethod, String notes, boolean broken, boolean removed) {
        this.building = building;
        this.detailLocation = detailLocation;
        this.name = name;
        this.price = price;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.broken = broken;
        this.removed = removed;
        this.updatedAt = Instant.now();
    }

    public void delete() {
        deleted = true;
        updatedAt = Instant.now();
    }
}
