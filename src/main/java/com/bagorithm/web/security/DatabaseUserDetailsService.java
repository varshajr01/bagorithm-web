package com.bagorithm.web.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;

    public DatabaseUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return jdbc.query("SELECT email, password_hash FROM app_users WHERE lower(email) = lower(?)",
                        (row, number) -> User.withUsername(row.getString("email"))
                                .password(row.getString("password_hash")).roles("USER").build(), email)
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }
}
