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
import org.springframework.scheduling.annotation.Async;
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
        for (Element group : doc.select(".menu-group")) {
            Element titleElement = group.selectFirst(".menu-group__title");
            if (titleElement == null) {
                continue;
            }

            MealType mealType = MealType.fromTitle(titleElement.text());
            for (Element item : group.select(".menu-item")) {
                Element nameElement = item.selectFirst(".menu-item__name");
                if (nameElement == null) {
                    continue;
                }

                String rawName = nameElement.text().replaceAll("\\s+", " ").trim();
                String mainDish = MenuParserUtils.removeEnglishTranslation(rawName);
                List<String> menuItems = new ArrayList<>();
                if (!mainDish.isBlank()) {
                    menuItems.add(mainDish);
                }

                Element descriptionElement = item.selectFirst(".menu-item__desc");
                if (descriptionElement != null) {
                    Arrays.stream(descriptionElement.html().split("(?i)<br\\s*/?>"))
                            .map(Jsoup::parse)
                            .map(Document::text)
                            .map(String::trim)
                            .filter(text -> !text.isBlank())
                            .forEach(menuItems::add);
                }

                Element priceElement = item.selectFirst(".menu-item__price");
                Integer price = priceElement == null ? null : MenuParserUtils.parsePrice(priceElement.text());
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
        }
        return menus;
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
