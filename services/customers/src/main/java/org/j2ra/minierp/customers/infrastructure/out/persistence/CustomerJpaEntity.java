package org.j2ra.minierp.customers.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.Contact;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerStatus;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    // The lists are changed in place, never replaced: orphanRemoval deletes the rows that leave the list.
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerContactJpaEntity> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddressJpaEntity> addresses = new ArrayList<>();

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
        syncContacts(customer.contacts());
        syncAddresses(customer.addresses());
    }

    /**
     * First step of a change of primary contact or default address. Returns true when a flag was cleared.
     * The unique indexes allow one flag per customer (and type), and they cannot be deferred. Hibernate writes new rows before
     * it updates old ones, so the old flag must be cleared and written first. Then applyFrom sets the final state.
     */
    public boolean releaseFlagsLostIn(Customer customer) {
        boolean released = false;
        for (CustomerContactJpaEntity contact : contacts) {
            boolean keeps = customer.contacts().stream().anyMatch(c -> c.id().equals(contact.getId()) && c.primary());
            if (contact.isPrimary() && !keeps) {
                contact.releasePrimary();
                released = true;
            }
        }
        for (CustomerAddressJpaEntity address : addresses) {
            boolean keeps = customer.addresses().stream().anyMatch(a -> a.id().equals(address.getId()) && a.defaultForType());
            if (address.isDefaultForType() && !keeps) {
                address.releaseDefault();
                released = true;
            }
        }
        return released;
    }

    private void syncContacts(List<Contact> wanted) {
        contacts.removeIf(existing -> wanted.stream().noneMatch(c -> c.id().equals(existing.getId())));
        for (Contact contact : wanted) {
            contacts.stream().filter(existing -> existing.getId().equals(contact.id())).findFirst()
                    .ifPresentOrElse(existing -> existing.applyFrom(contact),
                            () -> contacts.add(new CustomerContactJpaEntity(this, contact)));
        }
    }

    private void syncAddresses(List<Address> wanted) {
        addresses.removeIf(existing -> wanted.stream().noneMatch(a -> a.id().equals(existing.getId())));
        for (Address address : wanted) {
            addresses.stream().filter(existing -> existing.getId().equals(address.id())).findFirst()
                    .ifPresentOrElse(existing -> existing.applyFrom(address),
                            () -> addresses.add(new CustomerAddressJpaEntity(this, address)));
        }
    }
}
