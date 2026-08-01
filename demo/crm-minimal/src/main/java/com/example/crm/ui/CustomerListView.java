package com.example.crm.ui;

import com.example.crm.domain.Customer;
import com.example.crm.service.CustomerService;
import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.annotations.Permitted;
import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.Input;
import com.holonplatform.vaadin.flow.components.PropertyListing;
import com.holonplatform.vaadin.flow.components.SelectionMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * UC-001 — Manage Customers. Route guarded by Holon Auth ({@code customers:view}).
 * The "Archive" action is additionally gated on {@code customers:archive}.
 */
@Route(value = "customers", layout = MainLayout.class)
@Permitted("customers:view")
public class CustomerListView extends VerticalLayout {

    private final CustomerService customerService;
    private final PropertyListing listing;

    private Customer selected;

    public CustomerListView() {
        this.customerService = Context.get()
                .resource(CustomerService.CONTEXT_KEY, CustomerService.class)
                .orElseThrow();

        Datastore ds = Context.get()
                .resource(Datastore.CONTEXT_KEY, Datastore.class)
                .orElseThrow();

        listing = PropertyListing.builder(Customer.PROPERTIES)
                .dataSource(ds, Customer.PROPERTIES.getDataPath())
                .visibleProperties("name", "industry", "status", "createdAt")
                .selectionMode(SelectionMode.SINGLE)
                .withSelectionListener(e -> selected = e.getFirstSelectedItem().orElse(null))
                .build();

        setSizeFull();
        setPadding(false);
        add(buildCreateForm(), listing);
    }

    // FALLBACK: no Holon equivalent for FormLayout responsive steps
    private FormLayout buildCreateForm() {
        Input<String> nameInput = Components.input.string()
                .label("Company name")
                .required()
                .maxLength(200)
                .build();

        Input<String> industryInput = Components.input.string()
                .label("Industry")
                .maxLength(100)
                .build();

        Button addButton = Components.button()
                .text("Add customer")
                .themeVariants(ButtonVariant.LUMO_PRIMARY)
                .onClick(e -> {
                    try {
                        customerService.create(nameInput.getValue(), industryInput.getValue());
                        nameInput.clear();
                        industryInput.clear();
                        listing.refresh();
                        showSuccess("Customer added.");
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                })
                .build();

        Button archiveButton = Components.button()
                .text("Archive selected")
                .themeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY)
                .onClick(e -> {
                    if (selected == null) {
                        showError("Select a customer first.");
                        return;
                    }
                    customerService.archive(selected.getId());
                    listing.refresh();
                    showSuccess("Customer archived.");
                })
                .build();

        // BR-002 / NFR-002: only managers with customers:archive may archive.
        archiveButton.setVisible(AuthContext.require().isPermitted("customers:archive"));

        // FALLBACK: no Holon equivalent for FormLayout responsive steps
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0",    1),
                new FormLayout.ResponsiveStep("32em", 2),
                new FormLayout.ResponsiveStep("48em", 4));
        form.addClassName("crm-toolbar");
        form.add(nameInput.getComponent(), industryInput.getComponent(), addButton, archiveButton);
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
