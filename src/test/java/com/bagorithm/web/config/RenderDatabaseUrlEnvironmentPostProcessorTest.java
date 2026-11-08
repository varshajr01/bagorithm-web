package com.bagorithm.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderDatabaseUrlEnvironmentPostProcessorTest {
    @Test
    void convertsRenderConnectionUriToJdbcPropertiesWithoutLoggingCredentials() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL",
                "postgresql://bagorithm:p%40ss%3Aword@db.example:5432/bagorithm?sslmode=require");

        new RenderDatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("jdbc:postgresql://db.example:5432/bagorithm?sslmode=require",
                environment.getProperty("spring.datasource.url"));
        assertEquals("bagorithm", environment.getProperty("spring.datasource.username"));
        assertEquals("p@ss:word", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void leavesJdbcUrlsAndTheirSeparateCredentialsUnchanged() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("DATABASE_URL", "jdbc:postgresql://localhost:5432/bagorithm");
        environment.getSystemProperties().put("DATABASE_USERNAME", "local-user");
        environment.getSystemProperties().put("DATABASE_PASSWORD", "local-password");

        new RenderDatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertEquals("jdbc:postgresql://localhost:5432/bagorithm", environment.getProperty("DATABASE_URL"));
        assertEquals("local-user", environment.getProperty("DATABASE_USERNAME"));
        assertEquals("local-password", environment.getProperty("DATABASE_PASSWORD"));
    }

    @Test
    void requiresRememberMeKeyBeforeProductionStartup() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new RenderDatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication()));
    }
}
