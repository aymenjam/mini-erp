package org.j2ra.minierp.customers.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Spec 1.1, AC-8 and AC-13: the rules of the addresses of a customer. Plain JUnit, no Spring. */
class CustomerAddressesTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

    private static Customer customer() {
        return Customer.create(CustomerType.COMPANY, "ACME Ltd", null, null, null, T0);
    }

    private static Address address(AddressType type, String city, boolean isDefault) {
        return Address.create(type, "1 rue de la Paix", null, "75002", city, "FR", isDefault);
    }

    @Test
    @DisplayName("AC-8: an address is added, with a random UUID v4 id, and the update time changes")
    void add_address() {
        Address paris = address(AddressType.BILLING, "Paris", false);

        Customer changed = customer().addAddress(paris, T1);

        assertThat(changed.addresses()).containsExactly(paris);
        assertThat(paris.id().version()).isEqualTo(4);
        assertThat(changed.updatedAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("AC-8: a new default address replaces the old default of the same type only")
    void default_is_per_type() {
        Address billing1 = address(AddressType.BILLING, "Paris", true);
        Address shipping = address(AddressType.SHIPPING, "Lyon", true);
        Address billing2 = address(AddressType.BILLING, "Nice", true);

        Customer changed = customer().addAddress(billing1, T0).addAddress(shipping, T0).addAddress(billing2, T1);

        assertThat(changed.addresses()).filteredOn(Address::defaultForType).extracting(Address::id)
                .containsExactlyInAnyOrder(shipping.id(), billing2.id());
    }

    @Test
    @DisplayName("AC-8: replacing an address changes its fields; making it the default demotes the other one of its type")
    void replace_address() {
        Address first = address(AddressType.BILLING, "Paris", true);
        Address second = address(AddressType.BILLING, "Nice", false);
        Customer customer = customer().addAddress(first, T0).addAddress(second, T0);

        Customer changed = customer.replaceAddress(new Address(second.id(), AddressType.BILLING, "2 quai", "Floor 3", "06000", "Nice", "fr", true), T1);

        assertThat(changed.addresses()).filteredOn(Address::defaultForType).extracting(Address::id).containsExactly(second.id());
        assertThat(changed.addresses()).filteredOn(a -> a.id().equals(second.id())).extracting(Address::line1).containsExactly("2 quai");
    }

    @Test
    @DisplayName("AC-8: an address is removed; an unknown address gives AddressNotFoundException")
    void remove_address() {
        Address paris = address(AddressType.BILLING, "Paris", false);
        Customer customer = customer().addAddress(paris, T0);

        assertThat(customer.removeAddress(paris.id(), T1).addresses()).isEmpty();
        assertThatThrownBy(() -> customer.removeAddress(UUID.randomUUID(), T1)).isInstanceOf(AddressNotFoundException.class);
        assertThatThrownBy(() -> customer.replaceAddress(address(AddressType.BILLING, "X", false), T1))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("AC-2, AC-8: the country is an ISO 3166-1 alpha-2 code: lower case is fixed, anything else is refused")
    void country_rules() {
        assertThat(Address.create(AddressType.BILLING, "l1", null, null, "Paris", " fr ", false).country()).isEqualTo("FR");
        for (String bad : new String[]{"ZZ", "FRA", "F", "", "  ", null, "12"}) {
            assertThatThrownBy(() -> Address.create(AddressType.BILLING, "l1", null, null, "Paris", bad, false))
                    .as("country %s", bad)
                    .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("country"));
        }
    }

    @Test
    @DisplayName("AC-8: type, line1 and city are required; the limits are checked; optional fields are cleaned")
    void address_field_rules() {
        assertThatThrownBy(() -> Address.create(null, "l1", null, null, "Paris", "FR", false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("type"));
        assertThatThrownBy(() -> Address.create(AddressType.BILLING, " ", null, null, "Paris", "FR", false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("line1"));
        assertThatThrownBy(() -> Address.create(AddressType.BILLING, "l1", null, null, null, "FR", false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("city"));
        assertThatThrownBy(() -> Address.create(AddressType.BILLING, "x".repeat(201), null, null, "Paris", "FR", false))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Address.create(AddressType.BILLING, "l1", null, "1".repeat(21), "Paris", "FR", false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("postalCode"));

        Address cleaned = Address.create(AddressType.BILLING, " l1 ", "  ", " 75002 ", " Paris ", "FR", false);
        assertThat(cleaned.line1()).isEqualTo("l1");
        assertThat(cleaned.line2()).isNull();
        assertThat(cleaned.postalCode()).isEqualTo("75002");
        assertThat(cleaned.city()).isEqualTo("Paris");
    }

    @Test
    @DisplayName("AC-8: an inactive customer refuses every change of its addresses")
    void inactive_customer_refuses_address_changes() {
        Address paris = address(AddressType.BILLING, "Paris", false);
        Customer inactive = customer().addAddress(paris, T0).deactivate(T1);

        assertThatThrownBy(() -> inactive.addAddress(address(AddressType.SHIPPING, "Lyon", false), T1))
                .isInstanceOf(CustomerInactiveException.class);
        assertThatThrownBy(() -> inactive.replaceAddress(paris, T1)).isInstanceOf(CustomerInactiveException.class);
        assertThatThrownBy(() -> inactive.removeAddress(paris.id(), T1)).isInstanceOf(CustomerInactiveException.class);
    }

    @Test
    @DisplayName("AC-8: a customer built with two default addresses of one type is refused")
    void constructor_refuses_two_defaults_of_one_type() {
        assertThatThrownBy(() -> new Customer(UUID.randomUUID(), CustomerType.COMPANY, "ACME", null, null, null,
                CustomerStatus.ACTIVE, T0, T0, 0, List.of(),
                List.of(address(AddressType.BILLING, "Paris", true), address(AddressType.BILLING, "Nice", true))))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("addresses"));
    }

    @Test
    @DisplayName("AC-8: addresses are listed by type, then city")
    void addresses_have_a_stable_order() {
        Customer customer = customer().addAddress(address(AddressType.SHIPPING, "Lyon", false), T0)
                .addAddress(address(AddressType.BILLING, "Paris", false), T0)
                .addAddress(address(AddressType.BILLING, "Nice", false), T0);

        assertThat(customer.addresses()).extracting(Address::city).containsExactly("Nice", "Paris", "Lyon");
    }
}
