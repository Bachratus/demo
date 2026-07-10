package com.bachratus.demo.infra.web.controller;

import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import com.bachratus.demo.infra.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfiguration.class)
class CustomerControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("9ee4d795-569f-4f90-bc32-301e7d822944");

    private static final String DISPLAY_NAME = "Paweł";
    private static final String JWT_SUBJECT = "auth0|123456";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerAccountUseCase createCustomerAccountUseCase;

    @Test
    void shouldCreateCustomerUsingSubjectFromJwt() throws Exception {
        // given
        Customer createdCustomer = createCustomerFixture();

        when(createCustomerAccountUseCase.create(
                any(CreateCustomerAccountRequest.class)
        )).thenReturn(createdCustomer);

        String requestBody = """
                {
                  "displayName": "Paweł"
                }
                """;

        // when
        mockMvc.perform(post("/api/customer")
                        .with(jwt().jwt(token -> token.subject(JWT_SUBJECT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateCustomerAccountRequest> captor =
                ArgumentCaptor.forClass(CreateCustomerAccountRequest.class);

        verify(createCustomerAccountUseCase)
                .create(captor.capture());

        CreateCustomerAccountRequest passedRequest = captor.getValue();

        assertThat(passedRequest.displayName())
                .isEqualTo(DISPLAY_NAME);

        assertThat(passedRequest.subject())
                .isEqualTo(JWT_SUBJECT);
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        // given
        String requestBody = """
                {
                  "displayName": "Paweł"
                }
                """;

        // when
        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createCustomerAccountUseCase);
    }

    @Test
    void shouldReturnBadRequestForInvalidJson() throws Exception {
        // given
        String invalidJson = """
                {
                  "displayName":
                }
                """;

        // when
        mockMvc.perform(post("/api/customer")
                        .with(jwt().jwt(token -> token.subject(JWT_SUBJECT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                // then
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createCustomerAccountUseCase);
    }

    private Customer createCustomerFixture() {
        return Customer.builder()
                .id(CustomerId.of(CUSTOMER_ID))
                .userId(UserId.of(JWT_SUBJECT))
                .displayName(CustomerDisplayName.required(DISPLAY_NAME))
                .build();
    }
}