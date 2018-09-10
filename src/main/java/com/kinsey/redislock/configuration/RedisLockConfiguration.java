package com.kinsey.redislock.configuration;

import com.kinsey.redislock.lock.DistributedLock;
import com.kinsey.redislock.lock.RedisDistributedLock;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Created by zj on 2018/9/6
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisLockConfiguration {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public DistributedLock redisDistributedLock(RedisTemplate<Object, Object> redisTemplate){
        return new RedisDistributedLock(redisTemplate);
    }
}
