package life.hanyang.user.weather.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.weather.dto.WeatherBriefingResponse;
import life.hanyang.core.weather.dto.WeatherCompositeResponse;
import life.hanyang.core.weather.service.WeatherBriefingService;
import life.hanyang.core.weather.service.WeatherQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "날씨 API", description = "메인 화면용 날씨, 미세먼지, 자외선(UV) 통합 정보를 제공합니다.")
public class WeatherController {

    private final WeatherQueryService weatherQueryService;
    private final WeatherBriefingService weatherBriefingService;

    @GetMapping
    @Operation(
            summary = "메인 화면용 날씨/대기질/UV 통합 정보(현재 카드 + 과거/미래 24시간 슬라이더)를 조회합니다.",
            description = """
                    ### 📊 코드 및 지표 의미 안내
                    
                    **1. 미세먼지 / 초미세먼지 등급 (`pm10Grade`, `pm25Grade`)**
                    - `1`: 좋음 🔵
                    - `2`: 보통 🟢
                    - `3`: 나쁨 🟠
                    - `4`: 매우나쁨 🔴
                    - `null`: 측정 데이터 없음
                    
                    **2. 자외선 지수 구간 (`uvIndex`)**
                    - `0 ~ 2`: 낮음 🟢
                    - `3 ~ 5`: 보통 🟡
                    - `6 ~ 7`: 높음 🟠
                    - `8 ~ 10`: 매우높음 🔴
                    - `11 이상`: 위험 🟣
                    
                    **3. 날씨 상태 문자열 (`weatherCondition`)**
                    - `SUNNY`: 맑음 ☀️
                    - `MOSTLY_CLOUDY`: 구름많음 ⛅
                    - `CLOUDY`: 흐림 ☁️
                    - `RAIN`: 비 🌧️
                    - `RAIN_SNOW`: 비/눈 🌧️❄️
                    - `SNOW`: 눈 ❄️
                    - `SHOWER`: 소나기 🌦️
                    - `null`: 날씨 예보 수신 전
                    
                    **4. 단위 안내**
                    - `temperature`: 기온 (°C)
                    - `humidity`: 습도 (%)
                    - `precipProbability`: 강수확률 (%)
                    - `precipitation`: 강수량 (mm)
                    """
    )
    public ResponseEntity<ApiResponse<WeatherCompositeResponse>> getWeatherSummary() {
        WeatherCompositeResponse result = weatherQueryService.getWeatherSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "AI 날씨 브리핑 조회",
            description = """
                    ### 🤖 AI 날씨 브리핑 조회 안내
                    
                    **1. 파라미터 상세 안내**
                    - `location`: 날씨 지역 코드 (기본값: `ANSAN`)
                    - `dateTime`: 조회하고 싶은 특정 날씨 시각 (ISO-8601 포맷: `YYYY-MM-DDTHH:mm:ss`, 예: `2026-08-04T14:00:00`)
                    
                    **2. 동작 방식**
                    - `dateTime` **미입력 시 (기본)**: 가장 최근에 생성된 최신 AI 날씨 브리핑 1건을 조회합니다.
                    - `dateTime` **입력 시**: 입력받은 시각의 정각(`14:00:00`)에 생성된 AI 날씨 브리핑을 조회합니다.
                    """
    )
    @GetMapping("/briefing")
    public ResponseEntity<ApiResponse<WeatherBriefingResponse>> getWeatherBriefing(
            @RequestParam(defaultValue = "ANSAN") String location,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime
    ) {
        WeatherBriefingResponse response = weatherBriefingService.getBriefing(location, dateTime);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
