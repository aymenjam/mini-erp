package org.j2ra.minierp.customers.infrastructure.out.persistence;

import jakarta.persistence.EntityManager;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerSummary;
import org.j2ra.minierp.customers.domain.CustomerVersionConflictException;
import org.j2ra.minierp.customers.domain.DuplicateVatNumberException;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;
import org.j2ra.minierp.customers.domain.SortDirection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

/** Stores customers in PostgreSQL. It runs inside the transaction of the use case. */
public class CustomerPersistenceAdapter implements CustomerRepository {

    // Names of the unique indexes in V2 and V3.
    private static final String VAT_NUMBER_INDEX = "ux_customer_vat_number";
    private static final String PRIMARY_CONTACT_INDEX = "ux_customer_contact_primary";
    private static final String DEFAULT_ADDRESS_INDEX = "ux_customer_address_default";

    private final CustomerJpaRepository jpaRepository;
    private final EntityManager entityManager;
    private final CustomerPersistenceMapper mapper = new CustomerPersistenceMapper();

    public CustomerPersistenceAdapter(CustomerJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        try {
            // We write at once (flush), so a refusal of the database is seen here and not later at commit.
            Optional<CustomerJpaEntity> existing = jpaRepository.findById(customer.id());
            CustomerJpaEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
                // Two steps: clear the flags that are lost and write it, then write the final state (see releaseFlagsLostIn).
                if (entity.releaseFlagsLostIn(customer)) {
                    jpaRepository.flush();
                }
                entity.applyFrom(customer);
                jpaRepository.saveAndFlush(entity);
            } else {
                // persist, not save: save() would try a merge for an entity that already has an id.
                entity = new CustomerJpaEntity(customer);
                entityManager.persist(entity);
                jpaRepository.flush();
            }
            return mapper.toDomain(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new CustomerVersionConflictException(customer.id());
        } catch (DataIntegrityViolationException e) {
            if (mentionsIndex(e, VAT_NUMBER_INDEX)) {
                throw new DuplicateVatNumberException();
            }
            // Someone else set the primary contact or the default address between our read and our write.
            if (mentionsIndex(e, PRIMARY_CONTACT_INDEX) || mentionsIndex(e, DEFAULT_ADDRESS_INDEX)) {
                throw new CustomerVersionConflictException(customer.id());
            }
            throw e;
        }
    }

    @Override
    public PageResult<CustomerSummary> findAll(PageQuery query) {
        Sort.Direction direction = query.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<CustomerJpaEntity> page = jpaRepository.findAll(
                PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortField())));
        return new PageResult<>(page.getContent().stream().map(mapper::toSummary).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static boolean mentionsIndex(Throwable error, String indexName) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(indexName)) {
                return true;
            }
        }
        return false;
    }
}
