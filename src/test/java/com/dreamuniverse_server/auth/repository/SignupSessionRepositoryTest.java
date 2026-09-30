package com.dreamuniverse_server.auth.repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class SignupSessionRepositoryTest {
    @Container static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test void insertsRequiredCreationTimeAndPreservesItWhenRenewingTicket() throws Exception {
        var dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        var jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
            CREATE TABLE auth_signup_sessions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                provider VARCHAR(20) NOT NULL, provider_user_id VARCHAR(255) NOT NULL,
                token_hash VARCHAR(64) NOT NULL UNIQUE, email VARCHAR(320) NOT NULL,
                name VARCHAR(255), expires_at DATETIME(6) NOT NULL,
                created_at DATETIME(6) NOT NULL, consumed_at DATETIME(6),
                UNIQUE(provider, provider_user_id)
            )
            """);
        var query = SignupSessionRepository.class.getMethod("upsert", String.class, String.class,
                String.class, String.class, String.class, Instant.class).getAnnotation(Query.class).value();
        var namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        var expires = Timestamp.from(Instant.now().plusSeconds(600));
        namedParameterJdbcTemplate.update(query, Map.of("provider", "KAKAO", "providerUserId", "test-user",
                "hash", "a".repeat(64), "email", "test@example.com", "name", "test", "expiresAt", expires));
        var created = jdbcTemplate.queryForObject("SELECT created_at FROM auth_signup_sessions", Timestamp.class);
        assertNotNull(created);
        jdbcTemplate.update("UPDATE auth_signup_sessions SET consumed_at = UTC_TIMESTAMP(6)");
        namedParameterJdbcTemplate.update(query, Map.of("provider", "KAKAO", "providerUserId", "test-user",
                "hash", "b".repeat(64), "email", "test@example.com", "name", "test", "expiresAt", expires));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auth_signup_sessions", Integer.class));
        assertEquals(created, jdbcTemplate.queryForObject("SELECT created_at FROM auth_signup_sessions", Timestamp.class));
        assertNull(jdbcTemplate.queryForObject("SELECT consumed_at FROM auth_signup_sessions", Timestamp.class));
        assertEquals("b".repeat(64), jdbcTemplate.queryForObject("SELECT token_hash FROM auth_signup_sessions", String.class));
    }
}
