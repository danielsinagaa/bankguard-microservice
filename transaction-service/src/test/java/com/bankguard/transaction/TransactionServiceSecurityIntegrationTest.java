package com.bankguard.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionServiceSecurityIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("bankguard")
            .withUsername("bankguard")
            .withPassword("bankguard");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "admin,admin123,ROLE_ADMIN",
            "backoffice,backoffice123,ROLE_BACKOFFICE",
            "analyst,analyst123,ROLE_FRAUD_ANALYST"
    })
    void shouldLoginDemoUsers(String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Request-Id", "req-login-" + username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.roles[0]").value(role));
    }

    @Test
    void shouldRejectInvalidCredentialsWithCommonError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Request-Id", "req-invalid-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value("req-invalid-login"));
    }

    @Test
    void shouldRejectMissingTokenForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/TRX-20260604-000001")
                        .header("X-Request-Id", "req-missing-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "req-missing-token"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value("req-missing-token"));
    }

    @Test
    void shouldRejectFraudAnalystSubmittingTransaction() throws Exception {
        String analystToken = login("analyst", "analyst123");

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + analystToken)
                        .header("X-Request-Id", "req-analyst-submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").value("req-analyst-submit"));
    }

    @Test
    void shouldAllowFraudAnalystToAccessReportingApi() throws Exception {
        String analystToken = login("analyst", "analyst123");

        mockMvc.perform(get("/api/v1/reports/risk-score-distribution")
                        .header("Authorization", "Bearer " + analystToken)
                        .header("X-Request-Id", "req-report-analyst")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].riskBucket").value("LOW"))
                .andExpect(jsonPath("$.data[1].riskBucket").value("MEDIUM"))
                .andExpect(jsonPath("$.data[2].riskBucket").value("HIGH"))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void shouldRejectBackofficeAccessToReportingApi() throws Exception {
        String backofficeToken = login("backoffice", "backoffice123");

        mockMvc.perform(get("/api/v1/reports/risk-score-distribution")
                        .header("Authorization", "Bearer " + backofficeToken)
                        .header("X-Request-Id", "req-report-backoffice")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-04"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").value("req-report-backoffice"));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("accessToken").asText();
    }
}
