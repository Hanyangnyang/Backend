package life.hanyang.core.weather.domain;

import lombok.Getter;

@Getter
public enum WeatherCondition {
    SUNNY("맑음"),
    MOSTLY_CLOUDY("구름많음"),
    CLOUDY("흐림"),
    RAIN("비"),
    RAIN_SNOW("비/눈"),
    SNOW("눈"),
    SHOWER("소나기");

    private final String label;

    WeatherCondition(String label) {
        this.label = label;
    }

    public static WeatherCondition from(Integer sky, Integer pty) {
        if (pty != null && pty > 0) {
            return switch (pty) {
                case 1 -> RAIN;
                case 2 -> RAIN_SNOW;
                case 3 -> SNOW;
                case 4 -> SHOWER;
                default -> RAIN;
            };
        }
        if (sky != null) {
            return switch (sky) {
                case 1 -> SUNNY;
                case 3 -> MOSTLY_CLOUDY;
                case 4 -> CLOUDY;
                default -> SUNNY;
            };
        }
        return null;
    }
}
