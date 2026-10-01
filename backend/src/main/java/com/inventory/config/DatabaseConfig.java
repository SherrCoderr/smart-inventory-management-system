package com.inventory.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom DataSource configuration ensuring complete compatibility with Neon, Render,
 * and standard PostgreSQL JDBC drivers.
 * Automatically sanitizes URLs containing user-info (e.g., jdbc:postgresql://user:pass@host/db)
 * which are rejected by org.postgresql.Driver.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final Pattern USER_INFO_PATTERN = Pattern.compile("^jdbc:postgresql://([^@/]+)@(.+)$");

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (StringUtils.hasText(rawUrl)) {
            rawUrl = rawUrl.trim();
            // Handle raw URI schemes if passed without jdbc: prefix
            if (rawUrl.startsWith("postgres://")) {
                rawUrl = "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
            } else if (rawUrl.startsWith("postgresql://")) {
                rawUrl = "jdbc:" + rawUrl;
            }

            // Sanitize user-info (user:pass@) if present in the JDBC URL
            Matcher matcher = USER_INFO_PATTERN.matcher(rawUrl);
            if (matcher.matches()) {
                String userInfo = matcher.group(1);
                String hostAndRest = matcher.group(2);
                rawUrl = "jdbc:postgresql://" + hostAndRest;
                log.info("Sanitized user-info credentials from PostgreSQL JDBC URL for driver compatibility.");

                // If username was not explicitly configured, extract from user-info
                if (!StringUtils.hasText(username) || "postgres".equals(username)) {
                    if (userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        if (!StringUtils.hasText(password) || "postgres".equals(password)) {
                            password = parts[1];
                        }
                    } else {
                        username = userInfo;
                    }
                }
            }
        }

        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        if (StringUtils.hasText(rawUrl)) {
            dataSource.setJdbcUrl(rawUrl);
        }
        if (StringUtils.hasText(username)) {
            dataSource.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            dataSource.setPassword(password);
        }

        return dataSource;
    }

    /**
     * Helper method to sanitize a database URL string (exposed for testing/utility).
     */
    public static String sanitizeJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("postgres://")) {
            trimmed = "jdbc:postgresql://" + trimmed.substring("postgres://".length());
        } else if (trimmed.startsWith("postgresql://")) {
            trimmed = "jdbc:" + trimmed;
        }

        Matcher matcher = USER_INFO_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return "jdbc:postgresql://" + matcher.group(2);
        }
        return trimmed;
    }
}
