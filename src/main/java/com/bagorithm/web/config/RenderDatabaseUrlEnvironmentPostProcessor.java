package com.bagorithm.web.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "renderDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && environment.getProperty("REMEMBER_ME_KEY", "").isBlank()) {
            throw new IllegalStateException("REMEMBER_ME_KEY must be configured when the prod profile is active.");
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || !(databaseUrl.startsWith("postgres://")
                || databaseUrl.startsWith("postgresql://"))) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATABASE_URL is not a valid PostgreSQL connection URI.");
        }
        if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() < 2) {
            throw new IllegalStateException("DATABASE_URL must include a PostgreSQL host and database name.");
        }

        String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(host);
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl.toString());
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null) {
            int separator = rawUserInfo.indexOf(':');
            if (separator <= 0 || separator == rawUserInfo.length() - 1) {
                throw new IllegalStateException("DATABASE_URL must include both a database username and password.");
            }
            properties.put("spring.datasource.username", decode(rawUserInfo.substring(0, separator)));
            properties.put("spring.datasource.password", decode(rawUserInfo.substring(separator + 1)));
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATABASE_URL contains invalid credential encoding.");
        }
    }
}
