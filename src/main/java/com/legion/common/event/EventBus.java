package com.legion.common.event;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.logging.log4j.util.Strings.EMPTY;

/**
 * Order 注解，本类实例化越晚越好
 * create at     2019-08-19 00:22
 *
 * @author zing
 * @version 0.0.1
 */
@Slf4j
@Order
@Component
public class EventBus {

    @Autowired
    private ApplicationContext application;

    private ConcurrentHashMap<String, PublishSubject> eventMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<SubscriberWithOrder>> subscriberMap = new ConcurrentHashMap<>();
    private static EventBus instance;

    public static EventBus getInstance() {
        return instance;
    }

    private EventBus() {
    }

    /**
     * Spring 负责反射实例化
     */
    @PostConstruct
    public void init() {
        instance = this;
        registerAllListener();
    }

    public void registerAllListener() {
        log.info("start register event listener");
        Map<String, Object> listener = application.getBeansWithAnnotation(EventListener.class);
        listener.forEach((name, lis) -> {
            log.info("register {}", name);
            register(lis);
        });
        log.info("register event listener over");
    }


    public void register(Object finder) {
        Arrays.stream(finder.getClass().getMethods())
                .filter(m -> m.getAnnotation(Subscriber.class) != null)
                .peek(m -> log.info("method name:{}", m.getName()))
                .filter(m -> null != getType(m))
                .forEach(method -> {
                    Subscriber subscriber = method.getAnnotation(Subscriber.class);
                    String topic = subscriber.topic();
                    PublishSubject observable = eventMap.get(topic);
                    if (observable == null) {
                        observable = PublishSubject.create();
                    }
                    int order = subscriber.order();
                    SubscriberWithOrder s = new SubscriberWithOrder();
                    s.order = order;
                    s.subscriber = getSubscriber(finder, method);
                    resubscriberTopicByOrder(topic, observable, s);
                    eventMap.put(topic, observable);
                });
    }

    private Consumer getSubscriber(Object caller, Method method) {
        String type = getType(method);
        if (EMPTY.equals(type)) {
            return o -> {
                try {
                    method.invoke(caller);
                } catch (Exception e) {
                    log.warn(type + "failed to execute:", e);
                }
            };
        } else {
            return o -> {
                String name = o.getClass().getCanonicalName();
                if (type.equals(name)) {
                    try {
                        method.invoke(caller, o);
                    } catch (Exception e) {
                        log.warn(name + "failed to execute:", e);

                    }
                }
            };
        }
    }

    /**
     * 根据顺序重新订阅
     *
     * @param topic               topic
     * @param observable          事件发布器
     * @param subscriberWithOrder 事件接收器
     */
    private void resubscriberTopicByOrder(String topic, PublishSubject observable, SubscriberWithOrder subscriberWithOrder) {
        List<SubscriberWithOrder> subs = subscriberMap.get(topic);
        if (subs == null || subs.isEmpty()) {
            subs = new ArrayList<>();
        }
        subs.add(subscriberWithOrder);
        subs.stream()
                .sorted()
                .forEach(s -> {
                    if (s.disposable != null) {
                        s.disposable.dispose();
                    }
                    s.disposable = observable.subscribe(s.subscriber);
                });
        subscriberMap.put(topic, subs);
    }

    public <T> void post(String topic, T message) {
        PublishSubject o = eventMap.get(topic);
        if (o != null) {
            o.onNext(message);
        }
    }

    public static <T> void pushEvent(String topic, T message) {
        instance.post(topic, message);
    }

    public static <T> void pushEvent(String topic) {
        instance.post(topic, new Object());
    }

    public static <T> void pushEvent(LegionEvent topic, T message) {
        pushEvent(topic.getEventStr(), message);
    }

    private String getType(Method method) {
        Parameter[] p = method.getParameters();
        if (p.length == 1) {
            return p[0].getType().getCanonicalName();
        } else if (ArrayUtils.isEmpty(p)) {
            return EMPTY;
        }
        return null;
    }

    @Data
    class SubscriberWithOrder implements Comparable<SubscriberWithOrder> {

        /**
         * 订阅触发顺序
         */
        int order;

        /**
         * 事件接收者
         */
        Consumer subscriber;

        /**
         * 解除订阅
         */
        Disposable disposable;

        @Override
        public int compareTo(SubscriberWithOrder o) {
            assert o != null;
            return Integer.compare(this.order, o.order);
        }
    }

}
