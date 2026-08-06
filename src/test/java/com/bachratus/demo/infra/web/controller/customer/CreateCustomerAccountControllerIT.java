package com.bachratus.demo.infra.web.controller.customer;

import com.bachratus.demo.config.BaseFullIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.kafka.enabled=false")
class CreateCustomerAccountControllerIT extends BaseFullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("POST /api/customer")
    @Nested
    class CreateCustomerAccount {

        @Test
        void shouldCreateCustomerAndOutboxEventInOneFullHttpFlow() throws Exception {
            // given
            clock.setInstant(Instant.parse("2026-01-01T12:30:00Z"));
            String subject = uniqueSubject("success");

            // when
            MvcResult result = postCustomer(subject, """
                    {
                      "displayName": "Pawel"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id.value").isNotEmpty())
                    .andExpect(jsonPath("$.userId.value").value(subject))
                    .andExpect(jsonPath("$.customerDisplayName.value").value("Pawel"))
                    .andReturn();

            // then
            String customerId = responseCustomerId(result);

            assertThat(customerCountBySubject(subject)).isEqualTo(1);
            assertThat(customerDisplayName(subject)).isEqualTo("Pawel");

            List<Map<String, Object>> outboxRows = outboxRowsForAggregate(customerId);
            assertThat(outboxRows).singleElement().satisfies(row -> {
                assertThat(row.get("topic_key")).isEqualTo("customer-account-created");
                assertThat(row.get("topic_name")).isEqualTo("store.customer-account-created.v1");
                assertThat(row.get("aggregate_type")).isEqualTo("customer");
                assertThat(row.get("aggregate_id")).isEqualTo(customerId);
                assertThat(row.get("event_type")).isEqualTo("customer.account-created.v1");
                assertThat(row.get("status")).isEqualTo("PENDING");
                assertThat(row.get("retry_count")).isEqualTo(0);
                assertThat(row.get("payload_display_name")).isEqualTo("Pawel");
                assertThat(row.get("payload_user_id")).isEqualTo(subject);
            });
        }

        @Test
        void shouldTreatBlankDisplayNameAsOptionalAndStillWriteOutboxPayloadFieldAsNull() throws Exception {
            // given
            String subject = uniqueSubject("blank-display-name");

            // when
            MvcResult result = postCustomer(subject, """
                    {
                      "displayName": "   "
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id.value").isNotEmpty())
                    .andExpect(jsonPath("$.userId.value").value(subject))
                    .andReturn();

            // then
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            String customerId = response.at("/id/value").asText();

            assertThat(response.get("customerDisplayName").isNull()).isTrue();
            assertThat(customerDisplayName(subject)).isNull();

            Map<String, Object> outboxRow = outboxRowsForAggregate(customerId).getFirst();
            assertThat(outboxRow.get("payload_display_name")).isNull();
            assertThat(outboxRow.get("payload_has_display_name")).isEqualTo(true);
        }

        @Test
        void shouldReturnConflictAndNotCreateSecondCustomerOrOutboxEventForDuplicateSubject() throws Exception {
            // given
            String subject = uniqueSubject("duplicate");
            MvcResult firstResult = postCustomer(subject, """
                    {
                      "displayName": "First"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andReturn();
            String firstCustomerId = responseCustomerId(firstResult);

            // when & then
            postCustomer(subject, """
                    {
                      "displayName": "Second"
                    }
                    """)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("DOMAIN"))
                    .andExpect(jsonPath("$.code").value("ALREADY_EXISTS"));

            assertThat(customerCountBySubject(subject)).isEqualTo(1);
            assertThat(outboxRowsForAggregate(firstCustomerId)).hasSize(1);
        }

        @Test
        void shouldReturnBadRequestAndNotWriteAnythingWhenJwtSubjectIsBlank() throws Exception {
            // given
            long customersBefore = customerCount();
            long outboxBefore = outboxCount();

            // when & then
            postCustomer("   ", """
                    {
                      "displayName": "Pawel"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("VALIDATION"))
                    .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_FIELD"));

            assertThat(customerCount()).isEqualTo(customersBefore);
            assertThat(outboxCount()).isEqualTo(outboxBefore);
        }

        @Test
        void shouldReturnUnauthorizedAndNotWriteAnythingWhenJwtIsMissing() throws Exception {
            // given
            long customersBefore = customerCount();
            long outboxBefore = outboxCount();

            // when & then
            mockMvc.perform(post("/api/customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "displayName": "Pawel"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());

            assertThat(customerCount()).isEqualTo(customersBefore);
            assertThat(outboxCount()).isEqualTo(outboxBefore);
        }

        @Test
        void shouldReturnBadRequestAndNotWriteAnythingWhenJsonIsMalformed() throws Exception {
            // given
            long customersBefore = customerCount();
            long outboxBefore = outboxCount();

            // when & then
            postCustomer(uniqueSubject("malformed-json"), """
                    {
                      "displayName":
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("VALIDATION"));

            assertThat(customerCount()).isEqualTo(customersBefore);
            assertThat(outboxCount()).isEqualTo(outboxBefore);
        }
    }

    private org.springframework.test.web.servlet.ResultActions postCustomer(String subject, String body) throws Exception {
        return mockMvc.perform(post("/api/customer")
                .with(jwt().jwt(token -> token.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String responseCustomerId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/id/value")
                .asText();
    }

    private String uniqueSubject(String prefix) {
        return "auth0|" + prefix + "-" + UUID.randomUUID();
    }

    private long customerCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Long.class);
    }

    private long outboxCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_event", Long.class);
    }

    private int customerCountBySubject(String subject) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE user_id = ?",
                Integer.class,
                subject
        );
    }

    private String customerDisplayName(String subject) {
        return jdbcTemplate.queryForObject(
                "SELECT display_name FROM customer WHERE user_id = ?",
                String.class,
                subject
        );
    }

    private List<Map<String, Object>> outboxRowsForAggregate(String aggregateId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT topic_key,
                               topic_name,
                               aggregate_type,
                               aggregate_id,
                               event_type,
                               status,
                               retry_count,
                               payload ->> 'displayName' AS payload_display_name,
                               payload ->> 'userId' AS payload_user_id,
                               jsonb_exists(payload, 'displayName') AS payload_has_display_name
                        FROM outbox_event
                        WHERE aggregate_id = ?
                        ORDER BY occurred_at, id
                        """,
                aggregateId
        );
    }
}
