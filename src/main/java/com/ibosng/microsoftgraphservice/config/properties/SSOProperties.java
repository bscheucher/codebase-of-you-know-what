package com.ibosng.microsoftgraphservice.config.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@EqualsAndHashCode(callSuper = true)
@Data
public class SSOProperties extends AzureProperties {

    public SSOProperties(
            @Value("${sso.clientId}") String clientId,
            @Value("${sso.clientSecret}") String clientSecret,
            @Value("${sso.tenantId}") String tenantId,
            @Value("${mail.scope}") String scope) {
        super(clientId, clientSecret, tenantId, scope);
    }
}
