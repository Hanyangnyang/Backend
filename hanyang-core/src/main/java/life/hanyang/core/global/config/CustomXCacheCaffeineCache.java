package life.hanyang.core.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.Callable;

public class CustomXCacheCaffeineCache extends CaffeineCache {
    public static final String X_CACHE_ATTRIBUTE = "X_CACHE_STATUS";

    public CustomXCacheCaffeineCache(String name, Cache<Object, Object> cache) {
        super(name, cache);
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = super.get(key);
        setXCacheHeader(wrapper != null);
        return wrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = super.get(key, type);
        setXCacheHeader(value != null);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        boolean[] missed = {false};
        T value = super.get(key, () -> {
            missed[0] = true;
            return valueLoader.call();
        });
        setXCacheHeader(!missed[0]);
        return value;
    }

    private void setXCacheHeader(boolean hit) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String status = hit ? "HIT" : "MISS";
                HttpServletRequest request = attributes.getRequest();
                if (request != null) {
                    request.setAttribute(X_CACHE_ATTRIBUTE, status);
                }
                HttpServletResponse response = attributes.getResponse();
                if (response != null && !response.isCommitted()) {
                    response.setHeader("X-Cache", status);
                }
            }
        } catch (Exception ignored) {
        }

    }
}
