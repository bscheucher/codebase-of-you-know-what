package com.ibosng.natifservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Getter
@Configuration
public class NatifServiceWebClientConfig {

    @Value("${natifBaseEndpoint:#{null}}")
    private String natifBaseEndpoint;

    @Value("${natifApiKey:#{null}}")
    private String natifApiKey;

    @Bean(name = "natifservicewebclient")
    public WebClient webClient() {
        try {
            return WebClient.builder()
                    .baseUrl(getNatifBaseEndpoint())
                    .defaultHeaders(httpHeaders -> httpHeaders.add(AUTHORIZATION, getNatifApiKey()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
