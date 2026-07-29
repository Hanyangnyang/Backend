package life.hanyang.core.menu.repository;

import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByCafeteriaAndDateAndTypeAndDisplayOrder(Cafeteria cafeteria, LocalDate date, MealType type, Integer displayOrder);

    @Query("SELECT DISTINCT m FROM Menu m " +
           "JOIN FETCH m.cafeteria c " +
           "LEFT JOIN FETCH c.operatingHours " +
           "WHERE m.date BETWEEN :start AND :end " +
           "ORDER BY m.date ASC, m.displayOrder ASC")
    List<Menu> findByDateBetweenWithCafeteria(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT DISTINCT m FROM Menu m " +
           "JOIN FETCH m.cafeteria c " +
           "LEFT JOIN FETCH c.operatingHours " +
           "WHERE m.date BETWEEN :start AND :end " +
           "AND c.code IN :codes " +
           "ORDER BY m.date ASC, m.displayOrder ASC")
    List<Menu> findByDateBetweenAndCafeteria_CodeInWithCafeteria(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("codes") List<CafeteriaCode> codes
    );
}

