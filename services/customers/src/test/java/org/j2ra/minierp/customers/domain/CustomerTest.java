package org.j2ra.minierp.customers.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Spec 1.1, AC-1, AC-2, AC-6, AC-13: the business rules of the customer, plain JUnit, no Spring. */
class CustomerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

    private static Customer company() {
        return Customer.create(CustomerType.COMPANY, "ACME Ltd", "contact@acme.example", "+33 1", "FR123", T0);
    }

    @Test
    @DisplayName("AC-1: a new customer is active, has version 0, a random id and the creation time")
    void new_customer_is_active() {
        Customer customer = company();

        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.version()).isZero();
        assertThat(customer.id()).isNotNull();
        assertThat(customer.id().version()).isEqualTo(4);
        assertThat(customer.createdAt()).isEqualTo(T0);
        assertThat(customer.updatedAt()).isEqualTo(T0);
        assertThat(company().id()).isNotEqualTo(customer.id());
    }

    @Test
    @DisplayName("AC-1: an individual can be created without a VAT number")
    void individual_without_vat() {
        Customer customer = Customer.create(CustomerType.INDIVIDUAL, "Jane Doe", null, null, null, T0);

        assertThat(customer.type()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(customer.vatNumber()).isNull();
    }

    @Test
    @DisplayName("AC-2: an individual with a VAT number is refused")
    void individual_with_vat_is_refused() {
        assertThatThrownBy(() -> Customer.create(CustomerType.INDIVIDUAL, "Jane Doe", null, null, "FR123", T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("vatNumber"));
    }

    @Test
    @DisplayName("AC-2: a blank or missing name is refused, a name of 201 characters is refused, 200 is fine")
    void name_rules() {
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, "  ", null, null, null, T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("name"));
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, null, null, null, null, T0))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, "x".repeat(201), null, null, null, T0))
                .isInstanceOf(InvalidCustomerException.class);
        assertThat(Customer.create(CustomerType.COMPANY, "x".repeat(200), null, null, null, T0).name()).hasSize(200);
    }

    @Test
    @DisplayName("AC-2: a missing type is refused")
    void type_is_required() {
        assertThatThrownBy(() -> Customer.create(null, "ACME", null, null, null, T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("type"));
    }

    @Test
    @DisplayName("Scope 1: the VAT number is trimmed and upper case; text fields are trimmed; blank optional fields become null")
    void values_are_cleaned() {
        Customer customer = Customer.create(CustomerType.COMPANY, "  ACME Ltd ", "  ", " 0102 ", "  fr12ab  ", T0);

        assertThat(customer.name()).isEqualTo("ACME Ltd");
        assertThat(customer.email()).isNull();
        assertThat(customer.phone()).isEqualTo("0102");
        assertThat(customer.vatNumber()).isEqualTo("FR12AB");
    }

    @Test
    @DisplayName("AC-2: too long optional fields are refused")
    void optional_fields_have_a_limit() {
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, "A", "x".repeat(250) + "@a.io", null, null, T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("email"));
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, "A", null, "1".repeat(51), null, T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("phone"));
        assertThatThrownBy(() -> Customer.create(CustomerType.COMPANY, "A", null, null, "F".repeat(51), T0))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("vatNumber"));
    }

    @Test
    @DisplayName("AC-4: update replaces the main fields, keeps id, status, creation time and version, and sets the update time")
    void update_replaces_fields() {
        Customer before = company();

        Customer after = before.update(CustomerType.COMPANY, "New name", null, null, null, T1);

        assertThat(after.name()).isEqualTo("New name");
        assertThat(after.email()).isNull();
        assertThat(after.vatNumber()).isNull();
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.createdAt()).isEqualTo(T0);
        assertThat(after.updatedAt()).isEqualTo(T1);
        assertThat(after.version()).isEqualTo(before.version());
        assertThat(before.name()).isEqualTo("ACME Ltd");
    }

    @Test
    @DisplayName("AC-2: changing a company with a VAT number to an individual without dropping the VAT number is refused")
    void type_change_keeps_the_vat_rule() {
        assertThatThrownBy(() -> company().update(CustomerType.INDIVIDUAL, "Jane", null, null, "FR123", T1))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    @DisplayName("AC-6: deactivate makes the customer inactive; an inactive customer refuses update")
    void inactive_customer_refuses_update() {
        Customer inactive = company().deactivate(T1);

        assertThat(inactive.status()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(inactive.updatedAt()).isEqualTo(T1);
        assertThatThrownBy(() -> inactive.update(CustomerType.COMPANY, "Other", null, null, null, T1))
                .isInstanceOf(CustomerInactiveException.class);
    }

    @Test
    @DisplayName("AC-6: reactivate makes it active again; both operations change nothing when the status is already right")
    void reactivate_and_no_op() {
        Customer active = company();
        Customer inactive = active.deactivate(T1);

        assertThat(inactive.reactivate(T1).status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(active.reactivate(T1)).isSameAs(active);
        assertThat(inactive.deactivate(T1)).isSameAs(inactive);
    }
}
