package com.inventory.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTests {

    @Test
    @DisplayName("Should sanitize user-info from Neon/PostgreSQL JDBC URL with channel binding")
    void testSanitizeNeonJdbcUrlWithUserInfo() {
        String input = "jdbc:postgresql://neondb_owner:npg_secret123@ep-cool-pond-a5xyz.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
        String expected = "jdbc:postgresql://ep-cool-pond-a5xyz.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require";

        String actual = DatabaseConfig.sanitizeJdbcUrl(input);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Should convert postgresql:// URI format to valid JDBC URL and sanitize user-info")
    void testSanitizePostgresqlUri() {
        String input = "postgresql://neondb_owner:npg_secret123@ep-cool-pond-a5xyz.us-east-2.aws.neon.tech/neondb?sslmode=require";
        String expected = "jdbc:postgresql://ep-cool-pond-a5xyz.us-east-2.aws.neon.tech/neondb?sslmode=require";

        String actual = DatabaseConfig.sanitizeJdbcUrl(input);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Should preserve standard clean JDBC URL without modification")
    void testPreserveCleanJdbcUrl() {
        String input = "jdbc:postgresql://ep-cool-pond-a5xyz.us-east-2.aws.neon.tech/neondb?sslmode=require";

        String actual = DatabaseConfig.sanitizeJdbcUrl(input);
        assertEquals(input, actual);
    }

    @Test
    @DisplayName("Should preserve H2 test URL without modification")
    void testPreserveH2Url() {
        String input = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        String actual = DatabaseConfig.sanitizeJdbcUrl(input);
        assertEquals(input, actual);
    }
}
