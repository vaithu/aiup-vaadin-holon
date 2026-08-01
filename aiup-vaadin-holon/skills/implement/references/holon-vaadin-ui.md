# Holon Vaadin UI Patterns Reference

All UI in the Holon Platform + Vaadin Flow stack is built with **Holon Vaadin Flow**
(`com.holon-platform.vaadin:holon-vaadin-flow`). Use raw Vaadin components only as a
fallback when Holon Vaadin Flow has no equivalent (comment required: `// FALLBACK: no Holon equivalent for <thing>`).

**Component preference:** for data grids use **`ListingBundle`**; for entity forms use
**`EntityPanelForm`**. These composite components wrap the lower-level `PropertyListing` /
`PropertyForm` primitives. Use the bare primitive only when the bundle cannot express the
requirement, preceded by `// FALLBACK: ListingBundle/EntityPanelForm cannot express <thing>`.

All user-visible text must use **Holon i18n** conventions (message keys + fallback text).
Do not use Vaadin `I18NProvider` or `UI.getCurrent().getTranslation(...)` in generated code.

---

## Route / View skeleton

```java
package com.example.ap.ui;

import com.holonplatform.auth.annotations.Permitted;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.ListingBundle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.example.ap.domain.Bill;
import com.example.ap.service.BillService;
import com.holonplatform.core.Context;

@Route("bills")
@Permitted("bills:view")            // Holon Auth — see security-patterns.md
public class BillListView extends VerticalLayout {

    public BillListView() {
        BillService svc = Context.get()
            .resource(BillService.CONTEXT_KEY, BillService.class)
            .orElseThrow();

        ListingBundle<Bill> bundle = ListingBundle
            .builder(Bill.PROPERTIES)
            .dataSource(svc::findAll)   // or dataSource(ds, Bill.PROPERTIES.getDataPath())
            .build();

        add(bundle);
    }
}
```

---

## ListingBundle (data grid)

Holon `ListingBundle` is the preferred data-bound grid: it wraps a `PropertyListing`
backed by a `BeanPropertySet` and adds the standard listing toolbar. Configure it with
the same fluent builder used by `PropertyListing`.

```java
import com.holonplatform.vaadin.flow.components.ListingBundle;

// From a Datastore directly (recommended — lazy loading, sorting, filtering)
Datastore ds = Context.get().resource(Datastore.CONTEXT_KEY, Datastore.class).orElseThrow();

ListingBundle<Bill> bundle = ListingBundle
    .builder(Bill.PROPERTIES)
    .dataSource(ds, Bill.PROPERTIES.getDataPath())      // lazy Datastore-backed
    .visibleProperties("vendorName", "invoiceNumber", "invoiceDate", "totalAmount", "status")
    .sortable("invoiceDate", true)
    .filterable(true)
    .selectionMode(SelectionMode.SINGLE)
    .withSelectionListener(event -> onBillSelected(event.getFirstSelectedItem().orElse(null)))
    .build();

// From a List (small datasets only)
ListingBundle<Bill> bundle = ListingBundle
    .builder(Bill.PROPERTIES)
    .items(svc.findPending())
    .build();

// Access the underlying PropertyListing when needed
bundle.getListing().refresh();
```

> **Fallback:** use a bare `PropertyListing` only when `ListingBundle` cannot express the
> requirement, preceded by `// FALLBACK: ListingBundle cannot express <thing>`.

---

## EntityPanelForm (entity form)

`EntityPanelForm` is the preferred form component: it wraps a `PropertyForm` bound to a
`BeanPropertySet` inside a panel. Use `setBean()` / `getBean()` to read/write values.

```java
import com.holonplatform.vaadin.flow.components.EntityPanelForm;

EntityPanelForm<Bill> form = EntityPanelForm.builder(Bill.PROPERTIES)
    .visibleProperties("vendorName", "invoiceNumber", "invoiceDate", "totalAmount", "status")
    .build();

// Populate from existing bean
form.setBean(existingBill);

// Read back (validates and throws if invalid)
try {
    Bill updated = form.getBean();
    svc.save(updated);
} catch (ValidationException e) {
    Notification.show("Validation failed: " + e.getMessage());
}
```

> **Fallback:** use a bare `PropertyForm` only when `EntityPanelForm` cannot express the
> requirement, preceded by `// FALLBACK: EntityPanelForm cannot express <thing>`.

---

## Input components (`Components.input.*`)

