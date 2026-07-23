package life.hanyang.core.partnership.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MerchantCategory {
    RESTAURANT("food"),
    CAFE("cafe"),
    PUB("pub"),
    PLAY("play"),
    LIFE("life");

    private final String value;

    MerchantCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MerchantCategory fromValue(String value) {
        for (MerchantCategory category : MerchantCategory.values()) {
            if (category.getValue().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown merchant category: " + value);
    }
}