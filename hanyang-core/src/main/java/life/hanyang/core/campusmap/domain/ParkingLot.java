package life.hanyang.core.campusmap.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "campus_parking_lots")
@NoArgsConstructor
public class ParkingLot {
    @Id
    @Column(length = 100)
    private String id;
    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "campus", nullable = false)
    private Campus campus;
    @Embedded
    private Coordinates coordinates;
    @Column(name = "capacity")
    private Integer capacity;
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "image_url", nullable = false, columnDefinition = "text array")
    private List<String> imageUrl = new ArrayList<>();

    public ParkingLot(String id, String name, Campus campus, Coordinates coordinates, Integer capacity, String address, String description, List<String> imageUrl) {
        this.id = id;
        update(name, campus, coordinates, capacity, address, description, imageUrl);
    }

    public void update(String name, Campus campus, Coordinates coordinates, Integer capacity, String address, String description, List<String> imageUrl) {
        this.name = name;
        this.campus = campus;
        this.coordinates = coordinates;
        this.capacity = capacity;
        this.address = address;
        this.description = description;
        this.imageUrl.clear();
        if (imageUrl != null) this.imageUrl.addAll(imageUrl);
    }
}
