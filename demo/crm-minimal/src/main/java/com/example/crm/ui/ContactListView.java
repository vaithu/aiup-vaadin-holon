package com.example.crm.ui;

import com.example.crm.domain.Contact;
import com.example.crm.domain.Customer;
import com.example.crm.service.ContactService;
import com.example.crm.service.CustomerService;
import com.holonplatform.auth.annotations.Permitted;
import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.Input;
import com.holonplatform.vaadin.flow.components.PropertyListing;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * UC-002 — Manage Contacts. Route guarded by Holon Auth ({@code contacts:view}).
 */
@Route(value = "contacts", layout = MainLayout.class)
@Permitted("contacts:view")
public class ContactListView extends VerticalLayout {

    private final ContactService contactService;
    private final CustomerService customerService;
    private final PropertyListing listing;

    public ContactListView() {
        this.contactService = Context.get()
                .resource(ContactService.CONTEXT_KEY, ContactService.class)
                .orElseThrow();
        this.customerService = Context.get()
                .resource(CustomerService.CONTEXT_KEY, CustomerService.class)
                .orElseThrow();

        Datastore ds = Context.get()
                .resource(Datastore.CONTEXT_KEY, Datastore.class)
                .orElseThrow();

        listing = PropertyListing.builder(Contact.PROPERTIES)
                .dataSource(ds, Contact.PROPERTIES.getDataPath())
                .visibleProperties("firstName", "lastName", "email", "phone", "customerId")
                .build();

        setSizeFull();
        setPadding(false);
        add(buildCreateForm(), listing);
    }

    // FALLBACK: no Holon equivalent for FormLayout responsive steps
    private FormLayout buildCreateForm() {
        Input<Long> customerInput = Components.input.singleSelect(Long.class)
                .label("Customer")
                .items(customerService.findActive().stream().map(Customer::getId).toList())
                .required()
                .build();

        Input<String> firstNameInput = Components.input.string()
                .label("First name").required().maxLength(100).build();
        Input<String> lastNameInput = Components.input.string()
                .label("Last name").required().maxLength(100).build();
        Input<String> emailInput = Components.input.string()
                .label("Email").required().maxLength(200).build();
        Input<String> phoneInput = Components.input.string()
                .label("Phone").maxLength(40).build();

        Button addButton = Components.button()
                .text("Add contact")
                .themeVariants(ButtonVariant.LUMO_PRIMARY)
                .onClick(e -> {
                    String email = emailInput.getValue();
                    // BR-004 / FR: basic email format check.
                    if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                        showError("Enter a valid email address.");
                        return;
                    }
                    if (customerInput.getValue() == null) {
                        showError("Select a customer.");
                        return;
                    }
                    contactService.create(customerInput.getValue(), firstNameInput.getValue(),
                            lastNameInput.getValue(), email, phoneInput.getValue());
                    firstNameInput.clear();
                    lastNameInput.clear();
                    emailInput.clear();
                    phoneInput.clear();
                    listing.refresh();
                    showSuccess("Contact added.");
                })
                .build();

        // FALLBACK: no Holon equivalent for FormLayout responsive steps
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0",    1),
                new FormLayout.ResponsiveStep("32em", 2),
                new FormLayout.ResponsiveStep("48em", 3));
        form.addClassName("crm-toolbar");
        form.add(customerInput.getComponent(), firstNameInput.getComponent(),
                lastNameInput.getComponent(), emailInput.getComponent(),
                phoneInput.getComponent(), addButton);
        return form;
    }

    private static void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END);
    }

    // FALLBACK: no Holon equivalent for error-themed notifications
    private static void showError(String message) {
        Notification n = new Notification(message, 4000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.open();
    }
}
