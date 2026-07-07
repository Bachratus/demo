package com.bachratus.demo.infra.db.entities;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
@EntityListeners(AuditingEntityListener.class)
public class CustomerJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CustomerIdSeqGen")
    @SequenceGenerator(name = "CustomerIdSeqGen", sequenceName = "customer_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name")
    private String displayName;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    protected CustomerJpa() {
    }

    @Override
    public boolean equals(Object o) {
        if (this ==  o) return true;
        if (!( o instanceof CustomerJpa customer)) return false;
        return publicId != null && publicId.equals(customer.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }
}
