package com.fistkim.cachesupport.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "cache.hazelcast")
    public HazelcastConfiguration hazelcastConfiguration() {
        return new HazelcastConfiguration();
    }

}
