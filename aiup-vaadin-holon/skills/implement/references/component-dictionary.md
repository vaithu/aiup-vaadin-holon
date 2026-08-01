# Holon ↔ Vaadin Component Dictionary (I18N + A11Y)

This dictionary is the authoritative lookup for every UI component used in the
Holon Platform + Vaadin Flow stack. For **every** component entry:

- **I18N column** shows the Holon i18n wiring (never Vaadin `I18NProvider` / `getTranslation`).
- **A11Y column** lists the mandatory accessibility requirements that each component must
  satisfy before code is emitted.

Skills MUST consult this file when choosing a component. If a Holon component exists for
the job, use it. Raw Vaadin is a fallback only (comment required).

---

## Quick-reference table

| Job | **Holon component** | Vaadin fallback | Notes |
|-----|---------------------|-----------------|-------|
| Single-line text input | `Components.input.string()` | `TextField` | Prefer Holon |
| Password input | `Components.input.password()` | `PasswordField` | Prefer Holon |
| Integer number | `Components.input.integer()` | `IntegerField` | Prefer Holon |
| Decimal number | `Components.input.bigDecimal()` | `BigDecimalField` / `NumberField` | Prefer Holon |
| Local date | `Components.input.localDate()` | `DatePicker` | Prefer Holon |
| Local date-time | `Components.input.localDateTime()` | `DateTimePicker` | Prefer Holon |
| Boolean / checkbox | `Components.input.boolean_()` | `Checkbox` | Prefer Holon |
| Single-select combo | `Components.input.singleSelect(T)` | `ComboBox<T>` | Prefer Holon |
| Multi-select | `Components.input.multiSelect(T)` | `MultiSelectComboBox<T>` | Prefer Holon |
| Text area | `Components.input.string().multiLine()` | `TextArea` | Prefer Holon |
| Email | `Components.input.string()` + `.validator(EmailValidator)` | `EmailField` | FALLBACK if native email input is needed |
| Data grid / table | `ListingBundle<T>` | `Grid<T>` | Prefer Holon |
| Entity form (create/edit) | `EntityPanelForm<T>` | `FormLayout` + `Binder<T>` | Prefer Holon |
| Action button | `Components.button()` | `Button` | Prefer Holon |
| Upload | — | `Upload` | FALLBACK — no Holon equivalent |
| Confirm dialog | — | `ConfirmDialog` | FALLBACK — no Holon equivalent |
| Notification (success) | — | `Notification` | FALLBACK — no Holon Notification API |
| Navigation shell | — | `AppLayout` + `SideNav` | FALLBACK — no Holon equivalent |
| Responsive form container | — | `FormLayout` + `setResponsiveSteps()` | FALLBACK — no Holon equivalent |
| Tabs | — | `Tabs` + `Tab` | FALLBACK — no Holon equivalent |
| Avatar / icon | — | `Avatar` / `Icon` | FALLBACK — no Holon equivalent |

---

## Component entries

### 1 · String / text input

**Holon:** `Components.input.string()`  
**Vaadin fallback:** `TextField`

#### I18N

```java
// key: <domain>.<field>, fallback shown inline for readability
Input<String> nameInput = Components.input.string()
    .label(Localizable.of("Customer Name", "<domain>.customerName"))  // Holon i18n key
    .placeholder(Localizable.of("Enter full name", "<domain>.customerName.placeholder"))
    .helperText(Localizable.of("Max 200 characters", "<domain>.customerName.helper"))
    .required(Localizable.of("Customer name is required", "validation.required.customerName"))
    .maxLength(200)
    .build();
```

