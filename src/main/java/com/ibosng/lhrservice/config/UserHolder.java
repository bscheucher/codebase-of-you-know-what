package com.ibosng.lhrservice.config;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;
import static com.ibosng.lhrservice.utils.Constants.LHR_SERVICE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Component
public class UserHolder {
    private String username;
    private Integer userId;

    public String getUsername() {
        if (isNullOrBlank(username)) {
            return LHR_SERVICE;
        }
        return username;
    }

    @PreDestroy
    private void clearThreadLocal() {
        ThreadLocal<UserHolder> userHolderThreadLocal = new ThreadLocal<>();
        userHolderThreadLocal.remove();
    }
}
