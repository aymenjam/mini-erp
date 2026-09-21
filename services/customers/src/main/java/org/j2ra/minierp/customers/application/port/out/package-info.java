/**
 * Output ports: what a use case needs from the outside.
 *
 * <p><b>Role:</b> the use case says WHAT it needs (load a customer, publish an event); an adapter in
 * {@code infrastructure.out} says HOW. The interface lives here, so the dependency points inward (dependency inversion).
 * <p><b>Belongs here:</b> interfaces for repositories, event publishers and clients of other services, written with domain types.
 * <p><b>Must not be here:</b> implementations, JPA entities, Spring Data interfaces, Kafka types.
 */
package org.j2ra.minierp.customers.application.port.out;
