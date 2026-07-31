package life.hanyang.core.weather.client;

import life.hanyang.core.weather.dto.UltraSrtFcstResponseDto;
import life.hanyang.core.weather.dto.UltraSrtNcstResponseDto;
import life.hanyang.core.weather.dto.VillageFcstResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class WeatherApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceKey;

    public WeatherApiClient(
            RestClient.Builder builder,
            @Value("${api.weather.base-url:http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") String baseUrl,
            @Value("${api.weather.service-key:}") String serviceKey
    ) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        this.restClient = builder.uriBuilderFactory(factory).build();
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public VillageFcstResponseDto fetchVillageFcst(String baseDate, String baseTime, int nx, int ny) {
        return fetchApi("/getVilageFcst", baseDate, baseTime, nx, ny, VillageFcstResponseDto.class);
    }

    public UltraSrtFcstResponseDto fetchUltraSrtFcst(String baseDate, String baseTime, int nx, int ny) {
        return fetchApi("/getUltraSrtFcst", baseDate, baseTime, nx, ny, UltraSrtFcstResponseDto.class);
    }

    public UltraSrtNcstResponseDto fetchUltraSrtNcst(String baseDate, String baseTime, int nx, int ny) {
        return fetchApi("/getUltraSrtNcst", baseDate, baseTime, nx, ny, UltraSrtNcstResponseDto.class);
    }

    private <T> T fetchApi(String endpoint, String baseDate, String baseTime, int nx, int ny, Class<T> responseType) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + endpoint)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }
}
