package com.guolian.pm.common.cache;

/**
 * 缓存抽象。开发档用内存实现，生产档用 Redis 实现，两者均强制 TTL。
 */
public interface CacheService {

    <T> T get(String key, Class<T> type);

    void put(String key, Object value, long ttlSeconds);

    void evict(String key);

    void evictByPrefix(String prefix);
}
