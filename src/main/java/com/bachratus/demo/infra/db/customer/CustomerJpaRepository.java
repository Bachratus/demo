package com.bachratus.demo.infra.db.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerJpa, Long> {

    Optional<CustomerJpa> findByPublicId(UUID publicId);

    Optional<CustomerJpa> findByUserId(String userId);
}
