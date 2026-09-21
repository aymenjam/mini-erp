/**
 * Input ports: what the outside world may ask the application to do.
 *
 * <p><b>Role:</b> the only door into the application. Controllers and listeners call these interfaces, never a use case class.
 * That way an adapter can change (REST to gRPC) without touching a use case.
 * <p><b>Belongs here:</b> one interface per use case, for example {@code ListCustomersUseCase}, with its command and result types.
 * <p><b>Must not be here:</b> implementations, and HTTP or JPA types.
 */
package org.j2ra.minierp.template.application.port.in;
