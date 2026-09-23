package org.j2ra.minierp.customers.infrastructure.out.persistence;

import org.j2ra.minierp.customers.domain.Customer;

/** Converts between the domain object and the JPA entity. The only place that knows both. */
class CustomerPersistenceMapper {

    Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(entity.getId(), entity.getType(), entity.getName(), entity.getEmail(), entity.getPhone(),
                entity.getVatNumber(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
