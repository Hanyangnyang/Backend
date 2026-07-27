package life.hanyang.core.menu.service;

import life.hanyang.core.menu.dto.MenuResponse;
import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.Menu;
import life.hanyang.core.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 특정 기간 및 특정 식당 코드의 메뉴 목록을 날짜별로 그룹화하여 조회
     */
    public Map<LocalDate, List<MenuResponse>> getMenusGroupByDate(
            LocalDate startDate, 
            LocalDate endDate, 
            List<CafeteriaCode> codes
    ) {
        LocalDate start;
        LocalDate end;

        if (startDate == null && endDate == null) {
            start = LocalDate.now().minusDays(7);
            end = LocalDate.now().plusDays(7);
        } else if (startDate != null && endDate == null) {
            start = startDate;
            end = startDate;
        } else if (startDate == null && endDate != null) {
            start = endDate;
            end = endDate;
        } else {
            start = startDate;
            end = endDate;
        }

        // 1. DB에서 조건에 부합하는 모든 식단 데이터를 FETCH JOIN으로 1번의 쿼리로 일괄 조회
        List<Menu> menus = (codes == null || codes.isEmpty())
                ? menuRepository.findByDateBetweenWithCafeteria(start, end)
                : menuRepository.findByDateBetweenAndCafeteria_CodeInWithCafeteria(start, end, codes);

        // 2. 조회된 Menu 엔티티들을 날짜(LocalDate) -> 식당(Cafeteria) 순으로 계층 구조화
        return menus.stream()
                .collect(Collectors.groupingBy(
                        Menu::getDate,
                        Collectors.groupingBy(
                                Menu::getCafeteria,
                                Collectors.mapping(
                                        menuEntity -> {
                                            List<String> menuItems = (menuEntity.getDisplayMenu() == null || menuEntity.getDisplayMenu().isBlank())
                                                    ? List.of()
                                                    : List.of(menuEntity.getDisplayMenu().split("\n"));
                                            return new MenuResponse.MenuDetailResponse(
                                                    menuEntity.getId(),
                                                    menuEntity.getType(),
                                                    menuEntity.getDisplayOrder(),
                                                    menuEntity.getPrice(),
                                                    menuItems,
                                                    menuEntity.getRawMenu()
                                            );
                                        },
                                        Collectors.toList()
                                )
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .map(cafeteriaEntry -> {
                                    Cafeteria cafeteria = cafeteriaEntry.getKey();
                                    List<MenuResponse.MenuDetailResponse> details = cafeteriaEntry.getValue();
                                    return new MenuResponse(
                                            cafeteria.getCode(),
                                            cafeteria.getName(),
                                            cafeteria.getOperatingHours(),
                                            details
                                    );
                                })
                                .collect(Collectors.toList())
                ));
    }
}
