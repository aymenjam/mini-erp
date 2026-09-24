package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.AddAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.AddContactUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateContactUseCase;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.AddressNotFoundException;
import org.j2ra.minierp.customers.domain.AddressType;
import org.j2ra.minierp.customers.domain.Contact;
import org.j2ra.minierp.customers.domain.ContactNotFoundException;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerInactiveException;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.j2ra.minierp.customers.domain.CustomerType;
import org.j2ra.minierp.customers.domain.InvalidCustomerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AC-7, AC-8, AC-13: use case tests for contacts and addresses, with a fake output port and a fixed clock. No Spring. */
class ContactAndAddressUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryCustomerRepository repository = new InMemoryCustomerRepository();

    private Customer existing() {
        return repository.save(Customer.create(CustomerType.COMPANY, "ACME Ltd", null, null, null, Instant.parse("2026-01-01T00:00:00Z")));
    }

    private static AddContactUseCase.Command addContact(UUID customerId, boolean primary) {
        return new AddContactUseCase.Command(customerId, "Jane", "Doe", null, null, null, primary);
    }

    private static AddAddressUseCase.Command addAddress(UUID customerId, boolean isDefault) {
        return new AddAddressUseCase.Command(customerId, AddressType.BILLING, "1 rue de la Paix", null, "75002", "Paris", "fr", isDefault);
    }

    @Test
    @DisplayName("AC-7: add contact stores the customer once, returns the new contact id and uses the time of the clock")
    void add_contact() {
        Customer customer = existing();
        int savesBefore = repository.saves;

        AddContactUseCase.Result result = new AddContactService(repository, CLOCK).add(addContact(customer.id(), true));

        assertThat(result.customer().contacts()).extracting(Contact::id).containsExactly(result.contactId());
        assertThat(result.customer().updatedAt()).isEqualTo(NOW);
        assertThat(repository.saves).isEqualTo(savesBefore + 1);
        assertThat(repository.stored.get(customer.id())).isEqualTo(result.customer());
    }

    @Test
    @DisplayName("AC-7: update contact replaces it, remove contact removes it")
    void update_and_remove_contact() {
        Customer customer = existing();
        UUID contactId = new AddContactService(repository, CLOCK).add(addContact(customer.id(), false)).contactId();

        Customer updated = new UpdateContactService(repository, CLOCK).update(
                new UpdateContactUseCase.Command(customer.id(), contactId, "Janet", "Doe", null, null, "CEO", true));
        assertThat(updated.contacts()).singleElement().satisfies(c -> {
            assertThat(c.firstName()).isEqualTo("Janet");
            assertThat(c.primary()).isTrue();
        });

        assertThat(new RemoveContactService(repository, CLOCK).remove(customer.id(), contactId).contacts()).isEmpty();
    }

    @Test
    @DisplayName("AC-8: add address normalises the country, update and remove work")
    void address_use_cases() {
        Customer customer = existing();
        AddAddressUseCase.Result added = new AddAddressService(repository, CLOCK).add(addAddress(customer.id(), true));
        assertThat(added.customer().addresses()).singleElement().satisfies(a -> assertThat(a.country()).isEqualTo("FR"));

        Customer updated = new UpdateAddressService(repository, CLOCK).update(new UpdateAddressUseCase.Command(
                customer.id(), added.addressId(), AddressType.SHIPPING, "2 quai", null, null, "Lyon", "FR", false));
        assertThat(updated.addresses()).singleElement().satisfies(a -> {
            assertThat(a.type()).isEqualTo(AddressType.SHIPPING);
            assertThat(a.city()).isEqualTo("Lyon");
        });

        assertThat(new RemoveAddressService(repository, CLOCK).remove(customer.id(), added.addressId()).addresses()).isEmpty();
    }

    @Test
    @DisplayName("AC-7, AC-8: an unknown customer, contact or address throws not found and writes nothing")
    void not_found() {
        Customer customer = existing();
        int savesBefore = repository.saves;
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> new AddContactService(repository, CLOCK).add(addContact(unknown, false)))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> new AddAddressService(repository, CLOCK).add(addAddress(unknown, false)))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> new RemoveContactService(repository, CLOCK).remove(customer.id(), unknown))
                .isInstanceOf(ContactNotFoundException.class);
        assertThatThrownBy(() -> new RemoveAddressService(repository, CLOCK).remove(customer.id(), unknown))
                .isInstanceOf(AddressNotFoundException.class);
        assertThat(repository.saves).isEqualTo(savesBefore);
    }

    @Test
    @DisplayName("AC-7, AC-8: an invalid contact or address, and an inactive customer, write nothing")
    void refused_changes_write_nothing() {
        Customer customer = existing();
        int savesBefore = repository.saves;

        assertThatThrownBy(() -> new AddContactService(repository, CLOCK)
                .add(new AddContactUseCase.Command(customer.id(), " ", "Doe", null, null, null, false)))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> new AddAddressService(repository, CLOCK)
                .add(new AddAddressUseCase.Command(customer.id(), AddressType.BILLING, "l1", null, null, "Paris", "ZZ", false)))
                .isInstanceOf(InvalidCustomerException.class);

        repository.save(customer.deactivate(NOW));
        int savesAfterDeactivate = repository.saves;
        assertThatThrownBy(() -> new AddContactService(repository, CLOCK).add(addContact(customer.id(), false)))
                .isInstanceOf(CustomerInactiveException.class);
        assertThatThrownBy(() -> new AddAddressService(repository, CLOCK).add(addAddress(customer.id(), false)))
                .isInstanceOf(CustomerInactiveException.class);

        assertThat(savesAfterDeactivate).isEqualTo(savesBefore + 1);
        assertThat(repository.saves).isEqualTo(savesAfterDeactivate);
    }
}
