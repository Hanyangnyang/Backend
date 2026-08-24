package life.hanyang.core.menu.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.menu.dto.DailyMenuResponse;
import life.hanyang.core.menu.dto.MenuResponse;
import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.Menu;
import life.hanyang.core.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 특정 기간 및 특정 식당 코드의 메뉴 목록을 날짜별 List 구조로 캐싱 조회
     */
    @Cacheable(cacheNames = "menu", key = "{#startDate, #endDate, #codes}")
    public List<DailyMenuResponse> getDailyMenus(
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

        if (start.isAfter(end)) {
            throw new BusinessException(
                    String.format("시작 날짜는 종료 날짜보다 이전이거나 같아야 합니다. (시작: %s, 종료: %s)", start, end),
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }

        // 1. DB에서 조건에 부합하는 모든 식단 데이터를 FETCH JOIN으로 1번의 쿼리로 일괄 조회
        List<Menu> menus = (codes == null || codes.isEmpty())
                ? menuRepository.findByDateBetweenWithCafeteria(start, end)
                : menuRepository.findByDateBetweenAndCafeteria_CodeInWithCafeteria(start, end, codes);

        // 2. 조회된 Menu 엔티티들을 날짜(LocalDate) -> 식당(Cafeteria) 순으로 계층 구조화
        Map<LocalDate, List<MenuResponse>> groupedMap = menus.stream()
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

        return groupedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyMenuResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Map 구조 반환 메서드 (하위 호환용)
     */
    public Map<LocalDate, List<MenuResponse>> getMenusGroupByDate(
            LocalDate startDate, 
            LocalDate endDate, 
            List<CafeteriaCode> codes
    ) {
        List<DailyMenuResponse> dailyMenus = getDailyMenus(startDate, endDate, codes);
        return dailyMenus.stream()
                .collect(Collectors.toMap(
                        DailyMenuResponse::date,
                        DailyMenuResponse::cafeterias,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }
}
