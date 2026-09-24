package org.j2ra.minierp.customers.infrastructure.out.persistence;

import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.Contact;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerSummary;

/** Converts between the domain object and the JPA entity. The only place that knows both. */
class CustomerPersistenceMapper {

    /** The whole customer. It reads the contacts and addresses, so call it inside the transaction. */
    Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(entity.getId(), entity.getType(), entity.getName(), entity.getEmail(), entity.getPhone(),
                entity.getVatNumber(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion(),
                entity.getContacts().stream().map(this::toDomain).toList(),
                entity.getAddresses().stream().map(this::toDomain).toList());
    }

    /** Only the main fields: it does not touch the lazy contacts and addresses. */
    CustomerSummary toSummary(CustomerJpaEntity entity) {
        return new CustomerSummary(entity.getId(), entity.getType(), entity.getName(), entity.getEmail(), entity.getStatus());
    }

    private Contact toDomain(CustomerContactJpaEntity entity) {
        return new Contact(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getPhone(),
                entity.getJobTitle(), entity.isPrimary());
    }

    private Address toDomain(CustomerAddressJpaEntity entity) {
        return new Address(entity.getId(), entity.getType(), entity.getLine1(), entity.getLine2(), entity.getPostalCode(),
                entity.getCity(), entity.getCountry(), entity.isDefaultForType());
    }
}
