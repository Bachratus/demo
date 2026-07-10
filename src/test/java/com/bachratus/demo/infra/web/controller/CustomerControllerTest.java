package com.bachratus.demo.infra.web.controller;

import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerDisplayName;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerAccountUseCase createCustomerAccountUseCase;

    @Test
    void shouldCreateCustomerUsingSubjectFromJwt() throws Exception {
        // given
        Customer createdCustomer = createCustomerFixture();

        when(createCustomerAccountUseCase.create(any()))
                .thenReturn(createdCustomer);

        String requestBody = """
                {
                  "displayName": "Paweł",
                  "subject": "subject-sent-in-body"
                }
                """;

        // when
        mockMvc.perform(post("/api/customer")
                        .with(jwt().jwt(token -> token.subject("auth0|123456")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // then
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateCustomerAccountRequest> captor =
                ArgumentCaptor.forClass(CreateCustomerAccountRequest.class);

        verify(createCustomerAccountUseCase).create(captor.capture());

        CreateCustomerAccountRequest passedRequest = captor.getValue();

        assertThat(passedRequest.displayName())
                .isEqualTo("Paweł");

        assertThat(passedRequest.subject())
                .isEqualTo("auth0|123456");
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        String requestBody = """
                {
                  "displayName": "Paweł",
                  "subject": "irrelevant"
                }
                """;

        mockMvc.perform(post("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createCustomerAccountUseCase);
    }

    @Test
    void shouldReturnBadRequestForInvalidJson() throws Exception {
        String invalidJson = """
                {
                  "displayName":
                }
                """;

        mockMvc.perform(post("/api/customer")
                        .with(jwt().jwt(token -> token.subject("auth0|123456")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createCustomerAccountUseCase);
    }

    private Customer createCustomerFixture() {
        return Customer.builder()
                .id(CustomerId.create())
                .userId(UserId.of("unique-customer-id"))
                .displayName(CustomerDisplayName.required("me"))
                .build();
    }
}