package org.j2ra.minierp.template.infrastructure.out.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Base of every JPA entity (persistence adapter only): UUID id and optimistic locking. It has no table.
 *
 * <p>No setters, on purpose. {@code id} and {@code version} belong to Hibernate: changing them breaks the row identity and optimistic locking.
 * Subclasses also have no setters: they change through named methods. See {@code docs/jpa-entities.md}.
 */
@MappedSuperclass
// JPA needs a protected no-arg constructor. Constructors are not inherited, so every concrete entity needs its own.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractJpaEntity {

    @Id
    @Getter
    private UUID id;

    // Two writers on the same row: the second one fails instead of overwriting the first.
    @Version
    @Getter
    private long version;

    protected AbstractJpaEntity(UUID id) {
        this.id = id;
    }
}
