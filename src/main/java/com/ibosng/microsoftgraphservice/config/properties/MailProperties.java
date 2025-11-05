package com.ibosng.microsoftgraphservice.config.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@EqualsAndHashCode(callSuper = true)
@Data
@Component
public class MailProperties extends AzureProperties {
    @Value("${mailUserId:#{null}}")
    private String userId;

    public MailProperties(
            @Value("${mail.clientId}") String clientId,
            @Value("${mail.clientSecret}") String clientSecret,
            @Value("${mail.tenantId}") String tenantId,
            @Value("${mail.scope}") String scope) {
        super(clientId, clientSecret, tenantId, scope);
    }
}
