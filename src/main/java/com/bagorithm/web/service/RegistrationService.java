package com.bagorithm.web.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class RegistrationService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(String name, String email, String password) {
        String normalizedName = name == null ? "" : name.strip();
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalizedName.isBlank() || normalizedName.length() > 120 || !EMAIL.matcher(normalizedEmail).matches()
                || normalizedEmail.length() > 254 || password == null || password.length() < 10
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Enter a name and valid email. Passwords must be 10–72 UTF-8 bytes.");
        }
        try {
            jdbc.update("INSERT INTO app_users(full_name, email, password_hash) VALUES (?, ?, ?)",
                    normalizedName, normalizedEmail, passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("An account already exists for that email address.");
        }
    }
}
