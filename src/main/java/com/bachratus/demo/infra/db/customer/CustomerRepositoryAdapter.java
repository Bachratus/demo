package com.bachratus.demo.infra.db.customer;

import com.bachratus.demo.application.ports.out.CustomerRepository;
import com.bachratus.demo.domain.customer.Customer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Transactional
@RequiredArgsConstructor
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Customer createNewCustomer(Customer customer) {
        CustomerJpa customerJpa = customerMapper.toEntity(customer);
        return customerMapper.toDomain(customerJpaRepository.save(customerJpa));
    }
}
