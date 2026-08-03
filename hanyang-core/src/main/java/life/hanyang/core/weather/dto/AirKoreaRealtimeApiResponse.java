package life.hanyang.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AirKoreaRealtimeApiResponse(
        Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Body body,
            Header header
    ){
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Header(
                String resultMsg,
                String resultCode
        ){}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Body(
                List<AirKoreaRealtimeItem> items
        ){
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record AirKoreaRealtimeItem(
                    String pm25Grade1h,
                    String pm25Value,
                    String pm10Grade1h,
                    String pm10Value,
                    String dataTime
                    )
            {}
        }
    }
}