package com.aiwechat.aichat.service;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置缓存（带过期时间）
     *
     * @param key            缓存键
     * @param value          缓存值
     * @param timeoutSeconds 过期时间（秒）
     */
    void set(String key, Object value, long timeoutSeconds);

    /**
     * 获取缓存
     *
     * @param key  缓存键
     * @param type 目标类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置字符串值
     *
     * @param key   缓存键
     * @param value 字符串值
     */
    void setString(String key, String value);

    /**
     * 设置字符串值（带过期时间）
     *
     * @param key            缓存键
     * @param value          字符串值
     * @param timeoutSeconds 过期时间（秒）
     */
    void setString(String key, String value, long timeoutSeconds);

    /**
     * 获取字符串值
     *
     * @param key 缓存键
     * @return 字符串值
     */
    String getString(String key);
}
