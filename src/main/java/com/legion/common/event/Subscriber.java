package com.legion.common.event;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * create at     2019-08-19 00:07
 *
 * @author zing
 * @version 0.0.1
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Subscriber {
    /**
     * @return 消费主题
     */
    String topic();

    /**
     * @return 消费顺序，在同一主题下生效
     */
    int order() default 0;
}
