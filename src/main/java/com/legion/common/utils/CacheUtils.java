package com.legion.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.legion.core.api.X;
import com.legion.core.exception.ExConstants;
import com.legion.core.exception.LegionException;
import io.reactivex.SingleEmitter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * CacheUtils缓存SingleEmitter
 *
 * @author lance
 * 10/21/2019 15:34
 */
@Slf4j
public final class CacheUtils {
    private static Cache<String, SingleEmitter<X.XMessage>> cache;

    static {
        //缓存容量和超时时间需要配置
        cache = CacheBuilder.newBuilder()
                .maximumSize(102400L)
                .expireAfterAccess(Duration.ofSeconds(20L))
                .removalListener(removal -> {
                    if (removal.wasEvicted()) {
                        SingleEmitter<X.XMessage> emitter = (SingleEmitter<X.XMessage>) removal.getValue();
                        //如果没有缓存, 则不予处理(@Modify 2019.10.21)
                        if (emitter == null) {
                            return;
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("wait timeout, traceId={}", removal.getKey());
                        }

                        emitter.tryOnError(LegionException.valueOf(ExConstants.CONNECT_TIME_OUT));
                    }
                })
                .build();
    }

    private CacheUtils() {
    }

    /**
     * 根据key获取promise对象
     *
     * @param key key
     * @return Promise
     */
    public static SingleEmitter<X.XMessage> getEmitter(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 设置缓存
     *
     * @param key     key
     * @param emitter emitter
     */
    public static void putEmitter(String key, SingleEmitter<X.XMessage> emitter) {
        cache.put(key, emitter);
    }

    /**
     * 移除缓存
     *
     * @param key key
     */
    public static void removeEmitter(String key) {
        cache.invalidate(key);
    }
}
