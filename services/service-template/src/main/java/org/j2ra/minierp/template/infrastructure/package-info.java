/**
 * Infrastructure layer: the adapters and the configuration.
 *
 * <p><b>Role:</b> connect the application to the real world (HTTP, Kafka, PostgreSQL). Tools and frameworks live here, so they can be replaced
 * without touching business rules.
 * <p><b>Belongs here:</b> {@code in} (who calls us), {@code out} (what we call) and {@code config} (wiring).
 * <p><b>Must not be here:</b> business rules. An adapter only translates between the outside format and the domain.
 */
package org.j2ra.minierp.template.infrastructure;
