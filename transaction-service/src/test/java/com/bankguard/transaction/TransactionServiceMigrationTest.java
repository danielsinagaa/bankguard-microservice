package com.bankguard.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TransactionServiceMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bankguard")
            .withUsername("bankguard")
            .withPassword("bankguard");

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldRunFlywayMigrationsAndSeedData() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertTableCount(statement, "users", 3);
            assertTableCount(statement, "roles", 4);
            assertTableCount(statement, "customers", 3);
            assertTableCount(statement, "accounts", 3);
            assertTableCount(statement, "blacklisted_accounts", 2);
            assertTableCount(statement, "risk_rule_configs", 7);
            assertTableExists(statement, "transactions");
            assertTableExists(statement, "transaction_risk_factors");
            assertTableExists(statement, "kafka_event_logs");
        }
    }

    private static void assertTableCount(Statement statement, String tableName, int expectedCount) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(expectedCount);
        }
    }

    private static void assertTableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '" + tableName + "')"
        )) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean(1)).isTrue();
        }
    }
}
