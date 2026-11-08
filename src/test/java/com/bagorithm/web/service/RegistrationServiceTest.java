package com.bagorithm.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RegistrationServiceTest {
    private JdbcTemplate jdbc;
    private RegistrationService service;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        encoder = new BCryptPasswordEncoder();
        service = new RegistrationService(jdbc, encoder);
    }

    @Test
    void normalizesEmailAndStoresOnlyBcryptPasswordHash() {
        service.register("  Ada Traveler ", " ADA@example.com ", "a-long-test-password");

        var email = org.mockito.ArgumentCaptor.forClass(String.class);
        var hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(eq("INSERT INTO app_users(full_name, email, password_hash) VALUES (?, ?, ?)"),
                eq("Ada Traveler"), email.capture(), hash.capture());
        assertEquals("ada@example.com", email.getValue());
        assertTrue(encoder.matches("a-long-test-password", hash.getValue()));
        assertTrue(!"a-long-test-password".equals(hash.getValue()));
    }

    @Test
    void rejectsMalformedEmailAndBcryptOverlongPasswordBeforeDatabaseWrite() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Ada", "not-an-email", "a-long-test-password"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Ada", "ada@example.com", "x".repeat(73)));
        verifyNoInteractions(jdbc);
    }

    @Test
    void reportsDuplicateAccountsClearly() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(jdbc).update(any(String.class), any(Object[].class));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.register("Ada", "ada@example.com", "a-long-test-password"));

        assertEquals("An account already exists for that email address.", error.getMessage());
    }
}
