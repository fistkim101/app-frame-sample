package com.fistkim.securitycore.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityInMemoryAuthentication {

    @Getter
    @Setter
    public class SecurityUser {
        private String username;
        private String password;
    }

    @Bean("actuatorUser")
    @ConfigurationProperties(prefix = "security.user.actuator")
    public SecurityUser actuatorUser() {
        return new SecurityUser();
    }

    @Bean("adminUser")
    @ConfigurationProperties(prefix = "security.user.admin")
    public SecurityUser adminUser() {
        return new SecurityUser();
    }
}
