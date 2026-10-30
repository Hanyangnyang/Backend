package life.hanyang.core.campusmap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "campus_open_spaces")
@NoArgsConstructor
public class OpenSpace {
    @Id
    @Column(length = 100)
    private String id;
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;
    @Column(name = "floor", nullable = true, columnDefinition = "TEXT")
    private String floor;
    @Column(name = "hint", nullable = true, columnDefinition = "TEXT")
    private String hint;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private CampusBuilding building;

    public OpenSpace(String id, String name, String floor, String hint, CampusBuilding building) {
        this.id = id;
        this.building = building;
        update(name, floor, hint);
    }

    public void update(String name, String floor, String hint) {
        this.name = name;
        this.floor = floor;
        this.hint = hint;
    }
}
