/**
 * GraphQL adapter (inbound): for aggregated read screens of the Angular frontend.
 *
 * <p><b>Role:</b> read a query, call input ports, and shape the answer for one screen. Like a controller, it calls input ports only (rule R10).
 * <p><b>Belongs here:</b> GraphQL query handlers, their DTOs and the mapping from domain types.
 * <p><b>Must not be here:</b> business rules, transactions, database access. REST stays the default (ADR-009): every use of GraphQL needs
 * its own short ADR first. The package is empty on purpose: there is no GraphQL library and no schema in the template.
 */
package org.j2ra.minierp.customers.infrastructure.in.graphql;