> Key format: `<domain>.<fieldName>` (e.g. `order.customerName`).
> Every `Localizable.of(fallback, key)` pair is resolved at runtime via the Holon
> `LocalizationContext`; the fallback string is shown when no translation is loaded.

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | Always call `.label(...)` — never use a placeholder as the only label |
| Required indicator | `.required(...)` — Holon sets `aria-required="true"` automatically |
| Helper / hint text | `.helperText(...)` — linked to the field via `aria-describedby` by Vaadin |
| Error messages | `.withValidator(...)` — Vaadin surfaces validation messages as `role="alert"` |
| Min / max length hint | Include in helper text; set `.maxLength(n)` for browser-level enforcement |

---

### 2 · Password input

**Holon:** `Components.input.password()`  
**Vaadin fallback:** `PasswordField`

#### I18N

```java
Input<String> pwdInput = Components.input.password()
    .label(Localizable.of("Password", "auth.password"))
    .placeholder(Localizable.of("Enter password", "auth.password.placeholder"))
    .required(Localizable.of("Password is required", "validation.required.password"))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | Always call `.label(...)` |
| Toggle visibility button | Vaadin `PasswordField` renders a reveal button with `aria-label="Show password"` automatically; the Holon wrapper preserves it |
| Autocomplete hint | Set `autocomplete` attribute: `pwdInput.getComponent().getElement().setAttribute("autocomplete", "current-password")` |

---

### 3 · Integer input

**Holon:** `Components.input.integer()`  
**Vaadin fallback:** `IntegerField`

#### I18N

```java
Input<Integer> qtyInput = Components.input.integer()
    .label(Localizable.of("Quantity", "order.quantity"))
    .helperText(Localizable.of("Minimum 1", "order.quantity.helper"))
    .required(Localizable.of("Quantity is required", "validation.required.quantity"))
    .min(1)
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` |
| Min/max boundaries | `.min(n)` / `.max(n)` — Vaadin sets `aria-valuemin` / `aria-valuemax` on the underlying `input[type=number]` |
| Units | Include unit in label or helper text (e.g. "Quantity (units)") |

---

### 4 · Decimal / BigDecimal input

**Holon:** `Components.input.bigDecimal()`  
**Vaadin fallback:** `BigDecimalField` / `NumberField`

#### I18N

```java
Input<BigDecimal> amountInput = Components.input.bigDecimal()
    .label(Localizable.of("Total Amount (USD)", "invoice.totalAmount"))
    .helperText(Localizable.of("Enter amount in dollars", "invoice.totalAmount.helper"))
    .required(Localizable.of("Amount is required", "validation.required.totalAmount"))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label with currency/unit | Include currency or unit in the label text |
| Locale-aware formatting | Holon uses the active `LocalizationContext` locale; ensure `LocalizationContext` is configured for the user's locale |

---

### 5 · Date input

**Holon:** `Components.input.localDate()`  
**Vaadin fallback:** `DatePicker`

#### I18N

```java
Input<LocalDate> dateInput = Components.input.localDate()
    .label(Localizable.of("Invoice Date", "invoice.invoiceDate"))
    .helperText(Localizable.of("Format: MM/DD/YYYY", "invoice.invoiceDate.helper"))
    .required(Localizable.of("Invoice date is required", "validation.required.invoiceDate"))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` |
| Date format hint | `.helperText(...)` with locale-specific format hint |
| Keyboard navigation | Vaadin's calendar popup is keyboard-navigable; ensure focus returns to the field after selection |
| Locale | Pass locale to the underlying `DatePicker` via `dateInput.getComponent().setLocale(locale)` if needed beyond the Holon `LocalizationContext` locale |

---

### 6 · Date-time input

**Holon:** `Components.input.localDateTime()`  
**Vaadin fallback:** `DateTimePicker`

#### I18N

```java
Input<LocalDateTime> dtInput = Components.input.localDateTime()
    .label(Localizable.of("Scheduled At", "task.scheduledAt"))
    .required(Localizable.of("Scheduled date/time is required", "validation.required.scheduledAt"))
    .build();
```

#### A11Y

Same rules as **Date input** above; additionally set `aria-label` on the time sub-field if the
two pickers are rendered separately:

