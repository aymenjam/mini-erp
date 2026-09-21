/**
 * Composition root: the Spring configuration that wires everything together.
 *
 * <p><b>Role:</b> create the use cases as beans, connect ports to adapters, and set up security. It is the one place that may see all layers.
 * <p><b>Belongs here:</b> {@code @Configuration} classes, security setup, bean definitions.
 * <p><b>Must not be here:</b> business rules. Nothing may depend on this package (rule R9), so it can change without side effects.
 */
package org.j2ra.minierp.template.infrastructure.config;
