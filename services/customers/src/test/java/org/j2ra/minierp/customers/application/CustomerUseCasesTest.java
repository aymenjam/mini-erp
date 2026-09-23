package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.CreateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerInactiveException;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.j2ra.minierp.customers.domain.CustomerStatus;
import org.j2ra.minierp.customers.domain.CustomerType;
import org.j2ra.minierp.customers.domain.CustomerVersionConflictException;
import org.j2ra.minierp.customers.domain.InvalidCustomerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AC-13: use case tests with a fake output port and a fixed clock. No Spring. */
class CustomerUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryCustomerRepository repository = new InMemoryCustomerRepository();

    private Customer existing() {
        return repository.save(Customer.create(CustomerType.COMPANY, "ACME Ltd", null, null, "FR1", Instant.parse("2026-01-01T00:00:00Z")));
    }

    private static UpdateCustomerUseCase.Command update(Customer customer, long version, String name) {
        return new UpdateCustomerUseCase.Command(customer.id(), version, CustomerType.COMPANY, name, null, null, "FR1");
    }

    @Test
    @DisplayName("AC-1: create builds an active customer with the time of the clock and stores it once")
    void create_stores_the_customer() {
        Customer created = new CreateCustomerService(repository, CLOCK)
                .create(new CreateCustomerUseCase.Command(CustomerType.INDIVIDUAL, "Jane Doe", "jane@x.io", null, null));

        assertThat(created.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(repository.stored).containsEntry(created.id(), created);
        assertThat(repository.saves).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-2: create with an invalid customer stores nothing")
    void create_invalid_stores_nothing() {
        assertThatThrownBy(() -> new CreateCustomerService(repository, CLOCK)
                .create(new CreateCustomerUseCase.Command(CustomerType.INDIVIDUAL, "Jane", null, null, "FR1")))
                .isInstanceOf(InvalidCustomerException.class);
        assertThat(repository.saves).isZero();
    }

    @Test
    @DisplayName("AC-3: get returns the customer, and throws not found for an unknown id")
    void get_and_not_found() {
        Customer customer = existing();

        assertThat(new GetCustomerService(repository).get(customer.id())).isEqualTo(customer);
        assertThatThrownBy(() -> new GetCustomerService(repository).get(UUID.randomUUID()))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("AC-4: update with the current version changes the customer and sets the time of the clock")
    void update_with_current_version() {
        Customer customer = existing();

        Customer changed = new UpdateCustomerService(repository, CLOCK).update(update(customer, customer.version(), "New name"));

        assertThat(changed.name()).isEqualTo("New name");
        assertThat(changed.updatedAt()).isEqualTo(NOW);
        assertThat(repository.stored.get(customer.id())).isEqualTo(changed);
    }

    @Test
    @DisplayName("AC-4: update with an old version throws a conflict and writes nothing")
    void update_with_old_version() {
        Customer customer = existing();
        int savesBefore = repository.saves;

        assertThatThrownBy(() -> new UpdateCustomerService(repository, CLOCK).update(update(customer, customer.version() + 1, "X")))
                .isInstanceOf(CustomerVersionConflictException.class);
        assertThat(repository.saves).isEqualTo(savesBefore);
    }

    @Test
    @DisplayName("AC-3: update of an unknown customer throws not found")
    void update_unknown() {
        assertThatThrownBy(() -> new UpdateCustomerService(repository, CLOCK)
                .update(new UpdateCustomerUseCase.Command(UUID.randomUUID(), 0, CustomerType.COMPANY, "X", null, null, null)))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("AC-6: deactivate, then update is refused, then reactivate allows the update")
    void deactivate_update_reactivate() {
        Customer customer = existing();

        Customer inactive = new DeactivateCustomerService(repository, CLOCK).deactivate(customer.id());
        assertThat(inactive.status()).isEqualTo(CustomerStatus.INACTIVE);
        assertThatThrownBy(() -> new UpdateCustomerService(repository, CLOCK).update(update(inactive, inactive.version(), "X")))
                .isInstanceOf(CustomerInactiveException.class);

        Customer active = new ReactivateCustomerService(repository, CLOCK).reactivate(customer.id());
        assertThat(active.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(new UpdateCustomerService(repository, CLOCK).update(update(active, active.version(), "X")).name()).isEqualTo("X");
    }

    @Test
    @DisplayName("AC-6: deactivating an inactive customer writes nothing")
    void deactivate_twice_writes_once() {
        Customer customer = existing();
        var service = new DeactivateCustomerService(repository, CLOCK);
        service.deactivate(customer.id());
        int savesBefore = repository.saves;

        service.deactivate(customer.id());

        assertThat(repository.saves).isEqualTo(savesBefore);
    }

    @Test
    @DisplayName("AC-3: deactivate and reactivate of an unknown customer throw not found")
    void status_change_unknown() {
        assertThatThrownBy(() -> new DeactivateCustomerService(repository, CLOCK).deactivate(UUID.randomUUID()))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> new ReactivateCustomerService(repository, CLOCK).reactivate(UUID.randomUUID()))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
