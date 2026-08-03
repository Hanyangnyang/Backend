package life.hanyang.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UvResponseDto(
        Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Items items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            List<Item> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String code,
            String areaNo,
            String date, // 발표시각 예: "2026080306"
            String h0, String h3, String h6, String h9, String h12, String h15,
            String h18, String h21, String h24, String h27, String h30, String h33,
            String h36, String h39, String h42, String h45, String h48, String h51,
            String h54, String h57, String h60, String h63, String h66, String h69,
            String h72, String h75
    ) {}
}
