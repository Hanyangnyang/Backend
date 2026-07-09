package life.hanyang.core.partnership.repository;

import life.hanyang.core.partnership.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    @Query("SELECT DISTINCT m FROM Merchant m LEFT JOIN FETCH m.partnerships"
            + " WHERE m.isActive = true")
    List<Merchant> findAllWithPartnerships();
    Long countByMerchantIdIn(List<Long> merchantIds);
}