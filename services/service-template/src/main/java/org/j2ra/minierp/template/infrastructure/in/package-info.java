/**
 * Inbound (driving) adapters: they receive a call and hand it to an input port.
 *
 * <p><b>Role:</b> translate a request (HTTP, Kafka message) into a call of an input port.
 * <p><b>Belongs here:</b> the sub-packages {@code rest}, {@code graphql} (optional, ADR-009) and {@code kafka}.
 * <p><b>Must not be here:</b>
 * <ul>
 * <li>calls to output ports or use case classes: use the input port only (rule R3)
 * <li>other adapters, inbound or outbound: adapters do not know each other (rule R5)
 * </ul>
 */
package org.j2ra.minierp.template.infrastructure.in;
