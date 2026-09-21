/**
 * Messaging adapter (outbound): publishes events.
 *
 * <p><b>Role:</b> implement the event publisher ports. Events are written with the transactional outbox, in the same transaction as the data.
 * <p><b>Belongs here:</b> the outbox and the relay to Kafka.
 * <p><b>Must not be here:</b> business rules. The package is empty on purpose: the outbox comes in roadmap item 1.4.
 */
package org.j2ra.minierp.template.infrastructure.out.messaging;
