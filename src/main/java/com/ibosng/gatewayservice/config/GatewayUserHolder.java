package com.ibosng.gatewayservice.config;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;
import static com.ibosng.gatewayservice.utils.Constants.GATEWAY_SERVICE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Component
public class GatewayUserHolder {
    private String username;
    private Integer userId;

    public String getUsername() {
        if (isNullOrBlank(username)) {
            return GATEWAY_SERVICE;
        }
        return username;
    }

    @PreDestroy
    private void clearThreadLocal() {
        ThreadLocal<GatewayUserHolder> userHolderThreadLocal = new ThreadLocal<>();
        userHolderThreadLocal.remove();
    }
}
