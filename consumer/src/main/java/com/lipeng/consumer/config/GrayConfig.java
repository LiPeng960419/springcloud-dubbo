package com.lipeng.consumer.config;

import org.apache.dubbo.config.RegistryConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GrayConfig {

    @Bean("prodRegistryConfig")
    @ConfigurationProperties(prefix = "dubbo.registries.prod")
    public RegistryConfig prodRegistryConfig() {
        return new RegistryConfig();
    }

    @Bean("grayRegistryConfig")
    @ConfigurationProperties(prefix = "dubbo.registries.gray")
    public RegistryConfig grayRegistryConfig() {
        return new RegistryConfig();
    }

}