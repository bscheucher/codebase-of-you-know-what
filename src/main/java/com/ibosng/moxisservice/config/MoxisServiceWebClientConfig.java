package com.ibosng.moxisservice.config;

import com.ibosng.moxisservice.exceptions.MoxisAuthException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Getter
public class MoxisServiceWebClientConfig {

    @Value("${moxis.username}")
    private String moxisUsername;

    @Value("${moxis.password}")
    private String moxisPassword;

    @Value("${moxis.url}")
    private String moxisUrl;

    @Bean(name = "moxisservicewebclient")
    public WebClient webClient() {
        try {
            return WebClient.builder()
                    .baseUrl(getMoxisUrl())
                    .defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(getMoxisUsername(), getMoxisPassword()))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new MoxisAuthException("Authentication has failed!", e);
        }
    }

}
