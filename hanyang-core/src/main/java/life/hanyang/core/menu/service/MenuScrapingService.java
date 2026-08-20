package life.hanyang.core.menu.service;

import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import life.hanyang.core.menu.util.MenuParserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuScrapingService {
    private final Executor scrapingTaskExecutor;
    private final MenuSaveService menuSaveService;

    private static final String BASE_URL_PATTERN =
            "https://www.hanyang.ac.kr/web/www/%s?p_p_id=kr_ac_hanyang_cafe_web_portlet_CafePortlet&p_p_lifecycle=0&p_p_state=normal&p_p_mode=view&_kr_ac_hanyang_cafe_web_portlet_CafePortlet_sMenuDate=%s&_kr_ac_hanyang_cafe_web_portlet_CafePortlet_action=view";

    /**
     * CompletableFuture 기반 10개 병렬 스크래핑 수행
     */
    public CompletableFuture<Void> scrapeCafeterias(List<CafeteriaCode> codes, List<LocalDate> dates) {
        List<CafeteriaCode> targetCodes = (codes == null || codes.isEmpty())
                ? List.of(CafeteriaCode.values())
                : codes;

        List<LocalDate> targetDates;
        if (dates == null || dates.isEmpty()) {
            targetDates = new ArrayList<>();
            LocalDate start = LocalDate.now().minusDays(7); // 일주일 전(D-7)
            for (int i = 0; i < 15; i++) { // 일주일 전 ~ 일주일 뒤까지 총 15일
                targetDates.add(start.plusDays(i));
            }
        } else {
            targetDates = dates;
        }

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
                .thenRun(() -> log.info("Finished all scraping tasks for target dates"));
    }

    private void scrapeSingleCafeteriaForDate(CafeteriaCode code, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String encodedDate = URLEncoder.encode(dateStr, StandardCharsets.UTF_8);
        String url = String.format(BASE_URL_PATTERN, code.getCode(), encodedDate);

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

    private List<MenuCrawlResultDto.MenuDetailDto> parseMenus(Document doc) {
        List<MenuCrawlResultDto.MenuDetailDto> menus = new ArrayList<>();
        Elements h3Elements = doc.select("h3");

        for (Element h3 : h3Elements) {
            String title = h3.text().trim();
            if (!MenuParserUtils.isValidTitle(title)) {
                continue;
            }

            Element nextEl = h3.nextElementSibling();
            if (nextEl == null) {
                continue;
            }

            String menuText = nextEl.text().replaceAll("\\s+", " ").trim();
            if (menuText.length() <= 5 || menuText.contains("확인 가능합니다")) {
                continue;
            }

            List<MenuParserUtils.ParsedMenu> parsedSets = MenuParserUtils.parseMenuSets(menuText);
            MealType mealType = MealType.fromTitle(title);

            for (MenuParserUtils.ParsedMenu parsed : parsedSets) {
                menus.add(new MenuCrawlResultDto.MenuDetailDto(
                        mealType, menuText, parsed.cleanedMenu(), parsed.price()
                ));
            }
        }
        return menus;
    }

    private Map<String, String> parseOperatingHours(Document doc) {
        Map<String, String> hours = new HashMap<>();
        String fullText = doc.body().text().replaceAll("\\s+", " ");

        Matcher sectionMatcher = Pattern.compile("운영시간(.{0,300})").matcher(fullText);
        if (sectionMatcher.find()) {
            String sectionText = sectionMatcher.group(0);
            Matcher timeMatcher = Pattern.compile("(조식|중식|석식)[\\s:]*(\\d{1,2}:\\d{2}\\s*~\\s*\\d{1,2}:\\d{2})").matcher(sectionText);
            while (timeMatcher.find()) {
                String meal = timeMatcher.group(1);
                String time = timeMatcher.group(2)
                        .replaceAll("\\s+", " ")
                        .replaceAll("\\s*~\\s*", "~")
                        .trim();
                hours.put(meal, time);
            }
        }
        return hours;
    }
}

