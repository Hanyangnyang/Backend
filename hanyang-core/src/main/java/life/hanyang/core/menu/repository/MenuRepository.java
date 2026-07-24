package life.hanyang.core.menu.repository;

import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    Optional<Menu> findByCafeteriaAndDateAndType(Cafeteria cafeteria, LocalDate date, MealType type);
    List<Menu> findByCafeteriaAndDate(Cafeteria cafeteria, LocalDate date);
    List<Menu> findByDate(LocalDate date);
    List<Menu> findByDateBetween(LocalDate start, LocalDate end);
    List<Menu> findByDateBetweenAndCafeteria_CodeIn(LocalDate start, LocalDate end, List<CafeteriaCode> codes);
    void deleteByCafeteriaAndDate(Cafeteria cafeteria, LocalDate date);
}
