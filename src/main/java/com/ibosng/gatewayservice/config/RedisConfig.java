package com.ibosng.gatewayservice.config;

import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SslVerificationMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic RedissonClient config kept in place for data codec configuration, which cannot be defined via application properties.
 * A YAML file could be used for file-based configuration, see: <a href="https://redisson.pro/docs/configuration/">...</a>
 * Note: Removed unused "redisSslEnabled" property.
 * */
@Configuration("lhrRedisConfig")
public class RedisConfig {

    @Getter
    @Value("${spring.data.redis.host}")
    private String azureRedisHost;

    @Getter
    @Value("${spring.data.redis.password}")
    private String azureRedisPassword;

    @Getter
    @Value("${spring.data.redis.port}")
    private String azureRedisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());
        String redisAddress = String.format("redis://%s:%s", getAzureRedisHost(), getAzureRedisPort());
        config.useSingleServer()
                .setAddress(redisAddress)
                .setSslVerificationMode(SslVerificationMode.NONE);

        // Read properties from your configuration/environment
        String host = getAzureRedisHost();
        String port = getAzureRedisPort();
        String password = getAzureRedisPassword();

        if (host == null || host.isBlank() || port == null || port.isBlank()) {
            throw new IllegalStateException("Redis host/port not configured. Provide REDIS host/port or disable Redis usage.");
        }

        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }
}
