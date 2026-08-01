package com.example.crm.service;

import com.example.crm.domain.Contact;
import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;

import java.util.List;

/**
 * Contact operations backed by the Holon Datastore. Retrieved from the Holon
 * {@link Context} via {@link #CONTEXT_KEY} — never annotated with {@code @Service}.
 */
public class ContactService {

    public static final String CONTEXT_KEY = ContactService.class.getName();

    private final Datastore datastore;

    public ContactService() {
        this.datastore = Context.get()
                .resource(Datastore.CONTEXT_KEY, Datastore.class)
                .orElseThrow(() -> new IllegalStateException("Datastore not available in Holon Context"));
    }

    /** UC-002 / FR-005: list all contacts. */
    public List<Contact> findAll() {
        return datastore.query(Contact.PROPERTIES.getDataPath())
                .sort(Contact.PROPERTIES.property("lastName").asc())
                .list(Contact.PROPERTIES);
    }

    /** UC-002 / FR-005: list the contacts of a given customer. */
    public List<Contact> findByCustomer(Long customerId) {
        return datastore.query(Contact.PROPERTIES.getDataPath())
                .filter(Contact.PROPERTIES.property("customerId").eq(customerId))
                .sort(Contact.PROPERTIES.property("lastName").asc())
                .list(Contact.PROPERTIES);
    }

    /** UC-002 / FR-004: attach a contact to an existing customer. */
    public Contact create(Long customerId, String firstName, String lastName, String email, String phone) {
        Contact contact = new Contact();
        contact.setCustomerId(customerId);
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmail(email);
        contact.setPhone(phone);
        datastore.save(Contact.PROPERTIES.getDataPath(), contact);
        return contact;
    }
}
