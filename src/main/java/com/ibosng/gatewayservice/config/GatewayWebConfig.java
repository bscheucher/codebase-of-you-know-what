package com.ibosng.gatewayservice.config;

import com.ibosng.gatewayservice.services.BenutzerDetailsService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class GatewayWebConfig implements WebMvcConfigurer {

    @Autowired
    @Lazy
    private BenutzerDetailsService benutzerDetailsService;

    @Getter
    @Value("${restTemplateConnectTimeout:5000}")  // Default to 5000 milliseconds if not set
    private Integer restTemplateConnectTimeout;

    @Getter
    @Value("${restTemplateReadTimeout:5000}")  // Default to 5000 milliseconds if not set
    private Integer restTemplateReadTimeout;

    @Bean(name = "gatewayRestTemplate")
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(getRestTemplateConnectTimeout());
        factory.setReadTimeout(getRestTemplateReadTimeout());
        return new RestTemplate();
    }


    @Bean
    public FilterRegistrationBean<CorsFilter> simpleCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000", "https://ibosng.myspock.work/", "https://ibosngfrontendprod.azurewebsites.net/", "https://ibosngfrontendpreprod.azurewebsites.net/", "https://ibosngfrontendqa.azurewebsites.net/", "https://ibosngfrontenddev.azurewebsites.net/"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Collections.singletonList("Content-Disposition"));
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}

