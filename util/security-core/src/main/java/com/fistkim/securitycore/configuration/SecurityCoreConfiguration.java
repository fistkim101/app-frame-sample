package com.fistkim.securitycore.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class SecurityCoreConfiguration implements WebSecurityCustomizer {

    private final String PASSWORD_PREFIX_NOOP = "{noop}";
    private final String ROLE_ACTUATOR = "ACTUATOR";
    private final String ROLE_ADMIN = "ADMIN";

    private SecurityInMemoryAuthentication.SecurityUser actuatorUser;

    private SecurityInMemoryAuthentication.SecurityUser adminUser;

    public SecurityCoreConfiguration(SecurityInMemoryAuthentication.SecurityUser actuatorUser, SecurityInMemoryAuthentication.SecurityUser adminUser) {
        this.actuatorUser = actuatorUser;
        this.adminUser = adminUser;
    }

    @Autowired
    public void setInMemoryAuthenticatedUser(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.inMemoryAuthentication()
                .withUser(this.actuatorUser.getUsername()).password(this.PASSWORD_PREFIX_NOOP + this.actuatorUser.getPassword())
                .roles(this.ROLE_ACTUATOR)
                .and()
                .withUser(this.adminUser.getUsername()).password(this.PASSWORD_PREFIX_NOOP + this.adminUser.getPassword())
                .roles(this.ROLE_ADMIN, this.ROLE_ACTUATOR);
    }

    @Override
    public void customize(WebSecurity web) {
        web.ignoring()
                .antMatchers("css/**", "image/**", "js/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // default
        http.httpBasic().and()
                .csrf().disable();

        // actuator
        http.authorizeRequests()
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                .permitAll();
        http.authorizeRequests()
                .requestMatchers(EndpointRequest.toAnyEndpoint())
                .hasRole(this.ROLE_ACTUATOR);

        // eureka
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/eureka/**")
                .hasRole(this.ROLE_ACTUATOR);

        return http.build();
    }
}