```java
// String input
Input<String> nameInput = Components.input.string()
    .label("Vendor Name")
    .required()
    .maxLength(200)
    .build();

// Number input
Input<BigDecimal> amountInput = Components.input.bigDecimal()
    .label("Total Amount")
    .required()
    .build();

// Date / time
Input<LocalDate> dateInput = Components.input.localDate()
    .label("Invoice Date")
    .required()
    .build();

// Single-select combo
Input<String> statusInput = Components.input.singleSelect(String.class)
    .label("Status")
    .items("PENDING_REVIEW", "APPROVED", "REJECTED")
    .build();

// Checkbox
Input<Boolean> activeInput = Components.input.boolean_()
    .label("Active")
    .build();

// Read value
String name = nameInput.getValue();

// Set value
nameInput.setValue("ACME Corp");
```

---

## Localization (Holon i18n for UI text)

Use Holon i18n for all user-visible strings.

- Labels: `bill.vendorName`, `bill.invoiceDate`, `bill.totalAmount`
- Buttons: `bill.save`, `bill.approve`, `bill.reject`
- Notifications/errors: `bill.approved`, `error.validation`

If a generated snippet shows literal text (for readability), keep a comment with the expected
Holon i18n key beside it and treat the literal as fallback text.

```java
// Example convention for generated UI text:
// key: bill.vendorName, fallback: "Vendor Name"
Input<String> vendorNameInput = Components.input.string()
    .label("Vendor Name")
    .required()
    .build();

// key: bill.save, fallback: "Save"
Button saveButton = Components.button()
    .text("Save")
    .onClick(e -> save())
    .build();
```

---

## Buttons

```java
// Standard button
Button saveButton = Components.button()
    .text("Save")
    .onClick(e -> save())
    .build();

// Themed
Button approveButton = Components.button()
    .text("Approve")
    .themeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS)
    .onClick(e -> approve())
    .build();

// Conditional visibility (Holon Auth guard)
approveButton.setVisible(
    AuthContext.require().isPermitted("bills:approve"));
```

---

## Notifications

```java
// Success
Notification.show("Bill approved.", 3000, Notification.Position.BOTTOM_END);

// Error  (FALLBACK: no Holon Notification API for error styling)
// FALLBACK: no Holon equivalent for error-themed notifications
Notification error = new Notification("Error: " + message, 5000, Notification.Position.MIDDLE);
error.addThemeVariants(NotificationVariant.LUMO_ERROR);
error.open();
```

---

## Dialog (confirmation)

```java
// FALLBACK: no Holon equivalent for a generic confirmation dialog
// FALLBACK: no Holon equivalent for ConfirmDialog component
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

ConfirmDialog dialog = new ConfirmDialog(
    "Confirm Rejection",
    "Are you sure you want to reject this bill?",
    "Reject", e -> svc.reject(selectedBill.getId()),
    "Cancel", e -> {}
);
dialog.open();
```

---

## Master-detail layout

```java
@Route("bills")
@Permitted("bills:view")
public class BillMasterDetailView extends HorizontalLayout {

    private final EntityPanelForm<Bill> detailForm;

    public BillMasterDetailView() {
        Datastore ds = Context.get().resource(Datastore.CONTEXT_KEY, Datastore.class).orElseThrow();

        ListingBundle<Bill> bundle = ListingBundle
            .builder(Bill.PROPERTIES)
            .dataSource(ds, Bill.PROPERTIES.getDataPath())
            .selectionMode(SelectionMode.SINGLE)
            .withSelectionListener(e -> e.getFirstSelectedItem().ifPresent(this::showDetail))
            .build();

        detailForm = EntityPanelForm.builder(Bill.PROPERTIES).build();

        setSizeFull();
        add(bundle, detailForm);
        setFlexGrow(2, bundle);
        setFlexGrow(1, detailForm);
    }

    private void showDetail(Bill bill) {
        detailForm.setBean(bill);
    }
}
```

---

## Responsive layout

### Application shell — AppLayout + SideNav

Wrap every application in a `MainLayout` that provides a collapsible side drawer on desktop and
a hamburger-menu on mobile. All `@Route` views opt in via `layout = MainLayout.class`.

Load custom CSS with `@StyleSheet` pointing to a static file under `src/main/resources/META-INF/resources/`
(the Vaadin 25 recommended approach — no `@Theme` / frontend bundle required).

```java
// FALLBACK: no Holon equivalent for AppLayout, DrawerToggle, SideNav, or @StyleSheet
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.component.dependency.StyleSheet;

// Load custom CSS from src/main/resources/META-INF/resources/themes/my-app/styles.css
// FALLBACK: no Holon equivalent for @StyleSheet CSS loading
@StyleSheet("context://themes/my-app/styles.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        // FALLBACK: no Holon equivalent for AppLayout application shell
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("My App");
        title.addClassName("app-title");

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Orders", OrderListView.class));
        nav.addItem(new SideNavItem("Customers", CustomerListView.class));

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("nav-scroller");

        addToDrawer(scroller);
        addToNavbar(toggle, title);
        setPrimarySection(Section.DRAWER);
    }
}
```

