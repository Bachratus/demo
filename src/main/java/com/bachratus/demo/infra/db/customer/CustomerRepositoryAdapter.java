package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.domain.customer.Customer;
import com.bachratus.demo.domain.customer.CustomerId;
import com.bachratus.demo.domain.customer.UserId;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Transactional
@RequiredArgsConstructor
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        return customerJpaRepository.findByPublicId(customerId.value()).map(customerMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByUserId(UserId userId){
        Objects.requireNonNull(userId, "userId cannot be null");
        return customerJpaRepository.findByUserId(userId.value()).map(customerMapper::toDomain);
    }

    @Override
    public Customer createNewCustomer(Customer customer) {
        CustomerJpa customerJpa = customerMapper.toEntity(customer);
        return customerMapper.toDomain(customerJpaRepository.save(customerJpa));
    }
}
