/**
 * Outbound (driven) adapters: they implement the output ports.
 *
 * <p><b>Role:</b> do the real work the application asks for (save data, publish an event, call another service).
 * <p><b>Belongs here:</b> the sub-packages {@code persistence} and {@code messaging}.
 * <p><b>Must not be here:</b> calls to input ports or use cases, and inbound adapters (rules R4, R5). An adapter never calls back into the application.
 */
package org.j2ra.minierp.template.infrastructure.out;
