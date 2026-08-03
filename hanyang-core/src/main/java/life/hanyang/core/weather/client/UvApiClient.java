package life.hanyang.core.weather.client;

import life.hanyang.core.weather.dto.UvResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.time.Duration;

@Component
public class UvApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public UvApiClient(
            RestClient.Builder builder,
            @Value("${api.uv.base-url:https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5}") String baseUrl,
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
     * 자외선지수 조회 (getUVIdxV5)
     *
     * @param areaNo 행정구역코드 (예: "4127100000")
     * @param time 발표시각 (YYYYMMDDHH 형식, 예: "2026080306")
     * @return UvResponseDto
     */
    public UvResponseDto fetchUvIndex(String areaNo, String time) {
        String urlString = String.format(
                "%s/getUVIdxV5?serviceKey=%s&areaNo=%s&time=%s&dataType=JSON",
                baseUrl, serviceKey, areaNo, time
        );

        URI uri = URI.create(urlString);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(UvResponseDto.class);
    }
}
