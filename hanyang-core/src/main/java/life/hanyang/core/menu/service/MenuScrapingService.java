package life.hanyang.core.menu.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.util.MenuParserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuScrapingService {
    private final Executor scrapingTaskExecutor;
    private final MenuSaveService menuSaveService;
    private final TransactionCacheEvictor transactionCacheEvictor;

    private static final String BASE_URL_PATTERN =
            "https://life.hanyang.ac.kr/theme/pages/facilities/detail.php?id=%d&date=%s";

    /**
     * CompletableFuture 기반 병렬 스크래핑 수행
     */
    public CompletableFuture<Void> scrapeCafeterias(List<CafeteriaCode> codes, LocalDate startDate, LocalDate endDate) {
        List<CafeteriaCode> targetCodes = (codes == null || codes.isEmpty())
                ? List.of(CafeteriaCode.values())
                : codes;

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

        List<LocalDate> targetDates = start.datesUntil(end.plusDays(1)).toList();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (LocalDate date : targetDates) {
            for (CafeteriaCode code : targetCodes) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        scrapeSingleCafeteriaForDate(code, date);
                    } catch (Exception e) {
                        log.error("Failed to scrape cafeteria [{}] for date [{}]: {}",
                                code.getDefaultName(), date, e.getMessage(), e);
                    }
                }, scrapingTaskExecutor);

                futures.add(future);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    log.info("Finished all scraping tasks for target dates");
                    transactionCacheEvictor.evictCacheAfterCommit("menu");
                });
    }

    private void scrapeSingleCafeteriaForDate(CafeteriaCode code, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String url = String.format(BASE_URL_PATTERN, code.getFacilityId(), dateStr);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(20000)
                    .get();

            Map<String, String> hours = parseOperatingHours(doc);
            List<MenuCrawlResultDto.MenuDetailDto> menus = parseMenus(doc);

            MenuCrawlResultDto saveDto = new MenuCrawlResultDto(code, date, hours, menus);
            menuSaveService.saveCafeteriaAndMenus(saveDto);

            log.info("Successfully scraped cafeteria [{}] for date [{}]", code.getDefaultName(), dateStr);
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Scraping failed for %s (%s) on %s - Cause: %s (%s)",
                    code.getDefaultName(), code.getCode(), dateStr, e.getClass().getSimpleName(), e.getMessage()
            );
            log.error("Error occurred while scraping cafeteria [{}] for date [{}]: {}", code.getDefaultName(), dateStr, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    List<MenuCrawlResultDto.MenuDetailDto> parseMenus(Document doc) {
        List<MenuCrawlResultDto.MenuDetailDto> menus = new ArrayList<>();
        if (doc.select(".menu-group").isEmpty()) {
            Element emptyMessage = doc.selectFirst("#view-pane-daily .fac-empty-msg");
            if (emptyMessage != null && emptyMessage.text().contains("등록된 식단이 없습니다")) {
                return menus;
            }
            throw new IllegalStateException("학식 페이지에서 메뉴 영역을 찾지 못했습니다.");
        }
        for (Element group : doc.select(".menu-group")) {
            validateMenuGroup(group);
            int menuCountBeforeGroup = menus.size();
            Element titleElement = group.selectFirst(".menu-group__title");
            if (titleElement == null) {
                throw new IllegalStateException("학식 메뉴의 식사 구분을 찾지 못했습니다.");
            }

            MealType mealType = MealType.fromTitle(titleElement.text());
            for (Element item : group.select(".menu-item, .menu-card-v")) {
                Element nameElement = item.selectFirst(".menu-item__name, .menu-card-v__name");
                if (nameElement == null || nameElement.text().isBlank()) {
                    throw new IllegalStateException("학식 메뉴명을 읽지 못했습니다.");
                }

                String rawName = nameElement.text().replaceAll("\\s+", " ").trim();
                String mainDish = MenuParserUtils.formatMainDishName(rawName);
                List<String> menuItems = new ArrayList<>();
                if (!mainDish.isBlank()) {
                    menuItems.add(mainDish);
                }

                Element descriptionElement = item.selectFirst(".menu-item__desc, .menu-card-v__desc");
                Element priceElement = item.selectFirst(".menu-item__price, .menu-card-v__price");
                if (mainDish.isBlank() || descriptionElement == null || priceElement == null) {
                    throw new IllegalStateException("학식 메뉴 카드의 필수 정보를 읽지 못했습니다.");
                }
                if (descriptionElement != null) {
                    Arrays.stream(descriptionElement.html().split("(?i)<br\\s*/?>"))
                            .map(Jsoup::parse)
                            .map(Document::text)
                            .flatMap(text -> Arrays.stream(text.split("/")))
                            .map(String::trim)
                            .filter(text -> !text.isBlank())
                            .forEach(menuItems::add);
                }

                Integer price = MenuParserUtils.parsePrice(priceElement.text());
                if (price == null) {
                    throw new IllegalStateException("학식 메뉴 가격을 읽지 못했습니다.");
                }
                String rawMenu = String.join(" ", rawName,
                        descriptionElement == null ? "" : descriptionElement.text(),
                        priceElement == null ? "" : priceElement.text()).trim();

                if (menuItems.isEmpty()) {
                    continue;
                }

                menus.add(new MenuCrawlResultDto.MenuDetailDto(
                        mealType, rawMenu, String.join("\n", menuItems), price
                ));
            }
            // 구조 변경을 정상적인 빈 식단으로 저장하면 기존 메뉴가 삭제되므로 실패 처리한다.
            if (menus.size() == menuCountBeforeGroup) {
                throw new IllegalStateException("학식 메뉴 영역에서 메뉴를 읽지 못했습니다.");
            }
        }
        return menus;
    }

    private void validateMenuGroup(Element group) {
        Element title = group.selectFirst(".menu-group__title");
        if (title == null || !List.of("조식", "중식", "석식").contains(title.text().trim())) {
            throw new IllegalStateException("학식 메뉴의 식사 구분을 인식하지 못했습니다.");
        }
        // 인식된 카드 수만 검사하면 혼합된 새 구조의 카드를 조용히 누락할 수 있다.
        // 알려진 컨테이너의 모든 자식을 검사한다. 정상 구조로 잘린 HTML까지 감지할 수는 없다.
        for (Element child : group.children()) {
            if (child.hasClass("menu-group__title")) {
                continue;
            }
            if (child.hasClass("menu-grid-2col")) {
                if (child.children().isEmpty()) {
                    throw new IllegalStateException("학식 메뉴 컨테이너가 비어 있습니다.");
                }
                for (Element card : child.children()) {
                    if (!card.hasClass("menu-card-v") && !card.hasClass("menu-item")) {
                        throw new IllegalStateException("학식 메뉴 컨테이너에 알 수 없는 카드 구조가 있습니다.");
                    }
                }
            } else if (!child.hasClass("menu-item") && !child.hasClass("menu-card-v")) {
                throw new IllegalStateException("학식 메뉴 그룹에 알 수 없는 구조가 있습니다.");
            }
        }
    }

    Map<String, String> parseOperatingHours(Document doc) {
        Map<String, String> hours = new HashMap<>();
        for (Element row : doc.select(".info-table tr")) {
            Element label = row.selectFirst(".info-label");
            Element value = row.selectFirst(".info-value");
            if (label == null || value == null || !label.text().trim().equals("영업시간")) {
                continue;
            }

            Matcher timeMatcher = Pattern.compile("(조식|중식|석식)\\s*[:：]?\\s*(\\d{1,2}:\\d{2}\\s*~\\s*\\d{1,2}:\\d{2})")
                    .matcher(value.wholeText());
            while (timeMatcher.find()) {
                hours.put(timeMatcher.group(1), timeMatcher.group(2).replaceAll("\\s*~\\s*", "~"));
            }
        }
        return hours;
    }
}
