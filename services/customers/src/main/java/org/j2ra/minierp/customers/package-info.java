/**
 * Base package of the service. Only the Spring Boot main class lives here.
 *
 * <p><b>Role:</b> start the application. The three layers are in the sub-packages:
 * {@code domain}, {@code application}, {@code infrastructure}. See docs/hexagonal-guide.md.
 * <p><b>Must not be here:</b> any other class. Everything else belongs to one layer, so the ArchUnit rules can check it.
 */
package org.j2ra.minierp.customers;
