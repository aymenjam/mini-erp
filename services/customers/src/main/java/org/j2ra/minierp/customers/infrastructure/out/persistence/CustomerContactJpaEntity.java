package org.j2ra.minierp.customers.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.j2ra.minierp.customers.domain.Contact;

/** Storage form of a Contact. It belongs to one customer. No setters (docs/jpa-entities.md). */
@Entity
@Table(name = "customer_contact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CustomerContactJpaEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerJpaEntity customer;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    private String phone;

    private String jobTitle;

    // "primary" is a reserved word in SQL, so the column is is_primary.
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    CustomerContactJpaEntity(CustomerJpaEntity customer, Contact contact) {
        super(contact.id());
        this.customer = customer;
        applyFrom(contact);
    }

    /** Copies the business fields. Never id or version: Hibernate owns them. */
    void applyFrom(Contact contact) {
        this.firstName = contact.firstName();
        this.lastName = contact.lastName();
        this.email = contact.email();
        this.phone = contact.phone();
        this.jobTitle = contact.jobTitle();
        this.primary = contact.primary();
    }

    /** First step of a change of primary contact (see CustomerPersistenceAdapter). */
    void releasePrimary() {
        this.primary = false;
    }
}
