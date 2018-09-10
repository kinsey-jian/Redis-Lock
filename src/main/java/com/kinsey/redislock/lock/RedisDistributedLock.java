package com.kinsey.redislock.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by zj on 2018/9/6
 */
@Slf4j
public class RedisDistributedLock extends AbstractDistributedLock {

    private RedisTemplate<Object, Object> redisTemplate;

    private ThreadLocal<String> lockFlag = new ThreadLocal<>();

    private static final String UNLOCK_LUA;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if redis.call(\"get\",KEYS[1]) == ARGV[1] ");
        sb.append("then ");
        sb.append("    return redis.call(\"del\",KEYS[1]) ");
        sb.append("else ");
        sb.append("    return 0 ");
        sb.append("end ");
        UNLOCK_LUA = sb.toString();
    }

    public RedisDistributedLock(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean lock(String key, long expire, int retryTimes, long sleepMillis) {
        boolean result = setRedis(key, expire);

        if ((!result) && retryTimes-- > 0) {
            try {
                log.debug("lock failed, retrying..." + retryTimes);
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                return false;
            }
            result = setRedis(key, expire);
        }
        return result;
    }

    @Override
    public boolean releaseLock(String key) {
        // 释放锁的时候，有可能因为持锁之后方法执行时间大于锁的有效期，此时有可能已经被另外一个线程持有锁，所以不能直接删除
        try {
            String args = lockFlag.get();
            String keyAndArgs = key + args;

            // 使用lua脚本删除redis中匹配value的key，可以避免由于方法执行时间过长而redis锁自动过期失效的时候误删其他线程的锁
            // spring自带的执行脚本方法中，集群模式直接抛出不支持执行脚本的异常，所以只能拿到原redis的connection来执行脚本
            Long result = redisTemplate.execute(new RedisCallback<Long>() {
                public Long doInRedis(RedisConnection connection) throws DataAccessException {
                    return connection.eval(UNLOCK_LUA.getBytes(), ReturnType.INTEGER, key.getBytes().length, keyAndArgs.getBytes());
                }
            });

            return result > 0;
        } catch (Exception e) {
            log.error("release lock occured an exception", e);
        } finally {
            // 清除掉ThreadLocal中的数据，避免内存溢出
            lockFlag.remove();
        }
        return false;
    }

    private boolean setRedis(String key, long expire) {
        try {
            Boolean result = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                String uuid = UUID.randomUUID().toString();
                lockFlag.set(uuid);
                return connection.set(key.getBytes(), uuid.getBytes(), Expiration.milliseconds(expire), RedisStringCommands.SetOption.SET_IF_ABSENT);
            });
            return Objects.nonNull(result);
        } catch (Exception e) {
            log.error("set redis occured an exception", e);
        }
        return false;
    }
}
