package com.jianhui.project.harbor.platform.session;


import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SessionContext {

    /**
     * 拿到UserSession
     */
    public static UserSession getSession(){
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (UserSession) requestAttributes.getRequest().getAttribute("userSession");
    }
}
