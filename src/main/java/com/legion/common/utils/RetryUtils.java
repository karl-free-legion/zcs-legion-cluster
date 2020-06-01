package com.legion.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class RetryUtils {
    private static final int RETRY = 3;
    private static final long DELAY = 1000l;

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    /**
     * 重试调用
     *
     * @param callable  方法
     * @param throwable 自定义异常
     * @param message   自定义异常消息
     * @param <V>       返回类型
     * @return
     */
    public static <V> V retry(Callable<V> callable, Throwable throwable, String message) {
        return retryLogics(callable, throwable, message, RETRY, DELAY);
    }

    /**
     * 重试调用
     *
     * @param callable  方法
     * @param throwable 自定义异常
     * @param message   自定义异常消息
     * @param <V>       返回类型
     * @param retry     重试次数
     * @param delay     每次调用间隔
     * @return
     */
    public static <V> V retry(Callable<V> callable, Throwable throwable, String message, int retry, int delay) {
        return retryLogics(callable, throwable, message, retry, delay);
    }


    private static <T> T retryLogics(Callable<T> callable, Throwable throwable, String message, int retry, long delay) {
        AtomicInteger counter = new AtomicInteger();
        while (counter.incrementAndGet() < retry) {
            try {
                return callable.call();
            } catch (Exception e) {
                log.error("retry {} / {}, {}", counter, RETRY, message, e);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new RuntimeException(throwable);
    }
}
