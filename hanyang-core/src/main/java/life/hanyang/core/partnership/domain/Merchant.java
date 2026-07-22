package life.hanyang.core.partnership.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "merchant")
@NoArgsConstructor
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_name",nullable = false)
    private String storeName;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Partnership> partnerships = new ArrayList<>();

    public void addPartnership(Partnership partnership) {
        this.partnerships.add(partnership);
        partnership.setMerchant(this); // 자식 객체에도 부모 객체를 주입합니다.
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private MerchantCategory merchantCategory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "emoji", nullable = false)
    private String emoji;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "full_address")
    private String fullAddress;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "kakao_place_id")
    private String kakaoPlaceId;

    @Builder
    public Merchant(String storeName, MerchantCategory merchantCategory, Boolean isActive, String emoji, Double latitude, Double longitude, String fullAddress, String kakaoPlaceId) {
        this.storeName = storeName;
        this.merchantCategory = merchantCategory;
        this.isActive = isActive == null || isActive;
        this.emoji = emoji;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fullAddress = fullAddress;
        this.kakaoPlaceId = kakaoPlaceId;
    }

    public void update(String storeName, MerchantCategory merchantCategory, Boolean isActive, String emoji, Double latitude, Double longitude, String fullAddress, String kakaoPlaceId) {
        this.storeName = storeName;
        this.merchantCategory = merchantCategory;
        this.isActive = isActive == null || isActive;
        this.emoji = emoji;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fullAddress = fullAddress;
        this.kakaoPlaceId = kakaoPlaceId;
    }
}