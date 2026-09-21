/**
 * Domain layer: the business rules.
 *
 * <p><b>Role:</b> keep the rules of the business in plain Java, so they are easy to test and do not change when a framework changes.
 * <p><b>Belongs here:</b>
 * <ul>
 * <li>entities, value objects and domain events (records where possible)
 * <li>business rules and domain exceptions
 * </ul>
 * <p><b>Must not be here:</b>
 * <ul>
 * <li>Spring, JPA, Kafka, Jackson or Lombok: the domain must not depend on a tool (ArchUnit rule R1)
 * <li>DTOs, JPA entities, roles and security: those are infrastructure
 * </ul>
 */
package org.j2ra.minierp.customers.domain;