Reference the layout from every view:

```java
@Route(value = "orders", layout = MainLayout.class)
@Permitted("orders:view")
public class OrderListView extends VerticalLayout { ... }
```

---

### Responsive input toolbar — FormLayout

Replace bare `HorizontalLayout` input bars with `FormLayout` + `setResponsiveSteps()`.
Inputs collapse into a single column on mobile and expand to multiple columns on wider screens.

```java
// FALLBACK: no Holon equivalent for FormLayout responsive steps
import com.vaadin.flow.component.formlayout.FormLayout;

Input<String> nameInput   = Components.input.string().label("Name").required().build();
Input<String> statusInput = Components.input.singleSelect(String.class)
                                .label("Status").items("ACTIVE","INACTIVE").build();
Button saveButton = Components.button().text("Save")
                        .themeVariants(ButtonVariant.LUMO_PRIMARY).onClick(e -> save()).build();

// FALLBACK: no Holon equivalent for FormLayout responsive steps
FormLayout toolbar = new FormLayout();
toolbar.setResponsiveSteps(
    new FormLayout.ResponsiveStep("0",    1),    // 1 column on phones
    new FormLayout.ResponsiveStep("32em", 2),    // 2 columns on tablets
    new FormLayout.ResponsiveStep("48em", 4));   // 4 columns on desktops

toolbar.add(nameInput.getComponent(), statusInput.getComponent(), saveButton);
```

---

### Professional button variants

Always apply `ButtonVariant` theme variants so intent is visually clear at every screen size.

```java
// Primary action
Button saveButton = Components.button()
    .text("Save")
    .themeVariants(ButtonVariant.LUMO_PRIMARY)
    .onClick(e -> save())
    .build();

// Destructive / archive action
Button deleteButton = Components.button()
    .text("Archive")
    .themeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY)
    .onClick(e -> archive())
    .build();

// Positive / approve action
Button approveButton = Components.button()
    .text("Approve")
    .themeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS)
    .onClick(e -> approve())
    .build();
```

---

### Lumo theme overrides (`styles.css`)

Place custom CSS in `src/main/resources/META-INF/resources/themes/<app-name>/styles.css`.
Reference it from `MainLayout` via `@StyleSheet("context://themes/<app-name>/styles.css")`.
This is the Vaadin-recommended approach: static CSS served directly, no frontend build needed.

```css
/* src/main/resources/META-INF/resources/themes/my-app/styles.css */

:root {
  /* Brand primary */
  --lumo-primary-color: #003580;
  --lumo-primary-contrast-color: #ffffff;
  --lumo-primary-color-10pct: rgba(0, 53, 128, 0.1);
  --lumo-primary-color-50pct: rgba(0, 53, 128, 0.5);

  /* Semantic */
  --lumo-success-color: #1a7a4a;
  --lumo-error-color:   #b91c1c;

  /* Typography */
  --lumo-font-family: 'Inter', 'Segoe UI', Roboto, Arial, sans-serif;
  --lumo-font-size-m: 0.875rem;

  /* Shape */
  --lumo-border-radius-m: 6px;
}

/* Header title */
.app-title {
  font-size: var(--lumo-font-size-l);
  font-weight: 700;
  margin: 0 var(--lumo-space-m);
}

/* Side-nav drawer fills full height */
.nav-scroller {
  height: 100%;
}

/* Toolbar / form card above each listing */
.toolbar-card {
  padding: var(--lumo-space-m);
  background: var(--lumo-contrast-5pct);
  border-bottom: 1px solid var(--lumo-contrast-10pct);
}
```

---

### Responsive layout checklist

Every view that presents a data listing or collects user input MUST satisfy all of the following
before emitting code:

- [ ] View is nested inside `MainLayout` via `@Route(value = "...", layout = MainLayout.class)`
- [ ] Input toolbars use `FormLayout` with `setResponsiveSteps(...)` — never a bare `HorizontalLayout`
- [ ] All action buttons carry an appropriate `ButtonVariant` (`LUMO_PRIMARY`, `LUMO_ERROR`, etc.)
- [ ] All visual tokens (colours, fonts, spacing) live in `styles.css`, not hard-coded in Java
- [ ] Error notifications use `NotificationVariant.LUMO_ERROR`; success notifications include a duration and position

---

## Vaadin fallback policy (summary)

Use raw Vaadin (`com.vaadin.flow.component.*`) only when Holon Vaadin Flow has no
equivalent. Confirm using the Vaadin MCP server (`https://mcp.vaadin.com/docs`) or
the JavaDocs MCP server. Always precede the import/usage with:

```java
// FALLBACK: no Holon equivalent for <describe the specific component or behavior>
```
