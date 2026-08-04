package life.hanyang.core.weather.service;

import life.hanyang.core.weather.domain.HourlyWeather;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WeatherPromptBuilder {

    public String buildPrompt(List<HourlyWeather> hourlyWeathers, LocalDateTime baseTime) {
        if (hourlyWeathers == null || hourlyWeathers.isEmpty()) {
            throw new IllegalArgumentException("날씨 데이터 목록이 비어있습니다.");
        }

        int hour = baseTime.getHour();

        // 17시 이후부터는 현재 날씨 + 내일 날씨 대비 브리핑 모드로 전환
        if (hour >= 17) {
            return buildTomorrowPrompt(hourlyWeathers, baseTime);
        } else {
            return buildTodayPrompt(hourlyWeathers, baseTime);
        }
    }

    // ☀️ [17시 이전] 오늘 하루 날씨 중심 프롬프트
    private String buildTodayPrompt(List<HourlyWeather> hourlyWeathers, LocalDateTime baseTime) {
        int hour = baseTime.getHour();
        boolean isWeekend = isWeekend(baseTime);
        TimeContext context = getTimeContext(hour, isWeekend);

        HourlyWeather current = hourlyWeathers.get(0);
        double currentTemp = current.getTemperature() != null ? current.getTemperature() : 0.0;

        LocalDate today = baseTime.toLocalDate();
        List<HourlyWeather> todayWeathers = hourlyWeathers.stream()
                .filter(h -> h.getForecastAt() != null && h.getForecastAt().toLocalDate().equals(today))
                .toList();

        if (todayWeathers.isEmpty()) {
            todayWeathers = hourlyWeathers;
        }

        double maxTemp = todayWeathers.stream()
                .filter(h -> h.getTemperature() != null)
                .mapToDouble(HourlyWeather::getTemperature)
                .max().orElse(currentTemp);

        double minTemp = todayWeathers.stream()
                .filter(h -> h.getTemperature() != null)
                .mapToDouble(HourlyWeather::getTemperature)
                .min().orElse(currentTemp);

        int maxPrecipProb = todayWeathers.stream()
                .mapToInt(h -> h.getPrecipProbability() != null ? h.getPrecipProbability() : 0)
                .max().orElse(0);

        boolean hasRainOrSnowLater = maxPrecipProb >= 30;

        String currentRainStatus = (current.getRainState() != null && current.getRainState() > 0)
                ? "비 또는 눈 내리는 중" : "없음";

        String rainForecastStatus = hasRainOrSnowLater
                ? String.format("있음 (오늘 남은 시간 최고 강수확률 %d%%, 우산을 꼭 챙기도록 조언해줘)", maxPrecipProb)
                : "없음";

        String weatherLabel = current.getWeatherCondition() != null ? current.getWeatherCondition() : "정보 없음";

        String schoolNoticeRule = isWeekend
                ? "- 오늘은 주말이므로 학교, 등교, 하교, 수업, 출근, 퇴근 관련 표현을 절대로 사용하지 말 것\n"
                : "";

        return """
                너는 날씨 앱의 AI 어시스턴트야. 아래 [오늘 날씨 데이터]를 바탕으로 한국 대학생에게 친근하고 자연스러운 한국어로 오늘 날씨 코멘트를 한 문장으로 작성해줘.

                현재 시간대: %s (%d시)
                맥락 가이드: %s

                현재 기온: %.1f°C (오늘 최고 %.1f°C / 최저 %.1f°C)
                날씨 상태: %s
                미세먼지: %s / 초미세먼지: %s / 자외선: %s
                현재 강수 여부: %s
                오늘 중 비/눈 예보 여부: %s

                규칙:
                - 한 문장으로 매우 짧고 간결하게 작성할 것 (대략 30~50자 이내)
                - 실용적인 조언(외투, 우산, 자외선차단제 등)을 자연스럽게 포함
                - 이모지 사용 금지, 반말 금지, 친근한 존댓말 사용
                - 문장 부호로만 끝낼 것 (마침표 또는 느낌표)
                %s- 오직 코멘트 문장만 출력, 다른 말 하지 말 것
                """.formatted(
                context.label(), hour, context.guide(),
                currentTemp, maxTemp, minTemp,
                weatherLabel,
                getGradeLabel(current.getPm10Grade()),
                getGradeLabel(current.getPm25Grade()),
                getUvLabel(current.getUvIndex()),
                currentRainStatus,
                rainForecastStatus,
                schoolNoticeRule
        );
    }

    // 🌙 [17시 이후] 현재 날씨 + 내일 날씨 대비 프롬프트
    private String buildTomorrowPrompt(List<HourlyWeather> hourlyWeathers, LocalDateTime baseTime) {
        int hour = baseTime.getHour();
        boolean isTodayWeekend = isWeekend(baseTime);
        boolean isTomorrowWeekend = isWeekend(baseTime.plusDays(1));

        HourlyWeather current = hourlyWeathers.get(0);
        double currentTemp = current.getTemperature() != null ? current.getTemperature() : 0.0;

        LocalDate tomorrow = baseTime.toLocalDate().plusDays(1);
        List<HourlyWeather> tomorrowWeathers = hourlyWeathers.stream()
                .filter(h -> h.getForecastAt() != null && h.getForecastAt().toLocalDate().equals(tomorrow))
                .toList();

        boolean hasTomorrowData = !tomorrowWeathers.isEmpty();
        List<HourlyWeather> targetWeathers = hasTomorrowData ? tomorrowWeathers : hourlyWeathers;

        double tomorrowMaxTemp = targetWeathers.stream()
                .filter(h -> h.getTemperature() != null)
                .mapToDouble(HourlyWeather::getTemperature)
                .max().orElse(currentTemp);

        double tomorrowMinTemp = targetWeathers.stream()
                .filter(h -> h.getTemperature() != null)
                .mapToDouble(HourlyWeather::getTemperature)
                .min().orElse(currentTemp);

        int tomorrowMaxPrecipProb = targetWeathers.stream()
                .mapToInt(h -> h.getPrecipProbability() != null ? h.getPrecipProbability() : 0)
                .max().orElse(0);

        boolean hasRainOrSnowTomorrow = tomorrowMaxPrecipProb >= 30;
        String tomorrowRainStatus = hasRainOrSnowTomorrow
                ? String.format("있음 (최고 강수확률 %d%%, 우산 챙기도록 조언 포함)", tomorrowMaxPrecipProb)
                : "없음";

        String currentWeatherLabel = current.getWeatherCondition() != null ? current.getWeatherCondition() : "정보 없음";

        String eveningContextGuide = getEveningContextGuide(isTodayWeekend, isTomorrowWeekend);

        String missingTomorrowNotice = !hasTomorrowData
                ? "- 현재 내일 예보 데이터가 없으므로 '내일'이라는 단어나 표현을 절대로 사용하지 말고, 현시점 밤 기온과 날씨에 맞춘 저녁 인사만 작성할 것\n"
                : "";

        String schoolNoticeRule = (isTodayWeekend || isTomorrowWeekend)
                ? "- 오늘 또는 내일이 주말이므로 학교, 등교, 하교, 수업, 출근, 퇴근 관련 표현을 절대로 사용하지 말 것\n"
                : "";

        return """
                너는 날씨 앱의 AI 어시스턴트야. 아래 [현재 날씨 및 예보 데이터]를 바탕으로 한국 대학생에게 따뜻한 저녁/밤 인사와 함께 날씨 대비 코멘트를 한 문장으로 작성해줘.

                현재 시간대: 저녁/밤 시간대 (%d시)
                현재 날씨: %.1f°C (%s)

                [향후/내일 예보 정보]
                예보 기온: 최고 %.1f°C / 최저 %.1f°C
                비/눈 예보 여부: %s

                규칙:
                - 한 문장으로 매우 짧고 간결하게 작성할 것 (대략 30~50자 이내)
                - %s
                - 이모지 사용 금지, 반말 금지, 친근한 존댓말 사용
                - 문장 부호로만 끝낼 것 (마침표 또는 느낌표)
                %s%s- 오직 코멘트 문장만 출력, 다른 말 하지 말 것
                """.formatted(
                hour, currentTemp, currentWeatherLabel,
                tomorrowMaxTemp, tomorrowMinTemp,
                tomorrowRainStatus,
                eveningContextGuide,
                missingTomorrowNotice,
                schoolNoticeRule
        );
    }

    private String getEveningContextGuide(boolean isTodayWeekend, boolean isTomorrowWeekend) {
        if (isTodayWeekend && isTomorrowWeekend) {
            // 토요일 저녁 -> 일요일 내일
            return "즐거운 토요일 저녁 인사와 함께 편안한 일요일 주말 날씨 대비 조언을 포함해줘 (학교, 수업, 등교, 출근 언급 절대 금지).";
        } else if (!isTodayWeekend && isTomorrowWeekend) {
            // 금요일 저녁 -> 토요일 내일
            return "한 주 동안 수고했다는 금요일 저녁 인사와 함께 내일 주말 날씨 대비 조언을 포함해줘 (학교, 수업, 등교, 출근 언급 절대 금지).";
        } else if (isTodayWeekend && !isTomorrowWeekend) {
            // 일요일 저녁 -> 월요일 내일
            return "편안한 주말 마무리 인사와 함께 내일(월요일) 등교/출근 날씨 대비(외투, 우산 등) 조언을 포함해줘.";
        } else {
            // 월~목 저녁 -> 평일 내일
            return "오늘 하루 수고했다는 따뜻한 저녁 인사와 함께 내일 등교/출근 날씨 대비(외투, 우산 등) 조언을 포함해줘.";
        }
    }

    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private TimeContext getTimeContext(int hour, boolean isWeekend) {
        if (isWeekend) {
            return new TimeContext("휴일/주말", "오늘은 편안한 주말입니다. 학교, 등교, 하교, 출근, 퇴근 관련 언급은 절대 하지 말고 오늘 주말 날씨 팁을 친근하게 알려줘.");
        }

        if (hour >= 5 && hour < 12) {
            return new TimeContext("아침/등교 시간대", "상쾌한 아침 등교길 인사와 함께 오늘 하루 전반적인 날씨 대비 요령을 조언해줘.");
        } else {
            return new TimeContext("낮/활동 시간대", "활기찬 낮 일과 중 조언과 함께 자외선, 미세먼지 등 실외 활동 대비 요령을 조언해줘.");
        }
    }

    private record TimeContext(String label, String guide) {}

    private String getGradeLabel(Integer grade) {
        if (grade == null) return "보통";
        return switch (grade) {
            case 1 -> "좋음";
            case 2 -> "보통";
            case 3 -> "나쁨";
            case 4 -> "매우나쁨";
            default -> "보통";
        };
    }

    private String getUvLabel(Integer uvIndex) {
        if (uvIndex == null) return "보통";
        if (uvIndex <= 2) return "낮음";
        if (uvIndex <= 5) return "보통";
        if (uvIndex <= 7) return "높음";
        return "매우높음";
    }
}
