package life.hanyang.core.menu.service;

import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.Menu;
import life.hanyang.core.menu.repository.CafeteriaRepository;
import life.hanyang.core.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuSaveService {

    private final CafeteriaRepository cafeteriaRepository;
    private final MenuRepository menuRepository;

    public void saveCafeteriaAndMenus(MenuCrawlResultDto dto) {
        Cafeteria cafeteria = cafeteriaRepository.findByCode(dto.code())
                .orElseGet(() -> cafeteriaRepository.save(new Cafeteria(dto.code(), dto.code().getDefaultName())));

        if (dto.operatingHours() != null && !dto.operatingHours().isEmpty()) {
            cafeteria.updateOperatingHours(dto.operatingHours());
            cafeteriaRepository.save(cafeteria);
        }

        for (MenuCrawlResultDto.MenuDetailDto menuDto : dto.menus()) {
            Optional<Menu> existingOpt = menuRepository.findByCafeteriaAndDateAndType(
                    cafeteria, dto.date(), menuDto.mealType()
            );

            if (existingOpt.isPresent()) {
                Menu existing = existingOpt.get();
                existing.updateContent(menuDto.rawMenu(), menuDto.displayMenu(), menuDto.price());
                menuRepository.save(existing);
            } else {
                Menu menu = new Menu(
                        cafeteria, dto.date(), menuDto.mealType(),
                        menuDto.rawMenu(), menuDto.displayMenu(), menuDto.price()
                );
                menuRepository.save(menu);
            }
        }
    }
}
