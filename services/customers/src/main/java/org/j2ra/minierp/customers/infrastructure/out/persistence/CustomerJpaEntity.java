package org.j2ra.minierp.customers.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerStatus;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.time.Instant;

/** Storage form of a Customer. No business rule here: the rules are in the domain. No setters (docs/jpa-entities.md). */
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CustomerJpaEntity extends AbstractJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerType type;

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    private String vatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public CustomerJpaEntity(Customer customer) {
        super(customer.id());
        this.createdAt = customer.createdAt();
        applyFrom(customer);
    }

    /** Copies the business fields. Never id, version or createdAt: Hibernate owns the first two, and the third never changes. */
    public void applyFrom(Customer customer) {
        this.type = customer.type();
        this.name = customer.name();
        this.email = customer.email();
        this.phone = customer.phone();
        this.vatNumber = customer.vatNumber();
        this.status = customer.status();
        this.updatedAt = customer.updatedAt();
    }
}
