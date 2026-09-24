package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.AddressType;

import java.util.UUID;

@Schema(description = "An address of a customer")
record AddressResponse(
        @Schema(description = "Id of the address") UUID id,
        @Schema(description = "Use of the address", example = "BILLING") AddressType type,
        @Schema(description = "First address line", example = "1 rue de la Paix") String line1,
        @Schema(description = "Second address line", example = "Building B", nullable = true) String line2,
        @Schema(description = "Postal code", example = "75002", nullable = true) String postalCode,
        @Schema(description = "City", example = "Paris") String city,
        @Schema(description = "Country, ISO 3166-1 alpha-2 code", example = "FR") String country,
        @Schema(description = "True for the default address of its type (at most one per type)", example = "true") boolean defaultForType) {

    static AddressResponse from(Address address) {
        return new AddressResponse(address.id(), address.type(), address.line1(), address.line2(), address.postalCode(),
                address.city(), address.country(), address.defaultForType());
    }
}
