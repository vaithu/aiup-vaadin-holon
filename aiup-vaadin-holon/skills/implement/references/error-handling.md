# Error Handling Reference

This document covers how to surface validation errors, datastore exceptions, and
optimistic-lock conflicts in the Holon Platform + Vaadin Flow stack.

---

## 1 · Form validation errors (`EntityFormPanel`)

`EntityFormPanel` validates on save automatically from Jakarta Bean Validation annotations
(`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`) declared on the bean.
**Do not add manual `.withValidator(...)` calls** inside the form — they belong on the bean.

```java
EntityFormPanel<Bill> form = EntityFormPanel.bean(Bill.class)
    .autoRequiredIndicators(true)              // infer required from @NotNull / @NotBlank
    .saveButton(btn -> btn.text("Save bill"), bill -> {
        try {
            svc.save(bill);
            NotificationUtil.notificationSuccess("Bill saved.");
            Navigator.get().navigateTo(BillListView.class);
        } catch (ValidationException e) {
            // Show all field-level validation errors in a toast
            NotificationUtil.notificationError(e);
        } catch (Exception e) {
            log.error("Unexpected error saving bill", e);
            NotificationUtil.notificationError("An unexpected error occurred. Please try again.");
        }
    })
    .cancelButton(btn -> btn.text("Cancel"),
        () -> Navigator.get().navigateBack())
    .build();
```

> `NotificationUtil.notificationError(ValidationException)` iterates the violation list
> and formats each field name + message as a single error toast. Always prefer this overload
> over building the message string manually.

---

## 2 · Optimistic-lock conflicts (`@Version`)

Every domain bean carries a `@Version Long version` field. The Holon Datastore
checks the version before issuing an `UPDATE`; if the row was modified by another user
since the bean was loaded, it throws `DataAccessException` (wrapping a version mismatch).

Pattern: catch the exception in the save callback, reload the bean, and prompt the user.

```java
import com.holonplatform.core.exceptions.DataAccessException;

EntityFormPanel<Bill> form = EntityFormPanel.bean(Bill.class)
    .autoRequiredIndicators(true)
    .saveButton(btn -> btn.text("Save"), bill -> {
        try {
            svc.save(bill);
            NotificationUtil.notificationSuccess("Bill saved.");
            Navigator.get().navigateTo(BillListView.class);
        } catch (DataAccessException e) {
            if (isVersionConflict(e)) {
                // Another user saved this record while you were editing.
                // Reload and warn the user — do NOT silently overwrite.
                Components.alertModal(Alert.Variant.WARNING)
                    .title("Concurrent update detected")
                    .description(
                        "This record was modified by another user. " +
                        "Your changes have NOT been saved. " +
                        "Please review the latest version and re-apply your changes.")
                    .closeButton()
                    .open();
                // Reload the latest version into the form
                svc.findById(bill.getId()).ifPresent(form::setBean);
            } else {
                log.error("Datastore error saving bill {}", bill.getId(), e);
                NotificationUtil.notificationError("Failed to save. Please try again.");
            }
        }
    })
    .build();

// Helper — detects optimistic-lock messages from common database drivers
private static boolean isVersionConflict(DataAccessException e) {
    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
    return msg.contains("version") || msg.contains("optimistic") || msg.contains("concurrent");
}
```

---

## 3 · Service-layer error handling

Keep try/catch **out of** service methods — let exceptions propagate to the view so that
UI-level feedback (notifications, dialogs) stays in the view layer.

```java
// Service — NO try/catch; let exceptions propagate
public class BillService {

    private final BeanDatastoreHelper<Bill> helper;

    public BillService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
    }

    public void save(Bill bill) {
        // Audit fields must be set before saving (see audit-wiring.md)
        helper.save(bill);   // throws DataAccessException on error
    }
}
```

The view catches what it can handle; everything else is a programming error that should
be logged and shown as a generic "unexpected error" message.

---

## 4 · Global error / not-found view

Implement an `ErrorHandler` view for 404 (route not found) and uncaught exceptions:

```java
package com.example.ap.shared;

import com.holonplatform.vaadin.flow.navigator.Navigator;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;

@Route("not-found")
public class NotFoundView extends Layout implements HasErrorParameter<NotFoundException> {

    public NotFoundView() {
        setSizeFull();
        add(
            new H2("Page not found"),
            new Paragraph("The page you requested does not exist."),
            Components.button("Go home", e -> Navigator.get().navigateToDefault()).build()
        );
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return com.vaadin.flow.server.HttpStatusCode.NOT_FOUND.getCode();
    }
}
```

For uncaught exceptions on a route, implement `HasErrorParameter<Exception>`:

```java
@Route("error")
public class InternalErrorView extends Layout implements HasErrorParameter<Exception> {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(InternalErrorView.class);

    public InternalErrorView() {
        setSizeFull();
        add(
            new H2("Something went wrong"),
            new Paragraph("An unexpected error occurred. Please try again or contact support."),
            Components.button("Go home", e -> Navigator.get().navigateToDefault()).build()
        );
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        log.error("Unhandled navigation error", parameter.getException());
        return com.vaadin.flow.server.HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
    }
}
```

---

## 5 · Access-denied view

```java
package com.example.ap.shared;

import com.holonplatform.vaadin.flow.navigator.Navigator;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("access-denied")
public class AccessDeniedView extends Layout {

    public AccessDeniedView() {
        setSizeFull();
        add(
            new H2("Access denied"),
            new Paragraph("You do not have permission to access this page."),
            Components.button("Go home", e -> Navigator.get().navigateToDefault()).build()
        );
    }
}
```

Reroute to this view from `BeforeEnterObserver.beforeEnter`:

```java
@Override
public void beforeEnter(BeforeEnterEvent event) {
    if (!AuthContext.require().isPermitted("bills:approve")) {
        event.rerouteTo(AccessDeniedView.class);
    }
}
```

---

## Pre-Emit Checklist (error handling)

- [ ] Save callbacks in `EntityFormPanel` catch `ValidationException` and call `NotificationUtil.notificationError(e)` — not a generic string
- [ ] Save callbacks catch `DataAccessException`; version-conflict cases show an alert dialog + reload the form bean; do NOT silently overwrite
- [ ] Service methods do NOT catch exceptions — let them propagate to the view
- [ ] A `NotFoundView` implementing `HasErrorParameter<NotFoundException>` exists in `shared`
- [ ] An `InternalErrorView` implementing `HasErrorParameter<Exception>` exists in `shared` and logs the exception with SLF4J
- [ ] An `AccessDeniedView` exists in `shared`; `beforeEnter` reroutes to it on permission failure