```java
// FALLBACK: Holon wrapper exposes the underlying component for ARIA tuning
dtInput.getComponent().getElement()
    .setAttribute("aria-label", "Scheduled date and time");
```

---

### 7 · Boolean / checkbox

**Holon:** `Components.input.boolean_()`  
**Vaadin fallback:** `Checkbox`

#### I18N

```java
Input<Boolean> activeInput = Components.input.boolean_()
    .label(Localizable.of("Active", "customer.active"))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` renders as a `<label>` next to the checkbox — never use a placeholder only |
| Group of checkboxes | Wrap in a `<fieldset>` with a `<legend>` if multiple related booleans are shown together (raw Vaadin `CheckboxGroup` as FALLBACK) |

---

### 8 · Single-select combo

**Holon:** `Components.input.singleSelect(Class<T>)`  
**Vaadin fallback:** `ComboBox<T>` / `Select<T>`

#### I18N

```java
// Enum / string items with Holon i18n captions
Input<String> statusInput = Components.input.singleSelect(String.class)
    .label(Localizable.of("Status", "order.status"))
    .items("PENDING", "APPROVED", "REJECTED")
    .itemCaptionGenerator(s -> Localizable.of(s, "order.status." + s.toLowerCase()))
    .required(Localizable.of("Status is required", "validation.required.status"))
    .build();

// Or with entity items from a Datastore (lazy)
Input<Customer> customerInput = Components.input.singleSelect(Customer.class)
    .label(Localizable.of("Customer", "order.customer"))
    .dataSource(ds, Customer.PROPERTIES.getDataPath(), Customer.PROPERTIES)
    .itemCaptionGenerator(c -> Localizable.of(c.getName(), null))
    .required(Localizable.of("Customer is required", "validation.required.customer"))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` |
| Meaningful captions | `.itemCaptionGenerator(...)` with translated captions — never show raw enum constants to users |
| Placeholder | `.placeholder(Localizable.of("Select…", "common.select.placeholder"))` |
| Keyboard | Vaadin `ComboBox` supports type-ahead and arrow-key navigation; Holon wrapper preserves this |

---

### 9 · Multi-select

**Holon:** `Components.input.multiSelect(Class<T>)`  
**Vaadin fallback:** `MultiSelectComboBox<T>`

#### I18N

```java
Input<Set<String>> rolesInput = Components.input.multiSelect(String.class)
    .label(Localizable.of("Roles", "user.roles"))
    .items("ADMIN", "REVIEWER", "VIEWER")
    .itemCaptionGenerator(r -> Localizable.of(r, "role." + r.toLowerCase()))
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` |
| Selection count announcement | Vaadin's `MultiSelectComboBox` announces the selected count; Holon wrapper preserves this |
| Clear-all button | Vaadin renders a clear button with `aria-label="Clear"` by default; do not suppress it |

---

### 10 · Text area (multi-line)

**Holon:** `Components.input.string().multiLine()`  
**Vaadin fallback:** `TextArea`

#### I18N

```java
Input<String> notesInput = Components.input.string()
    .multiLine()
    .label(Localizable.of("Notes", "order.notes"))
    .helperText(Localizable.of("Optional. Max 1000 characters.", "order.notes.helper"))
    .maxLength(1000)
    .build();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Visible label | `.label(...)` |
| Character counter | `.maxLength(n)` enables Vaadin's built-in character counter (announces remaining characters to screen readers) |
| Resize | Do not set `resize: none` in CSS unless the layout requires it; resizing improves usability for low-vision users |

---

### 11 · Data grid — `ListingBundle<T>` ⭐ preferred

**Holon:** `ListingBundle<T>` (wraps `PropertyListing<T>`)  
**Vaadin fallback:** `Grid<T>` (with `// FALLBACK: ListingBundle cannot express <thing>`)

#### I18N

