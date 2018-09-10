package com.kinsey.redislock.aspect;

import com.kinsey.redislock.annotation.RedisLock;
import com.kinsey.redislock.configuration.RedisLockConfiguration;
import com.kinsey.redislock.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * Created by zj on 2018/9/6
 */
@Aspect
@Slf4j
@Configuration
@ConditionalOnClass(DistributedLock.class)
@AutoConfigureAfter(RedisLockConfiguration.class)
public class DistributedLockAspect {

    @Autowired
    private DistributedLock distributedLock;

    private ExpressionParser parser = new SpelExpressionParser();

    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Pointcut("@annotation(LockAction)")
    private void lockPoint() {
    }

    @Around("lockPoint()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RedisLock lockAction = method.getAnnotation(RedisLock.class);
        String key = lockAction.value();
        Object[] args = pjp.getArgs();
        key = parse(key, method, args);

        int retryTimes = lockAction.action().equals(RedisLock.LockFailAction.CONTINUE) ? lockAction.retryTimes() : 0;
        boolean lock = distributedLock.lock(key, lockAction.keepMills(), retryTimes, lockAction.sleepMills());
        if (!lock) {
            log.debug("get lock failed : " + key);
            return null;
        }

        //得到锁,执行方法，释放锁
        log.debug("get lock success : " + key);
        try {
            return pjp.proceed();
        } catch (Exception e) {
            log.error("execute locked method occured an exception", e);
            throw e;
        } finally {
            boolean releaseResult = distributedLock.releaseLock(key);
            log.debug("release lock : " + key + (releaseResult ? " success" : " failed"));
        }
    }

    private String parse(String key, Method method, Object[] args) {
        String[] params = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(params[i], args[i]);
        }
        return parser.parseExpression(key).getValue(context, String.class);
    }
}
