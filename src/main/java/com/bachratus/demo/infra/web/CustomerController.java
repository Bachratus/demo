package com.bachratus.demo.infra.web;

import com.bachratus.demo.application.request.CreateCustomerAccountRequest;
import com.bachratus.demo.application.ports.in.CreateCustomerAccountUseCase;
import com.bachratus.demo.domain.customer.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping(path = "/api")
@RestController
public class CustomerController {

    private final CreateCustomerAccountUseCase createCustomerAccountUseCase;

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping("/customer")
    public Customer createCustomerAccount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateCustomerAccountRequest request
    ) {
        //        Map.of(
        //                "message", "Hello from Bachratus",
        //                "userId", jwt.getSubject(),
        //                "email", jwt.getClaimAsString("email")
        //        );

        return createCustomerAccountUseCase.create(request, jwt.getSubject());
    }
}