When the bean carries `@Caption` annotations (as required by the bean-model convention),
`ListingBundle` resolves column headers **automatically** from the `BeanPropertySet` — no
`.columnHeader(...)` calls are needed.

```java
// Bean fields are annotated with @Caption — see bean-model.md
// @Caption(message = "Customer", messageCode = "order.customerName") on customerName
// @Caption(message = "Total (USD)", messageCode = "order.totalAmount") on totalAmount
// @Caption(message = "Status", messageCode = "order.status") on status

ListingBundle<Order> bundle = ListingBundle
    .builder(Order.PROPERTIES)
    .dataSource(ds, Order.PROPERTIES.getDataPath())
    .visibleProperties("id", "customerName", "totalAmount", "status", "createdAt")
    .sortable("createdAt", true)
    .filterable(true)
    .selectionMode(SelectionMode.SINGLE)
    .build();
// Column headers for customerName, totalAmount, status are resolved from
// @Caption messageCode via the active LocalizationContext automatically.
```

Override a column header only when the view-specific label must differ from the bean default:

```java
// Explicit override — use only when @Caption value is not suitable for this view
.columnHeader("totalAmount", Localizable.of("Invoice Amount (USD)", "view.orderList.totalAmount"))
```

Register message keys in `messages.properties`:
```properties
order.customerName  = Customer
order.totalAmount   = Total (USD)
order.status        = Status
order.createdAt     = Created
order.list.empty    = No orders found
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Grid `aria-label` | `bundle.getListing().getComponent().getElement().setAttribute("aria-label", LocalizationContext.require().getMessage("order.list.ariaLabel", "Orders list"))` |
| Column headers | Resolved from `@Caption` automatically; explicit `.columnHeader(...)` only for view-specific overrides — empty headers fail WCAG SC 1.3.1 |
| Row selection announcement | Use `SelectionMode.SINGLE` or `MULTI`; Vaadin Grid announces selection state automatically |
| Keyboard navigation | Vaadin Grid supports full keyboard navigation; do not suppress focus styles in CSS |
| Empty state | `bundle.getListing().setEmptyStateText(Localizable.of("No orders found", "order.list.empty"))` |
| Sort buttons | Vaadin Grid column sort buttons include `aria-sort` automatically when `.sortable(col, true)` is set |

---

### 12 · Entity form — `EntityPanelForm<T>` ⭐ preferred

**Holon:** `EntityPanelForm<T>` (wraps `PropertyForm<T>`)  
**Vaadin fallback:** `FormLayout` + `Binder<T>` (with `// FALLBACK: EntityPanelForm cannot express <thing>`)

#### I18N

When bean fields carry `@Caption` annotations (as required by the bean-model convention),
`EntityPanelForm` resolves field labels **automatically** from the `BeanPropertySet` — no
`.propertyCaption(...)` calls are needed.

```java
// Bean fields are annotated — see bean-model.md:
// @NotNull @Caption(message = "Customer Name", messageCode = "order.customerName")
// @NotNull @Caption(message = "Total Amount (USD)", messageCode = "order.totalAmount")
// @NotNull @Caption(message = "Status", messageCode = "order.status")
//          @Caption(message = "Notes", messageCode = "order.notes")

EntityPanelForm<Order> form = EntityPanelForm
    .builder(Order.PROPERTIES)
    .visibleProperties("customerName", "totalAmount", "status", "notes")
    .build();
// Field labels are resolved from @Caption messageCode via LocalizationContext automatically.
// @NotNull fields have aria-required="true" set automatically.
```

Override a field label only when the view-specific caption must differ from the bean default:

