package life.hanyang.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UltraSrtNcstResponseDto(
        Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Header header,
            Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String dataType,
            Items items,
            Integer pageNo,
            Integer numOfRows,
            Integer totalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            List<Item> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String baseDate,  // 기준일자 ("20260730")
            String baseTime,  // 기준시각 ("1100")
            String category,  // 카테고리 ("T1H", "RN1", "PTY" 등)
            Integer nx,       // X 좌표 (57)
            Integer ny,       // Y 좌표 (121)
            String obsrValue  // 관측값 ("31.8")
    ) {}
}