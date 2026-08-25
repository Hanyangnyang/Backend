package life.hanyang.core.holiday.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PublicHolidayApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;

    public record PublicHolidayItem(LocalDate date, String name) {}

    public PublicHolidayApiClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${api.holiday.base-url:https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService}") String baseUrl,
            @Value("${api.holiday.service-key:${api.public-data-key:}}") String serviceKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder.requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public List<PublicHolidayItem> fetchHolidays(int year) {
        List<PublicHolidayItem> result = new ArrayList<>();
        try {
            String url = String.format(
                    "%s/getRestDeInfo?serviceKey=%s&solYear=%d&numOfRows=100&_type=json",
                    baseUrl, serviceKey, year
            );

            String jsonResponse = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null || jsonResponse.isBlank()) {
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    addHolidayItem(item, formatter, result);
                }
            } else if (itemsNode.isObject()) {
                addHolidayItem(itemsNode, formatter, result);
            }
        } catch (Exception e) {
            log.error("공공데이터포털 공휴일 API 연동 실패 (year: {}): {}", year, e.getMessage(), e);
        }
        return result;
    }

    private void addHolidayItem(JsonNode item, DateTimeFormatter formatter, List<PublicHolidayItem> result) {
        String locdateStr = item.path("locdate").asText();
        String dateName = item.path("dateName").asText();

        if (!locdateStr.isBlank()) {
            LocalDate date = LocalDate.parse(locdateStr, formatter);
            result.add(new PublicHolidayItem(date, dateName));
        }
    }
}
