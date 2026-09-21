/**
 * Kafka adapter (inbound): listeners for events from other services.
 *
 * <p><b>Role:</b> read a message, turn it into a command, and call an input port. The consumer must be idempotent.
 * <p><b>Belongs here:</b> {@code @KafkaListener} classes and event payload mapping.
 * <p><b>Must not be here:</b> business rules or database access. The package is empty on purpose: Kafka is added in roadmap item 1.4.
 */
package org.j2ra.minierp.customers.infrastructure.in.kafka;