```java
// Explicit override — use only when @Caption value is not suitable for this view
.propertyCaption("totalAmount",
    Localizable.of("Invoice Amount (USD)", "view.orderForm.totalAmount"))
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| All fields labelled | Labels are resolved from `@Caption` on bean fields automatically — no `.propertyCaption(...)` needed unless overriding; empty labels fail WCAG SC 1.3.1 |
| Required fields | `@NotNull` on bean field → `EntityPanelForm` sets `aria-required="true"` automatically |
| Error summary | When `form.getBean()` throws `ValidationException`, show all messages in a `Notification` with `NotificationVariant.LUMO_ERROR` |
| Form landmark | Wrap in a `<section>` or `<article>` with an `aria-labelledby` pointing to the form heading |
| Focus management | After a successful save, move focus to a confirmation message or the listing; do not leave focus on a now-hidden field |

---

### 13 · Button

**Holon:** `Components.button()`  
**Vaadin fallback:** `Button` (Holon wraps Vaadin `Button`; always use `Components.button()`)

#### I18N

```java
// Primary action
Button saveButton = Components.button()
    .text(Localizable.of("Save", "action.save"))
    .themeVariants(ButtonVariant.LUMO_PRIMARY)
    .onClick(e -> save())
    .build();

// Destructive action
Button deleteButton = Components.button()
    .text(Localizable.of("Delete", "action.delete"))
    .themeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY)
    .onClick(e -> confirmDelete())
    .build();

// Icon-only button (requires accessible name)
Button refreshButton = Components.button()
    .icon(VaadinIcon.REFRESH.create())
    .title(Localizable.of("Refresh list", "action.refresh"))   // tooltip = accessible name fallback
    .onClick(e -> listing.refresh())
    .build();
// For icon-only: also set aria-label explicitly
refreshButton.getElement().setAttribute("aria-label",
    LocalizationContext.require().getMessage("action.refresh", "Refresh list"));
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Descriptive text or `aria-label` | Every button must have either visible text or an explicit `aria-label`; never rely solely on an icon without a label |
| `ButtonVariant` required | Always apply a variant (`LUMO_PRIMARY`, `LUMO_ERROR`, `LUMO_TERTIARY`, `LUMO_SUCCESS`) so intent is clear at all zoom levels and in high-contrast mode |
| Disabled state | Use `.setEnabled(false)` instead of hiding a button that is temporarily unavailable; add a tooltip explaining why it is disabled |
| Loading / async | During async operations, call `.setEnabled(false)` and restore after completion to prevent double-submit |

---

### 14 · Notification

**Vaadin fallback** (no Holon Notification API):  
`// FALLBACK: no Holon equivalent for Notification`

#### I18N

```java
// Success
String msg = LocalizationContext.require()
    .getMessage("order.saved", "Order saved successfully.");
Notification.show(msg, 3000, Notification.Position.BOTTOM_END);

// Error
String errMsg = LocalizationContext.require()
    .getMessage("error.save.order", "Could not save order. Please try again.");
Notification error = new Notification(errMsg, 5000, Notification.Position.MIDDLE);
error.addThemeVariants(NotificationVariant.LUMO_ERROR);
error.open();
```

> Never embed literal English strings without a `LocalizationContext.require().getMessage(key, fallback)` call.

#### A11Y

| Rule | Implementation |
|------|----------------|
| `role="alert"` for errors | `NotificationVariant.LUMO_ERROR` triggers Vaadin to add `role="alert"` — always use it for errors |
| Duration for success | Set a non-zero `duration` (e.g. 3000 ms) so the notification dismisses automatically; do not use 0 for success messages |
| Position | Use `BOTTOM_END` for non-critical messages; `MIDDLE` for errors that require user acknowledgement |
| Screen reader text | Keep notification text concise (≤ 100 chars) and action-oriented |

---

### 15 · Confirm dialog

**Vaadin fallback** (no Holon equivalent):  
`// FALLBACK: no Holon equivalent for ConfirmDialog`

#### I18N

