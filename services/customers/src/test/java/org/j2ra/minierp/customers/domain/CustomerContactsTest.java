package org.j2ra.minierp.customers.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Spec 1.1, AC-7 and AC-13: the rules of the contacts of a customer. Plain JUnit, no Spring. */
class CustomerContactsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

    private static Customer customer() {
        return Customer.create(CustomerType.COMPANY, "ACME Ltd", null, null, null, T0);
    }

    private static Contact contact(String first, String last, boolean primary) {
        return Contact.create(first, last, null, null, null, primary);
    }

    @Test
    @DisplayName("AC-7: a contact is added, with a random UUID v4 id, and the update time changes")
    void add_contact() {
        Contact jane = contact("Jane", "Doe", false);

        Customer changed = customer().addContact(jane, T1);

        assertThat(changed.contacts()).containsExactly(jane);
        assertThat(jane.id().version()).isEqualTo(4);
        assertThat(changed.updatedAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("AC-7: a new primary contact replaces the old primary contact")
    void new_primary_replaces_old_primary() {
        Contact first = contact("Jane", "Doe", true);
        Contact second = contact("John", "Smith", true);

        Customer changed = customer().addContact(first, T0).addContact(second, T1);

        assertThat(changed.contacts()).filteredOn(Contact::primary).extracting(Contact::id).containsExactly(second.id());
        assertThat(changed.contacts()).hasSize(2);
    }

    @Test
    @DisplayName("AC-7: a contact that is not primary does not change the primary contact")
    void non_primary_keeps_the_primary() {
        Contact first = contact("Jane", "Doe", true);

        Customer changed = customer().addContact(first, T0).addContact(contact("John", "Smith", false), T1);

        assertThat(changed.contacts()).filteredOn(Contact::primary).extracting(Contact::id).containsExactly(first.id());
    }

    @Test
    @DisplayName("AC-7: replacing a contact changes its fields, and making it primary demotes the other one")
    void replace_contact() {
        Contact first = contact("Jane", "Doe", true);
        Contact second = contact("John", "Smith", false);
        Customer customer = customer().addContact(first, T0).addContact(second, T0);

        Customer changed = customer.replaceContact(new Contact(second.id(), "Johnny", "Smith", "j@x.io", null, "CEO", true), T1);

        assertThat(changed.contacts()).extracting(Contact::firstName).containsExactlyInAnyOrder("Jane", "Johnny");
        assertThat(changed.contacts()).filteredOn(Contact::primary).extracting(Contact::id).containsExactly(second.id());
        assertThat(customer.contacts()).extracting(Contact::firstName).contains("John");
    }

    @Test
    @DisplayName("AC-7: a contact is removed; an unknown contact gives ContactNotFoundException")
    void remove_contact() {
        Contact jane = contact("Jane", "Doe", false);
        Customer customer = customer().addContact(jane, T0);

        assertThat(customer.removeContact(jane.id(), T1).contacts()).isEmpty();
        assertThatThrownBy(() -> customer.removeContact(UUID.randomUUID(), T1)).isInstanceOf(ContactNotFoundException.class);
        assertThatThrownBy(() -> customer.replaceContact(contact("A", "B", false), T1)).isInstanceOf(ContactNotFoundException.class);
    }

    @Test
    @DisplayName("AC-7: first name and last name are required; text fields are trimmed; the limits are checked")
    void contact_field_rules() {
        assertThatThrownBy(() -> contact(" ", "Doe", false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("firstName"));
        assertThatThrownBy(() -> contact("Jane", null, false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("lastName"));
        assertThatThrownBy(() -> contact("x".repeat(101), "Doe", false)).isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Contact.create("Jane", "Doe", null, "1".repeat(51), null, false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("phone"));
        assertThatThrownBy(() -> Contact.create("Jane", "Doe", null, null, "x".repeat(101), false))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("jobTitle"));

        Contact cleaned = Contact.create("  Jane ", " Doe", "  ", " 0102 ", null, false);
        assertThat(cleaned.firstName()).isEqualTo("Jane");
        assertThat(cleaned.lastName()).isEqualTo("Doe");
        assertThat(cleaned.email()).isNull();
        assertThat(cleaned.phone()).isEqualTo("0102");
    }

    @Test
    @DisplayName("AC-7: an inactive customer refuses every change of its contacts")
    void inactive_customer_refuses_contact_changes() {
        Contact jane = contact("Jane", "Doe", false);
        Customer inactive = customer().addContact(jane, T0).deactivate(T1);

        assertThatThrownBy(() -> inactive.addContact(contact("A", "B", false), T1)).isInstanceOf(CustomerInactiveException.class);
        assertThatThrownBy(() -> inactive.replaceContact(jane, T1)).isInstanceOf(CustomerInactiveException.class);
        assertThatThrownBy(() -> inactive.removeContact(jane.id(), T1)).isInstanceOf(CustomerInactiveException.class);
    }

    @Test
    @DisplayName("AC-7: a customer built with two primary contacts is refused (also when read from the database)")
    void constructor_refuses_two_primary_contacts() {
        assertThatThrownBy(() -> new Customer(UUID.randomUUID(), CustomerType.COMPANY, "ACME", null, null, null,
                CustomerStatus.ACTIVE, T0, T0, 0, List.of(contact("A", "A", true), contact("B", "B", true)), List.of()))
                .isInstanceOfSatisfying(InvalidCustomerException.class, e -> assertThat(e.field()).isEqualTo("contacts"));
    }

    @Test
    @DisplayName("AC-7: contacts are listed by last name, then first name")
    void contacts_have_a_stable_order() {
        Customer customer = customer().addContact(contact("Zoe", "Brown", false), T0)
                .addContact(contact("Anna", "Brown", false), T0).addContact(contact("Max", "Adams", false), T0);

        assertThat(customer.contacts()).extracting(Contact::firstName).containsExactly("Max", "Anna", "Zoe");
    }
}
