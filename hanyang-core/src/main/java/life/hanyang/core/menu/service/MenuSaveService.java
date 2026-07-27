package life.hanyang.core.menu.service;

import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.entity.Menu;
import life.hanyang.core.menu.repository.CafeteriaRepository;
import life.hanyang.core.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuSaveService {

    private final CafeteriaRepository cafeteriaRepository;
    private final MenuRepository menuRepository;

    /**
     * 관리자에 의한 특정 식단 내용 수동 수정 (isOverridden = true 설정되어 자동 크롤링 시 보호됨)
     */
    public void updateMenuDisplay(Long menuId, String displayMenu) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식단 ID입니다: " + menuId));
        menu.updateDisplayMenu(displayMenu);
        menuRepository.save(menu);
    }

    public void saveCafeteriaAndMenus(MenuCrawlResultDto dto) {
        Cafeteria cafeteria = cafeteriaRepository.findByCode(dto.code())
                .orElseGet(() -> cafeteriaRepository.save(new Cafeteria(dto.code(), dto.code().getDefaultName())));

        if (dto.operatingHours() != null && !dto.operatingHours().isEmpty()) {
            cafeteria.updateOperatingHours(dto.operatingHours());
            cafeteriaRepository.save(cafeteria);
        }

        // MealType별 순서(displayOrder) 카운터
        Map<MealType, Integer> orderCounter = new HashMap<>();

        for (MenuCrawlResultDto.MenuDetailDto menuDto : dto.menus()) {
            MealType mealType = menuDto.mealType();
            int currentOrder = orderCounter.getOrDefault(mealType, 0);

            Optional<Menu> existingOpt = menuRepository.findByCafeteriaAndDateAndTypeAndDisplayOrder(
                    cafeteria, dto.date(), mealType, currentOrder
            );

            if (existingOpt.isPresent()) {
                Menu existing = existingOpt.get();
                existing.updateContent(menuDto.rawMenu(), menuDto.displayMenu(), menuDto.price());
                menuRepository.save(existing);
            } else {
                Menu menu = new Menu(
                        cafeteria, dto.date(), mealType, currentOrder,
                        menuDto.rawMenu(), menuDto.displayMenu(), menuDto.price()
                );
                menuRepository.save(menu);
            }

            orderCounter.put(mealType, currentOrder + 1);
        }
    }
}