```java
// FALLBACK: no Holon equivalent for ConfirmDialog
ConfirmDialog dialog = new ConfirmDialog();
dialog.setHeader(LocalizationContext.require()
    .getMessage("dialog.confirm.delete.header", "Confirm Delete"));
dialog.setText(LocalizationContext.require()
    .getMessage("dialog.confirm.delete.text",
        "Are you sure you want to delete this record? This action cannot be undone."));
dialog.setConfirmText(LocalizationContext.require()
    .getMessage("action.delete", "Delete"));
dialog.setConfirmButtonTheme("error primary");
dialog.setCancelText(LocalizationContext.require()
    .getMessage("action.cancel", "Cancel"));
dialog.addConfirmListener(e -> performDelete());
dialog.open();
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Dialog role | Vaadin `ConfirmDialog` renders as `role="dialog"` with `aria-modal="true"` automatically |
| Focus trap | Focus is automatically moved to the first focusable element inside the dialog on open |
| Focus restore | On close (confirm or cancel), focus must return to the element that triggered the dialog: `triggerButton.focus()` in both listeners |
| Descriptive heading | Always call `setHeader(...)` — dialog heading is the accessible name (`aria-labelledby`) |

---

### 16 · Upload

**Vaadin fallback** (no Holon equivalent):  
`// FALLBACK: no Holon equivalent for Upload`

#### I18N

```java
// FALLBACK: no Holon equivalent for Upload
Upload upload = new Upload();
UploadI18N i18n = new UploadI18N();
i18n.setDropFiles(new UploadI18N.DropFiles()
    .setOne(LocalizationContext.require().getMessage("upload.dropFiles.one", "Drop file here"))
    .setMany(LocalizationContext.require().getMessage("upload.dropFiles.many", "Drop files here")));
i18n.setAddFiles(new UploadI18N.AddFiles()
    .setOne(LocalizationContext.require().getMessage("upload.addFile", "Upload File…"))
    .setMany(LocalizationContext.require().getMessage("upload.addFiles", "Upload Files…")));
i18n.setError(new UploadI18N.Error()
    .setFileIsTooBig(LocalizationContext.require().getMessage("upload.error.tooBig", "File is too big"))
    .setIncorrectFileType(LocalizationContext.require().getMessage("upload.error.wrongType", "Incorrect file type")));
upload.setI18n(i18n);
upload.setAcceptedFileTypes("application/pdf", ".pdf");
upload.setMaxFileSize(10 * 1024 * 1024); // 10 MB
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| Drop zone label | Set `UploadI18N` drop-files text so screen readers can announce the drop target |
| Accepted file types | State accepted types in helper text next to the upload component |
| Progress announcements | Vaadin `Upload` announces progress via ARIA live regions automatically |
| Error messages | Surface `Upload` error events via the LUMO_ERROR `Notification` pattern above |

---

### 17 · Navigation shell (`AppLayout` + `SideNav`)

**Vaadin fallback** (no Holon equivalent):  
`// FALLBACK: no Holon equivalent for AppLayout, DrawerToggle, SideNav`

#### I18N

```java
// FALLBACK: no Holon equivalent for AppLayout application shell
H1 title = new H1(LocalizationContext.require()
    .getMessage("app.title", "My Application"));

SideNav nav = new SideNav();
nav.addItem(new SideNavItem(
    LocalizationContext.require().getMessage("nav.orders", "Orders"),
    OrderListView.class,
    VaadinIcon.PACKAGE.create()));
nav.addItem(new SideNavItem(
    LocalizationContext.require().getMessage("nav.customers", "Customers"),
    CustomerListView.class,
    VaadinIcon.USERS.create()));
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| `<nav>` landmark | Vaadin `SideNav` renders inside a `<nav>` element automatically; do not wrap it in an extra `<nav>` |
| `aria-label` on `<nav>` | `nav.getElement().setAttribute("aria-label", LocalizationContext.require().getMessage("nav.ariaLabel", "Main navigation"))` |
| Skip-to-content link | Add a visually-hidden `<a href="#main-content">Skip to main content</a>` as the first focusable element in `MainLayout`; set `id="main-content"` on the content area |
| Drawer toggle label | Vaadin `DrawerToggle` renders with `aria-label="Toggle menu"` by default; override if the application is not in English: `toggle.setAriaLabel(LocalizationContext.require().getMessage("nav.toggle", "Toggle menu"))` |
| Active route indication | Vaadin `SideNav` marks the active item with `aria-current="page"` automatically |

---

### 18 · Responsive form toolbar (`FormLayout`)

**Vaadin fallback** (no Holon equivalent):  
`// FALLBACK: no Holon equivalent for FormLayout responsive steps`

