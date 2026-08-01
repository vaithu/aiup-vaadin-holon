package com.example.crm.service;

import com.example.crm.domain.Customer;
import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Customer operations backed by the Holon Datastore. Retrieved from the Holon
 * {@link Context} via {@link #CONTEXT_KEY} — never annotated with {@code @Service}
 * or injected with {@code @Autowired}.
 */
public class CustomerService {

    public static final String CONTEXT_KEY = CustomerService.class.getName();

    private final Datastore datastore;

    public CustomerService() {
        this.datastore = Context.get()
                .resource(Datastore.CONTEXT_KEY, Datastore.class)
                .orElseThrow(() -> new IllegalStateException("Datastore not available in Holon Context"));
    }

    /** UC-001 / FR-002: list active customers. */
    public List<Customer> findActive() {
        return datastore.query(Customer.PROPERTIES.getDataPath())
                .filter(Customer.PROPERTIES.property("status").eq(Customer.STATUS_ACTIVE))
                .sort(Customer.PROPERTIES.property("name").asc())
                .list(Customer.PROPERTIES);
    }

    public Optional<Customer> findById(Long id) {
        return datastore.query(Customer.PROPERTIES.getDataPath())
                .filter(Customer.PROPERTIES.property("id").eq(id))
                .findOne(Customer.PROPERTIES);
    }

    /** BR-001: reject duplicate names. */
    public boolean nameExists(String name) {
        return datastore.query(Customer.PROPERTIES.getDataPath())
                .filter(Customer.PROPERTIES.property("name").eq(name))
                .count() > 0;
    }

    /** UC-001 / FR-001: register a new active customer. */
    public Customer create(String name, String industry) {
        if (nameExists(name)) {
            throw new IllegalArgumentException("A customer named '" + name + "' already exists");
        }
        Customer customer = new Customer();
        customer.setName(name);
        customer.setIndustry(industry);
        customer.setStatus(Customer.STATUS_ACTIVE);
        customer.setCreatedAt(LocalDateTime.now());
        datastore.save(Customer.PROPERTIES.getDataPath(), customer);
        return customer;
    }

    /** UC-001 A2 / FR-006: archive a customer (manager-only action, guarded in the UI). */
    public void archive(Long id) {
        findById(id).ifPresent(customer -> {
            customer.setStatus(Customer.STATUS_ARCHIVED);
            datastore.save(Customer.PROPERTIES.getDataPath(), customer);
        });
    }
}
