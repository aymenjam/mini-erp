/**
 * Persistence adapter: PostgreSQL through JPA.
 *
 * <p><b>Role:</b> implement the repository ports. JPA entities live here and are mapped to domain objects, so the domain has no JPA.
 * <p><b>Belongs here:</b> JPA entities (they extend {@code AbstractJpaEntity}), Spring Data repositories, mappers, the adapter classes.
 * Each concrete entity has {@code @NoArgsConstructor(access = AccessLevel.PROTECTED)}: JPA needs it, and constructors are not inherited.
 * <p><b>Entity rules:</b> no setters (no {@code @Setter}), no {@code @Data}, {@code @EqualsAndHashCode} or {@code @ToString}. Create with a constructor or
 * a factory, update with one entity method. Why and how: {@code docs/jpa-entities.md}.
 * <p><b>Must not be here:</b> business rules, and anything that lets a JPA entity leave the package (rule R7). Schema changes are Flyway migrations, not code.
 */
package org.j2ra.minierp.customers.infrastructure.out.persistence;
