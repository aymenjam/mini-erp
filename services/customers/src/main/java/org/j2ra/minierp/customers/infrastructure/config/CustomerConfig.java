package org.j2ra.minierp.customers.infrastructure.config;

import jakarta.persistence.EntityManager;
import org.j2ra.minierp.customers.application.AddAddressService;
import org.j2ra.minierp.customers.application.AddContactService;
import org.j2ra.minierp.customers.application.CreateCustomerService;
import org.j2ra.minierp.customers.application.DeactivateCustomerService;
import org.j2ra.minierp.customers.application.GetCustomerService;
import org.j2ra.minierp.customers.application.ListCustomersService;
import org.j2ra.minierp.customers.application.ReactivateCustomerService;
import org.j2ra.minierp.customers.application.RemoveAddressService;
import org.j2ra.minierp.customers.application.RemoveContactService;
import org.j2ra.minierp.customers.application.UpdateAddressService;
import org.j2ra.minierp.customers.application.UpdateContactService;
import org.j2ra.minierp.customers.application.UpdateCustomerService;
import org.j2ra.minierp.customers.application.port.in.AddAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.AddContactUseCase;
import org.j2ra.minierp.customers.application.port.in.CreateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.DeactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.GetCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.application.port.in.ReactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.RemoveAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.RemoveContactUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateContactUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.infrastructure.out.persistence.CustomerJpaRepository;
import org.j2ra.minierp.customers.infrastructure.out.persistence.CustomerPersistenceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wiring: the application layer has no Spring annotations, so its beans are created here. */
@Configuration
class CustomerConfig {

    // One clock for all use cases. Tests can replace it to get a fixed time.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CustomerRepository customerRepository(CustomerJpaRepository jpaRepository, EntityManager entityManager) {
        return new CustomerPersistenceAdapter(jpaRepository, entityManager);
    }

    @Bean
    ListCustomersUseCase listCustomersUseCase(CustomerRepository customerRepository) {
        return new ListCustomersService(customerRepository);
    }

    @Bean
    GetCustomerUseCase getCustomerUseCase(CustomerRepository customerRepository) {
        return new GetCustomerService(customerRepository);
    }

    @Bean
    CreateCustomerUseCase createCustomerUseCase(CustomerRepository customerRepository, Clock clock) {
        return new CreateCustomerService(customerRepository, clock);
    }

    @Bean
    UpdateCustomerUseCase updateCustomerUseCase(CustomerRepository customerRepository, Clock clock) {
        return new UpdateCustomerService(customerRepository, clock);
    }

    @Bean
    DeactivateCustomerUseCase deactivateCustomerUseCase(CustomerRepository customerRepository, Clock clock) {
        return new DeactivateCustomerService(customerRepository, clock);
    }

    @Bean
    ReactivateCustomerUseCase reactivateCustomerUseCase(CustomerRepository customerRepository, Clock clock) {
        return new ReactivateCustomerService(customerRepository, clock);
    }

    @Bean
    AddContactUseCase addContactUseCase(CustomerRepository customerRepository, Clock clock) {
        return new AddContactService(customerRepository, clock);
    }

    @Bean
    UpdateContactUseCase updateContactUseCase(CustomerRepository customerRepository, Clock clock) {
        return new UpdateContactService(customerRepository, clock);
    }

    @Bean
    RemoveContactUseCase removeContactUseCase(CustomerRepository customerRepository, Clock clock) {
        return new RemoveContactService(customerRepository, clock);
    }

    @Bean
    AddAddressUseCase addAddressUseCase(CustomerRepository customerRepository, Clock clock) {
        return new AddAddressService(customerRepository, clock);
    }

    @Bean
    UpdateAddressUseCase updateAddressUseCase(CustomerRepository customerRepository, Clock clock) {
        return new UpdateAddressService(customerRepository, clock);
    }

    @Bean
    RemoveAddressUseCase removeAddressUseCase(CustomerRepository customerRepository, Clock clock) {
        return new RemoveAddressService(customerRepository, clock);
    }
}
