/**
 * Application layer: the use cases.
 *
 * <p><b>Role:</b> one class per use case. It calls the domain and the output ports, and it starts the transaction:
 * one use case is one local transaction.
 * <p><b>Belongs here:</b> use case classes that implement an input port, and {@code @Transactional} (only here, rule R6).
 * <p><b>Must not be here:</b>
 * <ul>
 * <li>HTTP, JSON, SQL, Kafka: the use case must not know how it is called or where data is stored (rules R2, R3)
 * <li>{@code @Service} or other Spring stereotypes: {@code infrastructure.config} creates the use cases, so this layer stays free of Spring
 * <li>Lombok
 * </ul>
 */
package org.j2ra.minierp.customers.application;
