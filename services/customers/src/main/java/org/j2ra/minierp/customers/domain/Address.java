package org.j2ra.minierp.customers.domain;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** A postal address of a customer. Part of the Customer aggregate. */
public record Address(
        UUID id,
        AddressType type,
        String line1,
        String line2,
        String postalCode,
        String city,
        String country,
        boolean defaultForType) {

    public static final int LINE_MAX = 200;
    public static final int POSTAL_CODE_MAX = 20;
    public static final int CITY_MAX = 100;

    // ISO 3166-1 alpha-2 codes, from the JDK: no library needed.
    private static final Set<String> COUNTRY_CODES = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

    public Address {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (type == null) {
            throw new InvalidCustomerException("type", "must not be null");
        }
        line1 = Fields.required("line1", line1, LINE_MAX);
        line2 = Fields.clean(line2);
        postalCode = Fields.clean(postalCode);
        city = Fields.required("city", city, CITY_MAX);
        country = Fields.cleanUpper(country);
        Fields.tooLong("line2", line2, LINE_MAX);
        Fields.tooLong("postalCode", postalCode, POSTAL_CODE_MAX);
        if (country == null || !COUNTRY_CODES.contains(country)) {
            throw new InvalidCustomerException("country", "must be an ISO 3166-1 alpha-2 country code, for example FR");
        }
    }

    /** A new address with a new random id (UUID v4). */
    public static Address create(AddressType type, String line1, String line2, String postalCode, String city,
                                 String country, boolean defaultForType) {
        return new Address(UUID.randomUUID(), type, line1, line2, postalCode, city, country, defaultForType);
    }

    Address withDefaultForType(boolean value) {
        return new Address(id, type, line1, line2, postalCode, city, country, value);
    }
}
