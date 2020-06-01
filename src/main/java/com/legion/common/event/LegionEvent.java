package com.legion.common.event;


/**
 * Create At 2019/8/20
 * 本接口子类定义了EventBus消费的事件 ,由于枚举类型则不可扩展，所以事件枚举需要实现这个接口
 * 每条枚举调用getEventStr 获得事件的topic
 * 所以订阅者Topic与事件枚举转换成string一致
 * 事件发布可以直接使用 <code>EventBus.pushEvent(LegionEvent event, T message)</code>
 * <p>
 * 注解中不可以使用自定义类型，所以订阅者topic 使用String作为Topic 消息订阅者参考：
 *
 * @author Zing
 * @version 0.0.1
 * @see com.legion.node.controller.ServerNodeStateEventListener
 */
public interface LegionEvent {
    String getEventStr();
}
