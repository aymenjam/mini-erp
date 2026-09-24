package org.j2ra.minierp.customers.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.AddressType;

/** Storage form of an Address. It belongs to one customer. No setters (docs/jpa-entities.md). */
@Entity
@Table(name = "customer_address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CustomerAddressJpaEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerJpaEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;

    @Column(nullable = false)
    private String line1;

    private String line2;

    private String postalCode;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private boolean defaultForType;

    CustomerAddressJpaEntity(CustomerJpaEntity customer, Address address) {
        super(address.id());
        this.customer = customer;
        applyFrom(address);
    }

    /** Copies the business fields. Never id or version: Hibernate owns them. */
    void applyFrom(Address address) {
        this.type = address.type();
        this.line1 = address.line1();
        this.line2 = address.line2();
        this.postalCode = address.postalCode();
        this.city = address.city();
        this.country = address.country();
        this.defaultForType = address.defaultForType();
    }

    /** First step of a change of default address (see CustomerPersistenceAdapter). */
    void releaseDefault() {
        this.defaultForType = false;
    }
}
