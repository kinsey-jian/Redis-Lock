package com.kinsey.redislock.lock;

/**
 * Created by zj on 2018/9/6
 */
public interface DistributedLock {

    public static final long TIMEOUT_MILLIS = 10000;

    public static final int RETRY_TIMES = 3;

    public static final long SLEEP_MILLIS = 500;

    public boolean lock(String key);

    public boolean lock(String key, int retryTimes);

    public boolean lock(String key, int retryTimes, long sleepMillis);

    public boolean lock(String key, long expire);

    public boolean lock(String key, long expire, int retryTimes);

    public boolean lock(String key, long expire, int retryTimes, long sleepMillis);

    public boolean releaseLock(String key);

}
