package com.bachratus.demo.infra.db.customer;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;

@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerJpa, Long> {

    @QueryHints({
            @QueryHint(name = HINT_CACHEABLE, value = "true"),
            @QueryHint(name = HINT_CACHE_REGION, value = "customer.byPublicId")
    })
    Optional<CustomerJpa> findByPublicId(UUID publicId);

    @QueryHints({
            @QueryHint(name = HINT_CACHEABLE, value = "true"),
            @QueryHint(name = HINT_CACHE_REGION, value = "customer.byUserId")
    })
    Optional<CustomerJpa> findByUserId(String userId);
}
