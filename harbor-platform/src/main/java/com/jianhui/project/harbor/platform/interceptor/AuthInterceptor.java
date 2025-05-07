package com.jianhui.project.harbor.platform.interceptor;

import com.alibaba.fastjson.JSON;
import com.jianhui.project.harbor.common.util.JwtUtil;
import com.jianhui.project.harbor.platform.config.props.JwtProperties;
import com.jianhui.project.harbor.platform.enums.ResultCode;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@AllArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        String token = request.getHeader("accessToken");
        if(token == null || token.isEmpty()){
            log.error("未登录，url:{}", request.getRequestURI());
            throw new GlobalException(ResultCode.NO_LOGIN);
        }
        String jsonstr = JwtUtil.getInfo(token);
        UserSession userSession = JSON.parseObject(jsonstr, UserSession.class);

        //验证token
        if(!JwtUtil.checkSign(token,jwtProperties.getAccessTokenSecret())){
            log.error("token已失效，用户:{}", userSession.getUsername());
            log.error("token:{}", token);
            throw new GlobalException(ResultCode.INVALID_TOKEN);
        }
        request.setAttribute("userSession", userSession);
        return true;
    }
}
