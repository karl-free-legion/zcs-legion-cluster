package com.legion.connector;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class SchedulerTaskPool {
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    private static ConcurrentHashMap<String, Runnable> tasks = new ConcurrentHashMap<>();


    /**
     * 开始心跳
     *
     * @param name
     * @param r
     */
    public static void addTask(String name, Runnable r) {
        tasks.put(name, r);
        if (isRunning.compareAndSet(false, true)) {
            startAllTask();
        }
    }

    /**
     * 停止心跳
     *
     * @param name
     */
    public static void cancelTask(String name) {
        if (isRunning.get()) {
            tasks.remove(name);
            log.debug("stop task:{} ", name);
            if (tasks.isEmpty()) {
                scheduler.shutdown();
                isRunning.set(false);
            }
        }
    }

    private static void startAllTask() {
        if (scheduler == null || scheduler.isTerminated() || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(4);
        }

        Runnable r = () -> tasks.forEach((n, t) -> {
            t.run();
        });
        // FIXME 改为可配置心跳时间
        repeat(r, 1, TimeUnit.SECONDS);

    }

    /**
     * 延时循环执行
     */
    public static ScheduledFuture repeat(Runnable r, long delay, TimeUnit timeUnit) {
        return scheduler.scheduleWithFixedDelay(r, 0, delay, timeUnit);
    }
}
