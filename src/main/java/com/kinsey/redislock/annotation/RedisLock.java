package com.kinsey.redislock.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Created by zj on 2018/9/6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    @AliasFor("key")
    String value() default "default";

    @AliasFor("value")
    String key() default "default";

    /** 持续锁时间，单位ms */
    long keepMills() default 10000;

    /** 当获取失败时候动作 */
    LockFailAction action() default LockFailAction.CONTINUE;

    enum LockFailAction{
        /** 放弃 */
        GIVEUP,
        /** 继续 */
        CONTINUE;
    }

    /** 重试的间隔时间,设置GIVEUP忽略此项 */
    long sleepMills() default 200;

    /** 重试次数 */
    int retryTimes() default 3;
}
