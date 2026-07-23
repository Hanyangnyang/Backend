package life.hanyang.core.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Entity
@Table(name  = "cafeteria")
public class Cafeteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private CafeteriaCode code;

    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "cafeteria_operating_hours", joinColumns = @JoinColumn(name = "cafeteria_id"))
    @MapKeyColumn(name = "meal_type")
    @Column(name = "operating_time")
    private Map<String, String> operatingHours = new HashMap<>();
}
