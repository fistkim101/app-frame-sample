package com.fistkim.servicecore.support;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ApplicationEnvironment {

    private final Environment environment;

    public ApplicationEnvironment(Environment environment) {
        this.environment = environment;
    }

    public boolean isLocalProfile() {
        return environment.acceptsProfiles(Profiles.of("local"));
    }

    public boolean isDevelopmentProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    public List<String> getActiveProfiles() {
        return Arrays.asList(environment.getActiveProfiles());
    }

    public String getApplicationName() {
        return environment.getProperty("spring.application.name");
    }

}
