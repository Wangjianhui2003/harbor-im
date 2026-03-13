package com.jianhui.project.harbor.platform.aspect;

import com.jianhui.project.harbor.platform.annotation.RepeatSubmit;
import com.jianhui.project.harbor.platform.constant.RedisKey;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        UserSession session = SessionContext.getSession();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String key = RedisKey.IM_REPEAT_SUBMIT + ":" + session.getUserId() + ":" + request.getRequestURI();
        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, 1,
                        repeatSubmit.interval(), repeatSubmit.timeUnit()));

        if (!acquired) {
            throw new GlobalException(repeatSubmit.message());
        }
        return joinPoint.proceed();
    }
}
