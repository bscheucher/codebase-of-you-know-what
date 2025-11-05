package com.ibosng.validationservice.config;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.ibosng.validationservice.utils.Constants.VALIDATION_SERVICE;
import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ValidationUserHolder {
    private String username;
    private Integer userId;

    public String getUsername() {
        if(isNullOrBlank(username)) {
            return VALIDATION_SERVICE;
        }
        return username;
    }

    @PreDestroy
    private void clearThreadLocal() {
        ThreadLocal<ValidationUserHolder> userHolderThreadLocal = new ThreadLocal<>();
        userHolderThreadLocal.remove();
    }
}