#### I18N

Labels are applied on the individual `Components.input.*` fields — see the input entries above.
`FormLayout` itself has no text to localize.

#### A11Y

| Rule | Implementation |
|------|----------------|
| Logical reading order | Add fields to `FormLayout` in the order a screen reader should announce them (left-to-right, top-to-bottom when displayed in multiple columns) |
| Column span hints | Use `toolbar.setColspan(component, 2)` to keep related fields on the same row — avoid arbitrary column spans that break logical grouping |
| `<fieldset>` + `<legend>` for groups | When a `FormLayout` groups related fields (e.g. "Shipping Address"), wrap it in a `Details` component or use a Vaadin `Details` with a localized summary |

---

### 19 · Tabs

**Vaadin fallback** (no Holon equivalent):  
`// FALLBACK: no Holon equivalent for Tabs`

#### I18N

```java
// FALLBACK: no Holon equivalent for Tabs
Tabs tabs = new Tabs();
Tab detailsTab = new Tab(LocalizationContext.require()
    .getMessage("tab.details", "Details"));
Tab historyTab = new Tab(LocalizationContext.require()
    .getMessage("tab.history", "History"));
tabs.add(detailsTab, historyTab);
```

#### A11Y

| Rule | Implementation |
|------|----------------|
| `role="tablist"` | Vaadin `Tabs` renders as `role="tablist"` with individual `role="tab"` children automatically |
| `aria-selected` | Vaadin sets `aria-selected="true"` on the active tab automatically |
| `aria-controls` | Manually link each `Tab` to its panel: `detailsTab.getElement().setAttribute("aria-controls", "details-panel")` and `detailsPanel.setId("details-panel")` |
| Keyboard | Left/right arrow keys navigate between tabs (Vaadin default); do not intercept these key events |

---

## I18N conventions (global rules)

All user-visible strings in the Holon + Vaadin stack MUST use Holon i18n. Never use Vaadin
`I18NProvider` or `UI.getCurrent().getTranslation(...)`.

### Primary source of truth: `@Caption` on the bean

The preferred and required approach is to **embed I18N directly on the bean field** using
`@Caption` from `com.holonplatform.core.i18n.Caption`. `BeanPropertySet` reads all
`@Caption` annotations at startup, and every Holon component built from that
`BeanPropertySet` automatically uses them.

```java
import com.holonplatform.core.i18n.Caption;
import com.holonplatform.core.beans.NotNull;

@NotNull
@Caption(message = "Customer Name", messageCode = "order.customerName")
private String customerName;

@NotNull
@Caption(message = "Total Amount (USD)", messageCode = "order.totalAmount")
private BigDecimal totalAmount;
```

With `@Caption` on every field:
- `ListingBundle` uses `messageCode` for column headers — no `.columnHeader(...)` needed.
- `EntityPanelForm` uses `messageCode` for field labels — no `.propertyCaption(...)` needed.
- `@NotNull` causes `EntityPanelForm` to mark the field as required (`aria-required="true"`).

Override with `.columnHeader(...)` / `.propertyCaption(...)` **only** when a view-specific
label must differ from the bean-level default.

### Key naming convention

