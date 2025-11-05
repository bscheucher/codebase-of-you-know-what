package com.ibosng.lhrservice.config;

import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.services.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.logging.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {
    private final static String USER_ID_KEY = "user_id";
    private final static String USER_HEADER = "auth-user-id";

    private final ThreadLocal<UserHolder> userHolderThreadLocal;
    private final BenutzerService benutzerService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader(USER_HEADER);
        if(!NumberUtils.isParsable(userId)) {
            return true;
        }
        Integer userIdInt = NumberUtils.toInt(userId, 0);
        Benutzer benutzer = benutzerService.findById(userIdInt).orElse(null);
        if (benutzer != null) {
            UserHolder userHolder = userHolderThreadLocal.get();
            userHolder.setUsername(benutzer.getEmail());
            userHolder.setUserId(benutzer.getId());
            MDC.put(USER_ID_KEY, benutzer.getId());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(USER_ID_KEY);
        userHolderThreadLocal.remove();
    }
}
