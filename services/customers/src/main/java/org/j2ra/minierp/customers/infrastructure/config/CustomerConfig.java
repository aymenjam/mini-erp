package org.j2ra.minierp.customers.infrastructure.config;

import org.j2ra.minierp.customers.application.ListCustomersService;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.infrastructure.out.persistence.CustomerPersistenceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring: the application layer has no Spring annotations, so its beans are created here. */
@Configuration
class CustomerConfig {

    @Bean
    CustomerRepository customerRepository() {
        return new CustomerPersistenceAdapter();
    }

    @Bean
    ListCustomersUseCase listCustomersUseCase(CustomerRepository customerRepository) {
        return new ListCustomersService(customerRepository);
    }
}
