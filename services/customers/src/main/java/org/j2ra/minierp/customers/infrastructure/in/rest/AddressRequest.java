package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.j2ra.minierp.customers.application.port.in.AddAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateAddressUseCase;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.AddressType;

import java.util.UUID;

// The limits repeat the domain ones on purpose: here they give a clear 400 with all bad fields at once.
// The country code is checked by the domain, against the list of the JDK.
@Schema(description = "Data of an address. The same body is used to add an address and to replace one.")
record AddressRequest(
        @Schema(description = "Use of the address", example = "BILLING") @NotNull AddressType type,
        @Schema(description = "First address line", example = "1 rue de la Paix") @NotBlank @Size(max = Address.LINE_MAX) String line1,
        @Schema(description = "Second address line", example = "Building B") @Size(max = Address.LINE_MAX) String line2,
        @Schema(description = "Postal code", example = "75002") @Size(max = Address.POSTAL_CODE_MAX) String postalCode,
        @Schema(description = "City", example = "Paris") @NotBlank @Size(max = Address.CITY_MAX) String city,
        @Schema(description = "Country, ISO 3166-1 alpha-2 code. Letters become upper case.", example = "FR") @NotBlank String country,
        @Schema(description = "The default address of this type. If true, the address that was the default for the same type is not the default any more. Default: false.",
                example = "true") boolean defaultForType) {

    AddAddressUseCase.Command toAddCommand(UUID customerId) {
        return new AddAddressUseCase.Command(customerId, type, line1, line2, postalCode, city, country, defaultForType);
    }

    UpdateAddressUseCase.Command toUpdateCommand(UUID customerId, UUID addressId) {
        return new UpdateAddressUseCase.Command(customerId, addressId, type, line1, line2, postalCode, city, country, defaultForType);
    }
}
