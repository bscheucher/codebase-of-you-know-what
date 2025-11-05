package com.ibosng.gatewayservice.config;

import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.gatewayservice.services.BenutzerDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.ibosng.gatewayservice.utils.Helpers.getTokenFromRequest;

@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {
    private final static String USER_ID_KEY = "user_id";

    private final BenutzerDetailsService benutzerDetailsService;
    private final ThreadLocal<GatewayUserHolder> userHolderThreadLocal;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().equals("/probe") ||
                request.getRequestURI().contains("/swagger-ui") ||
                request.getRequestURI().contains("/ai-engine") ||
                request.getRequestURI().contains("/api-docs")) {
            return true;
        }
        String token = getTokenFromRequest(request);
        Benutzer benutzer = benutzerDetailsService.getUserFromToken(token);
        if (benutzer != null) {
            GatewayUserHolder gatewayUserHolder = userHolderThreadLocal.get();
            gatewayUserHolder.setUsername(benutzer.getEmail());
            gatewayUserHolder.setUserId(benutzer.getId());
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
