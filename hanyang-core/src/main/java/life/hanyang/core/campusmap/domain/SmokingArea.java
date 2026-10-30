package life.hanyang.core.campusmap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "campus_smoking_areas")
@NoArgsConstructor
public class SmokingArea {
    @Id
    @Column(length = 100)
    private String id;
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SmokingAreaType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "campus", nullable = false)
    private Campus campus;
    @Embedded
    private Coordinates coordinates;
    @Column(name = "has_ashtray", nullable = false)
    private Boolean hasAshtray;
    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "image_url", nullable = false, columnDefinition = "text array")
    private List<String> imageUrl = new ArrayList<>();

    public SmokingArea(String id, String name, SmokingAreaType type, Campus campus, Coordinates coordinates, Boolean hasAshtray, String description, List<String> imageUrl) {
        this.id = id;
        update(name, type, campus, coordinates, hasAshtray, description, imageUrl);
    }

    public void update(String name, SmokingAreaType type, Campus campus, Coordinates coordinates, Boolean hasAshtray, String description, List<String> imageUrl) {
        this.name = name;
        this.type = type;
        this.campus = campus;
        this.coordinates = coordinates;
        this.hasAshtray = hasAshtray;
        this.description = description;
        this.imageUrl.clear();
        if (imageUrl != null) this.imageUrl.addAll(imageUrl);
    }
}
