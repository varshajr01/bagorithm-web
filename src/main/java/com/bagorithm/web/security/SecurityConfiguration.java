package com.bagorithm.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    private final Environment environment;

    @Value("${REMEMBER_ME_KEY:}")
    private String rememberMeKey;

    public SecurityConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        if (environment.acceptsProfiles(Profiles.of("prod")) && rememberMeKey.isBlank()) {
            throw new IllegalStateException("REMEMBER_ME_KEY must be configured when the prod profile is active.");
        }
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/welcome", "/login", "/register", "/css/**", "/images/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
                .rememberMe(remember -> remember.key(rememberMeKey.isBlank()
                        ? java.util.UUID.randomUUID().toString() : rememberMeKey))
                .logout(logout -> logout.logoutSuccessUrl("/welcome"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
