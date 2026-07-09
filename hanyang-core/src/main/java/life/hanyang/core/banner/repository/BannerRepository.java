package life.hanyang.core.banner.repository;

import life.hanyang.core.banner.domain.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findAllByOrderByDisplayOrderAsc();
    List<Banner> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}