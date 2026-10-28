package com.guolian.pm.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存缓存：开发档默认实现，所有条目强制 TTL，避免脏权限长期驻留。
 */
@Component
@ConditionalOnProperty(name = "app.cache.type", havingValue = "memory", matchIfMissing = true)
public class MemoryCacheService implements CacheService {

    private record Entry(Object value, long expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() < System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return (T) entry.value();
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
