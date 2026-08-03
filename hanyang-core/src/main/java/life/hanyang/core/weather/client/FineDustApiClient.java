package life.hanyang.core.weather.client;

import life.hanyang.core.weather.dto.AirKoreaRealtimeApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class FineDustApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public FineDustApiClient(
            RestClient.Builder builder,
            @Value("${api.finedust.base-url:https://apis.data.go.kr/B552584/ArpltnInforInqireSvc}") String baseUrl,
            @Value("${api.weather.service-key:}") String serviceKey
    ) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder
                .requestFactory(requestFactory)
                .uriBuilderFactory(factory)
                .build();
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    /**
     * 측정소별 실시간 미세먼지/대기오염 정보 조회 (getMsrstnAcctoRltmMesureDnsty)
     *
     * @param stationName 측정소명 (예: "본오동")
     * @return AirKoreaRealtimeApiResponse
     */
    public AirKoreaRealtimeApiResponse fetchRealtimeFineDust(String stationName) {
        String encodedStationName = UriUtils.encode(stationName, StandardCharsets.UTF_8);

        String urlString = String.format(
                "%s/getMsrstnAcctoRltmMesureDnsty?serviceKey=%s&returnType=json&numOfRows=100&pageNo=1&stationName=%s&dataTerm=DAILY&ver=1.3",
                baseUrl, serviceKey, encodedStationName
        );

        URI uri = URI.create(urlString);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(AirKoreaRealtimeApiResponse.class);
    }
}