```
<domain>.<field>                      # field label          order.customerName
<domain>.<field>.placeholder          # placeholder          order.customerName.placeholder
<domain>.<field>.helper               # helper text          order.customerName.helper
validation.required.<field>           # required message     validation.required.customerName
validation.<rule>.<field>             # other validation     validation.maxLength.customerName
action.<verb>                         # button / action      action.save  action.delete
dialog.<type>.<key>                   # dialog text          dialog.confirm.delete.header
nav.<key>                             # navigation           nav.orders  nav.toggle
app.<key>                             # app-level            app.title
error.<key>                           # error messages       error.save.order
<domain>.list.empty                   # empty grid state     order.list.empty
```

### Inline convention for generated snippets

When emitting code, always pair the Holon i18n key with a fallback string for readability:

```java
// Preferred form for generated code
.label(Localizable.of("Customer Name", "order.customerName"))
// reads as: fallback="Customer Name", key="order.customerName"
```

### `LocalizationContext` for non-component strings (notifications, dialogs)

```java
// Retrieve a translated string outside a component builder
String msg = LocalizationContext.require()
    .getMessage("order.saved", "Order saved.");

// With parameters
String msg = LocalizationContext.require()
    .getMessage("order.savedWithId", new Object[]{ order.getId() },
        "Order #" + order.getId() + " saved.");
```

---

## A11Y conventions (global rules)

Every screen produced by a construction skill MUST pass the following before code is emitted:

### Mandatory checklist

- [ ] Every bean field that is user-visible carries `@Caption(message, messageCode)` — this is the single source of truth for field labels
- [ ] Every mandatory field also carries `@NotNull` so `EntityPanelForm` marks it `aria-required="true"` automatically
- [ ] Every interactive element (input, button, link, grid) has a descriptive accessible name
      (visible label, `aria-label`, or `aria-labelledby`)
- [ ] Standalone inputs (not part of an `EntityPanelForm`) call `.label(Localizable.of(...))` explicitly
- [ ] Error messages are surfaced as `role="alert"` or via `NotificationVariant.LUMO_ERROR`
- [ ] No information conveyed by colour alone (always add text or icon alongside colour cues)
- [ ] All `ButtonVariant` colours have sufficient contrast in Lumo light and dark themes
- [ ] Focus order follows the visual reading order; no focus traps except inside open dialogs
- [ ] After dialog close, focus returns to the triggering element
- [ ] Navigation landmarks present: `<header>` (navbar), `<nav>` (side nav), `<main>` (content area)
- [ ] Skip-to-content link present in `MainLayout` as the first focusable element
- [ ] Grid columns all have visible, translated headers (auto from `@Caption`; override only when needed)
- [ ] Empty grid state has a readable, translated message via `.setEmptyStateText(Localizable)`
- [ ] Icon-only buttons have `aria-label` set (via Holon `.title()` and explicit `.setAttribute`)
- [ ] No `tabindex` values > 0 (breaks natural tab order)
- [ ] Custom CSS does not suppress `:focus-visible` outline styles

### WCAG 2.1 AA target level

The stack targets **WCAG 2.1 AA**. The Lumo design tokens (colour, spacing, font size) are
pre-tuned to AA contrast ratios. Override them via `styles.css` CSS custom properties only;
never hard-code colours or font sizes in Java.

---

## Cross-reference

| Reference | Purpose |
|-----------|---------|
| [`holon-vaadin-ui.md`](holon-vaadin-ui.md) | Composite component patterns (ListingBundle, EntityPanelForm, responsive layout) |
| [`bean-model.md`](bean-model.md) | JavaBean + BeanPropertySet conventions |
| [`datastore-patterns.md`](datastore-patterns.md) | Datastore query / save / delete idioms |
| [`security-patterns.md`](security-patterns.md) | Holon Auth (`@Permitted`, `AuthContext`) |
| [`context-wiring.md`](context-wiring.md) | Holon `Context` wiring |
| [`../../rules/holon-stack.md`](../../rules/holon-stack.md) | Master allow/ban list |
