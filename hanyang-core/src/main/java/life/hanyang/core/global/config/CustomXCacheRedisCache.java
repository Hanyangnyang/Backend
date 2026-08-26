package life.hanyang.core.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class CustomXCacheRedisCache extends RedisCache {

    public static final String X_CACHE_ATTRIBUTE = "X_CACHE_STATUS";
    private final RedisCacheWriter cacheWriter;

    public CustomXCacheRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
        this.cacheWriter = cacheWriter;
    }

    @Override
    public void clear() {
        byte[] pattern = (getName() + "::*").getBytes(StandardCharsets.UTF_8);
        cacheWriter.clean(getName(), pattern);
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
        ValueWrapper wrapper = super.get(key);
        if (wrapper != null) {
            setXCacheHeader(true);
            return (T) wrapper.get();
        }
        setXCacheHeader(false);
        return super.get(key, valueLoader);
    }

    private void setXCacheHeader(boolean hit) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
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
