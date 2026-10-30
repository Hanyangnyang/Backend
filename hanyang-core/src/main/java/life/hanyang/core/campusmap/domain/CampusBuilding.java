package life.hanyang.core.campusmap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "campus_buildings")
@NoArgsConstructor
public class CampusBuilding {
    @Id
    @Column(length = 100)
    private String id;
    @Column(name = "building_number", nullable = false)
    private String buildingNumber;
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;
    @Column(name = "english_name", nullable = true, columnDefinition = "TEXT")
    private String englishName;
    @Enumerated(EnumType.STRING)
    @Column(name = "campus", nullable = false)
    private Campus campus;
    @Embedded
    private Coordinates coordinates;
    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;
    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OpenSpace> openSpaces = new ArrayList<>();

    public void addOpenSpace(OpenSpace space) { openSpaces.add(space); }
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "aliases", nullable = false, columnDefinition = "text array")
    private List<String> aliases = new ArrayList<>();
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "primary_colleges", nullable = false, columnDefinition = "text array")
    private List<String> primaryColleges = new ArrayList<>();
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "facilities", nullable = false, columnDefinition = "text array")
    private List<String> facilities = new ArrayList<>();
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "image_url", nullable = false, columnDefinition = "text array")
    private List<String> imageUrl = new ArrayList<>();

    public CampusBuilding(String id, String buildingNumber, String name, String englishName, Campus campus, Coordinates coordinates, String description, List<String> aliases, List<String> primaryColleges, List<String> facilities, List<String> imageUrl) {
        this.id = id;
        update(buildingNumber, name, englishName, campus, coordinates, description, aliases, primaryColleges, facilities, imageUrl);
    }

    public void update(String buildingNumber, String name, String englishName, Campus campus, Coordinates coordinates, String description, List<String> aliases, List<String> primaryColleges, List<String> facilities, List<String> imageUrl) {
        this.buildingNumber = buildingNumber;
        this.name = name;
        this.englishName = englishName;
        this.campus = campus;
        this.coordinates = coordinates;
        this.description = description;
        this.aliases.clear();
        if (aliases != null) this.aliases.addAll(aliases);
        this.primaryColleges.clear();
        if (primaryColleges != null) this.primaryColleges.addAll(primaryColleges);
        this.facilities.clear();
        if (facilities != null) this.facilities.addAll(facilities);
        this.imageUrl.clear();
        if (imageUrl != null) this.imageUrl.addAll(imageUrl);
    }
}
