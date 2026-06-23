package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.service.CacheService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存缓存服务实现
 * 替代 Redis，适合本地开发测试
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.cache.type", havingValue = "memory", matchIfMissing = true)
public class MemoryCacheServiceImpl implements CacheService {

    /** 线程安全的缓存存储 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 清理过期缓存的调度器 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MemoryCacheServiceImpl() {
        // 每5分钟清理一次过期缓存
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.MINUTES);
        log.info("内存缓存服务已启动");
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, 3600); // 默认1小时过期
    }

    @Override
    public void set(String key, Object value, long timeoutSeconds) {
        try {
            String jsonValue = JSON.toJSONString(value);
            long expirationTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
            cache.put(key, new CacheEntry(jsonValue, expirationTime));
            log.debug("缓存已设置 - key: {}, timeout: {}s", key, timeoutSeconds);
        } catch (Exception e) {
            log.error("缓存设置失败 - key: {}", key, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return JSON.parseObject(entry.value, type);
        } catch (Exception e) {
            log.error("缓存获取失败 - key: {}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            cache.remove(key);
            log.debug("缓存已删除 - key: {}", key);
        } catch (Exception e) {
            log.error("缓存删除失败 - key: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            if (entry.isExpired()) {
                cache.remove(key);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("缓存检查失败 - key: {}", key, e);
            return false;
        }
    }

    @Override
    public void setString(String key, String value) {
        setString(key, value, 3600); // 默认1小时过期
    }

    @Override
    public void setString(String key, String value, long timeoutSeconds) {
        try {
            long expirationTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
            cache.put(key, new CacheEntry(value, expirationTime));
            log.debug("字符串缓存已设置 - key: {}, timeout: {}s", key, timeoutSeconds);
        } catch (Exception e) {
            log.error("字符串缓存设置失败 - key: {}", key, e);
        }
    }

    @Override
    public String getString(String key) {
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        } catch (Exception e) {
            log.error("字符串缓存获取失败 - key: {}", key, e);
            return null;
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("已清理 {} 个过期缓存项", removed);
        }
    }

    /**
     * 缓存条目内部类
     */
    private static class CacheEntry {
        final String value;
        final long expirationTime;

        CacheEntry(String value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
