package life.hanyang.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UltraSrtFcstResponseDto(
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
            String baseDate,  // 발표일자 ("20260730")
            String baseTime,  // 발표시각 ("1530")
            String category,  // 카테고리 ("T1H", "RN1", "SKY", "PTY", "LGT" 등)
            String fcstDate,  // 예보일자 ("20260730")
            String fcstTime,  // 예보시각 ("1600")
            String fcstValue, // 예보값 ("31", "강수없음" 등)
            Integer nx,       // X 좌표 (57)
            Integer ny        // Y 좌표 (121)
    ) {}
}