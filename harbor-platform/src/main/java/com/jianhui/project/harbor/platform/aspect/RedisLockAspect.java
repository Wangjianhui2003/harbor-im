package com.jianhui.project.harbor.platform.aspect;

import com.jianhui.project.harbor.platform.annotation.RedisLock;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class RedisLockAspect {

    private final RedissonClient redissonClient;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String spelKey = parseSpelKey(joinPoint, redisLock.key());
        String lockKey = redisLock.prefixKey() + ":" + spelKey;

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired;
        if (redisLock.waitTime() == -1) {
            acquired = lock.tryLock();
        } else {
            acquired = lock.tryLock(redisLock.waitTime(), redisLock.unit());
        }
        if (!acquired) {
            throw new GlobalException("请求频繁，请稍后再试");
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String parseSpelKey(ProceedingJoinPoint joinPoint, String spelExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return PARSER.parseExpression(spelExpression).getValue(context, String.class);
    }
}
