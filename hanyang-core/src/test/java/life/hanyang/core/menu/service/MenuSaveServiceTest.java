package life.hanyang.core.menu.service;

import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.entity.Menu;
import life.hanyang.core.menu.repository.CafeteriaRepository;
import life.hanyang.core.menu.repository.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuSaveServiceTest {

    @Test
    @DisplayName("검증된 식단으로 갱신하고 원본에서 철회된 일반 메뉴만 삭제한다")
    void replaceMenusAfterValidatedCollection() {
        CafeteriaRepository cafeteriaRepository = mock(CafeteriaRepository.class);
        MenuRepository menuRepository = mock(MenuRepository.class);
        MenuSaveService service = new MenuSaveService(cafeteriaRepository, menuRepository,
                mock(TransactionCacheEvictor.class));
        Cafeteria cafeteria = new Cafeteria(CafeteriaCode.RE12, "학생식당");
        LocalDate date = LocalDate.of(2026, 10, 14);
        Menu first = new Menu(cafeteria, date, MealType.LUNCH, 0, "메뉴 A", "메뉴 A", 4500);
        Menu omitted = new Menu(cafeteria, date, MealType.LUNCH, 1, "메뉴 B", "메뉴 B", 6500);
        Menu overridden = new Menu(cafeteria, date, MealType.DINNER, 0, "메뉴 C", "메뉴 C", 6500);
        overridden.updateDisplayMenu("관리자 메뉴");
        when(cafeteriaRepository.findByCode(CafeteriaCode.RE12)).thenReturn(Optional.of(cafeteria));
        when(menuRepository.findByCafeteriaAndDateAndTypeAndDisplayOrder(cafeteria, date, MealType.LUNCH, 0))
                .thenReturn(Optional.of(first));
        when(menuRepository.findByCafeteriaAndDate(cafeteria, date))
                .thenReturn(List.of(first, omitted, overridden));

        service.saveCafeteriaAndMenus(new MenuCrawlResultDto(CafeteriaCode.RE12, date, Map.of(),
                List.of(new MenuCrawlResultDto.MenuDetailDto(MealType.LUNCH, "새 메뉴 A", "새 메뉴 A", 5000))));

        verify(menuRepository).save(first);
        verify(menuRepository).deleteAll(List.of(omitted));
        assertEquals("새 메뉴 A", first.getDisplayMenu());
        assertEquals("메뉴 B", omitted.getDisplayMenu());
        assertEquals("관리자 메뉴", overridden.getDisplayMenu());
    }

    @Test
    @DisplayName("빈 식단을 수집하면 일반 메뉴만 정리하고 관리자 수정 메뉴는 보존한다")
    void removeOnlyNonOverriddenStaleMenus() {
        CafeteriaRepository cafeteriaRepository = mock(CafeteriaRepository.class);
        MenuRepository menuRepository = mock(MenuRepository.class);
        MenuSaveService service = new MenuSaveService(
                cafeteriaRepository,
                menuRepository,
                mock(TransactionCacheEvictor.class)
        );
        Cafeteria cafeteria = new Cafeteria(CafeteriaCode.RE12, "학생식당");
        LocalDate date = LocalDate.of(2026, 9, 13);
        Menu staleMenu = new Menu(cafeteria, date, MealType.LUNCH, 0, "기존 메뉴", "기존 메뉴", 4500);
        Menu overriddenMenu = new Menu(cafeteria, date, MealType.LUNCH, 1, "수정 전", "수정 전", 5000);
        overriddenMenu.updateDisplayMenu("관리자 수정 메뉴");

        when(cafeteriaRepository.findByCode(CafeteriaCode.RE12)).thenReturn(Optional.of(cafeteria));
        when(menuRepository.findByCafeteriaAndDate(cafeteria, date))
                .thenReturn(List.of(staleMenu, overriddenMenu));

        service.saveCafeteriaAndMenus(new MenuCrawlResultDto(
                CafeteriaCode.RE12,
                date,
                Map.of(),
                List.of()
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Menu>> captor = ArgumentCaptor.forClass(List.class);
        verify(menuRepository).deleteAll(captor.capture());
        assertEquals(List.of(staleMenu), captor.getValue());
    }
}
