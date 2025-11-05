package com.ibosng.moxisservice.config;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;
import static com.ibosng.moxisservice.utils.Constants.MOXIS_SERVICE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserHolder {
    private String username;
    private Integer userId;

    public String getUsername() {
        if(isNullOrBlank(username)) {
            return MOXIS_SERVICE;
        }
        return username;
    }

    @PreDestroy
    private void clearThreadLocal() {
        ThreadLocal<UserHolder> userHolderThreadLocal = new ThreadLocal<>();
        userHolderThreadLocal.remove();
    }
}
