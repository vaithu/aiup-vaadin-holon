# Holon Vaadin UI Patterns Reference
# `holon-vaadin-flow` 10.0.3-SNAPSHOT

All UI is built exclusively through **`Components.*`** factory methods and
**`EntityFormPanel`** / **`ListingBundle`** / **`MasterDetailLayout`** composites.

**No raw `com.vaadin.flow.component.*` imports** — if a Holon component does not
exist for what you need, stop and ask the developer before writing anything.

---

## Quick component map

| Need | Use |
|---|---|
| Application shell / nav | `AppShellLayout.builder()` |
| Programmatic navigation | `Navigator.get().navigateTo(...)` |
| Data grid | `Components.listing(T.class)` (bean) or `Components.listing(PropertySet)` (PropertySet) |
| Entity form with save/cancel | `EntityFormPanel.bean(T.class)...build()` |
| Master-list + detail panel | `Components.masterDetail(T.class)...build()` |
| Standalone input | `Components.input.*` |
| Notification | `NotificationUtil.notificationSuccess/Error/Warning(msg)` |
| Alert inline | `Components.alert()` |
| Alert dialog | `Components.alertDialog()` |
| Kanban board | `KanbanBoard.builder()...build()` |
| Filter form | `FilterInputForm.formLayout()/verticalLayout()` |
| Button | `Components.button()` |
| Layouts | `Components.vl()`, `Components.hl()`, `Components.formLayout()`, `Components.row()`, `Components.column()` |
| Labels | `Components.span()`, `Components.h1()`–`h6()`, `Components.label()` |
| Tabs / Accordion | `Components.tabSheet()`, `Components.accordion()` |
| Side navigation | `Components.sideNav()` |
| Card / Panel | `Components.panel()` |
| Breadcrumb | `Components.breadcrumb()` |
| Sheet (slide-over) | `Components.sheet()` |
| Stepper / Timeline | `Components.stepper()`, `Components.timelineStepper()` |
| Avatar / AvatarGroup | `Components.avatar()`, `Components.avatarGroup()` |
| OTP input | `Components.inputOTP()` |
| Empty state | `Components.empty()` |
| Separator | `Components.separator()` |
| Icon badge | `Components.iconBadge()` |

---

## Application Shell (`AppShellLayout`)

Use `AppShellLayout.builder()` for the application shell — **never** extend raw `AppLayout`
directly or construct `DrawerToggle`/`SideNav` manually.

```java
package com.example.ap.shared;

import com.holonplatform.vaadin.flow.vaadinplus.components.AppShellLayout;
import com.vaadin.flow.router.RouterLayout;

public class MainLayout extends AppShellLayout implements RouterLayout {

    public MainLayout() {
        AppShellLayout.builder()
            .navbarBrand("MyApp", DashboardView.class)   // brand text + home route
            .themeToggle()                                // light/dark toggle button
            .nav(buildDrawerNav())                        // drawer content
            .configure(this);                             // apply to this instance
    }

    private static com.vaadin.flow.component.html.Div buildDrawerNav() {
        // use Components.sideNav() for the navigation items
        var nav = Components.sideNav()
            .withItem("Dashboard", DashboardView.class)
            .withItem("Bills", BillListView.class)
            .withItem("Customers", CustomerListView.class)
            .build();
        var div = new com.vaadin.flow.component.html.Div(nav);
        return div;
    }
}
```

`AppShellLayoutBuilder` key methods:

| Method | Description |
|---|---|
| `navbarBrand(title)` | Title in navbar |
| `navbarBrand(title, routeClass)` | Title as home link |
| `navbarBrandLogo(component)` | Logo component |
| `search(placeholder)` | Global search field |
| `search(placeholder, consumer)` | Search with handler |
| `notifications(labels...)` | Notification bell |
| `themeToggle()` | Light/dark toggle |
| `user(consumer)` | User avatar/menu |
| `languages(locales...)` | Language switcher |
| `nav(div)` | Drawer nav content |
| `nav(div, sideNav)` | Drawer nav with SideNav |
| `drawerToggle(bool)` | Show/hide drawer toggle |
| `customizeStart/End(consumer)` | Inject custom AppBar components |
| `configure(appLayout)` | Apply to existing layout |
| `build()` | Build new `AppShellLayout` |

---

## Route / View skeleton

```java
package com.example.ap.bill;

import com.holonplatform.auth.annotations.Authenticate;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Authenticate
@RolesAllowed("bills:view")
@Route(value = "bills", layout = MainLayout.class)
public class BillListView extends Layout {

    public BillListView(BillService svc) {
        setSizeFull();
        setPadding(false);
        // ... build content
    }
}
```

For access control use `@Authenticate` + `@RolesAllowed` (Holon Auth). If per-action checks are needed use
`BeforeEnterObserver`:

```java
@Override
public void beforeEnter(BeforeEnterEvent event) {
    if (!AuthContext.require().isPermitted("bills:view")) {
        event.rerouteTo(AccessDeniedView.class);
    }
}
```

---

## Navigation (`Navigator`)

```java
import com.holonplatform.vaadin.flow.navigator.Navigator;

// Navigate to a view class
Navigator.get().navigateTo(BillListView.class);

// Navigate back / to previous
Navigator.get().navigateBack();
Navigator.get().navigateToPrevious();

// Navigate with URL parameters
Navigator.get().navigateTo(BillDetailView.class, Map.of("id", 42L));

// Build a URL (e.g. for links)
String url = Navigator.get().getUrl(BillDetailView.class);

// Navigate to default (home)
Navigator.get().navigateToDefault();
```

Use `@QueryParameter` on a view field to bind URL query parameters:

```java
@Route("bills")
public class BillListView extends Layout {

    @QueryParameter("status")
    private String filterStatus;   // bound from ?status=PENDING_REVIEW

    @OnShow
    void onShow() {
        // called after navigation parameters are injected
        refresh();
    }
}
```

---

## `ListingBundle` (data grid)

`ListingBundle<T>` is the standard data grid component. It wraps an `ItemListing<T>` with
a toolbar (search, filter panel, menu actions, pagination) and an optional empty/no-results
state. Build it via `Components.listing(...)`.

### Bean-based listing

> **Rule:** Every `ListingBundle` **must** call `.emptyState()` (or `.emptyState(customEmpty)`)
> so that a meaningful UI is shown when the application is opened for the first time and there
> is no data yet. Omitting the call leaves the grid blank with no feedback to the user.

```java
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.ListingBundle;

// Components.listing(Class<T>) → ListingBundleBuilder<T> → ListingBundle<T>
// Use q.getOffset() + q.getLength() for lazy/virtual-scroll loading via BeanDatastoreHelper.findSlice
ListingBundle<Bill> bundle = Components.<Bill>listing(Bill.class)
    .fetch(q -> svc.findSlice(q.getOffset(), q.getLength()))  // lazy — never call .toList() here
    .emptyState()                                              // MANDATORY: shown on first open when no data
    .build();

add(bundle);
```

### Filtered fetch

```java
ListingBundle<Bill> bundle = Components.<Bill>listing(Bill.class)
    .fetch(q -> {
        // q.getOffset() and q.getLength() carry the virtual-scroll window;
        // pass them to BeanDatastoreHelper.findSlice for lazy server-side loading.
        // q.getQueryFilter() is Optional<QueryFilter>; q.getQuerySort() is List<QuerySort>.
        return svc.findSlice(q.getOffset(), q.getLength(), q.getQueryFilter().orElse(null));
    })
    .search("Search bills...")       // enables search box; filter is passed to fetch callback
    .withFilterPanel()               // enables advanced filter panel
    .paginated()                     // paginated mode (default: virtual scroll)
    .defaultPageSize(20)
    .pageSizes(10, 20, 50)
    .emptyState()                    // MANDATORY: shown on first open when no data
    .noResultsState()                // shown when search/filter returns no matches
    .build();
```

### Columns, actions, empty state

```java
ListingBundle<Bill> bundle = Components.<Bill>listing(Bill.class)
    .fetch(q -> svc.findAll())
    .columns("vendorName", "invoiceDate", "totalAmount", "status")  // visible + order
    .hidden("id")                              // hide specific columns
    .gridHeader("Bills", actionButton)         // toolbar title + context components
    .withMenuAction("Export CSV", this::exportCsv)
    .withMenuAction(VaadinIcon.DOWNLOAD, "Export Excel", this::exportXlsx)
    .importAction(this::handleImport)
    .withEditAction(bill -> openEditForm(bill))
    .withDeleteAction(bill -> svc.delete(bill))
    .withRowAction(VaadinIcon.EYE, "View", bill -> openDetailSheet(bill))
    .multiSelect()                             // enable multi-select
    .emptyState()                              // show default empty state when no data
    .noResultsState()                          // show default no-results state after filtering
    .build();
```

### Custom empty state with `EmptyBuilder`

Use `Empty.builder()` to build a branded empty state with an icon, title, description, and an
optional call-to-action button. Pass the result to `.emptyState(Empty)`:

```java
import com.holonplatform.vaadin.flow.vaadinplus.components.Empty;
import com.vaadin.flow.component.icon.VaadinIcon;

Empty myEmptyState = Empty.builder()
    .icon(VaadinIcon.INBOX.create())
    .title("No bills yet")
    .description("Once bills are created they will appear here.")
    .build();

// With an action button (e.g. navigate to the create form)
Empty myEmptyStateWithAction = Empty.builder()
    .icon(VaadinIcon.PLUS_CIRCLE.create())
    .title("No bills yet")
    .description("Create your first bill to get started.")
    .action(Components.button().text("Create bill").onClick(e -> navigator.navigateTo(NewBillView.class)).build())
    .build();

ListingBundle<Bill> bundle = Components.<Bill>listing(Bill.class)
    .fetch(q -> svc.findAll())
    .emptyState(myEmptyState)          // custom empty state — shown on first open with no data
    .noResultsState()                  // default no-results state for search/filter
    .build();
```

### Mobile-responsive column

Prefer Holon's **`LitRendererBuilder`** (`com.holonplatform.vaadin.flow.components.builders`)
over a hand-written `LitRenderer` template string: it is type-safe, auto-binds properties, and
its semantic sub-builders (`gridCell()`, `documentRow()`, `mobileListItem()`, `mobileGridColumn()`)
own the CSS class contract internally, so no raw CSS class strings leak into view code. Style rows
through the `CellStyle` API (`CellStyle.text().title()`, `CellStyle.text().amount()`,
`CellStyle.span().caption()`, `CellStyle.pill().success()`, …).

`gridCell()` requires its bundled stylesheet — add `@StyleSheet("context://grid-cell.css")`
(from `com.vaadin.flow.component.dependency.StyleSheet`) on the consuming view; the CSS ships
inside the Holon Vaadin core jar.

```java
@StyleSheet("context://grid-cell.css") // on the view class
// ...
ListingBundle<Bill> bundle = Components.<Bill>listing(Bill.class)
    .fetch(q -> svc.findAll())
    .mobileViewColumn(LitRendererBuilder.<Bill>gridCell()
        .mediaAvatar(b -> b.getVendorName() != null ? b.getVendorName() : "")
        .addRow(row -> row
            .startText(Bill::getVendorName, CellStyle.text().title())
            .endText(b -> b.getTotalAmount().toString(), CellStyle.text().amount()))
        .build())
    .mobileViewHeader("Bills")
    .build();
```

> **No Holon `LitRendererBuilder`?** If the Holon version on the classpath predates it, fall back to
> a plain `com.vaadin.flow.data.renderer.LitRenderer.<Bill>of("…").withProperty(…)` — `mobileViewColumn`
> accepts any Vaadin `LitRenderer`.

### Access the underlying grid

```java
bundle.listing();          // ItemListing<Bill>
bundle.grid();             // com.vaadin.flow.component.grid.Grid<Bill>
bundle.toolbar();          // toolbar Div
bundle.footer();           // footer Div
bundle.header();           // GridHeader — call to build the full header component
```

### PropertySet-based listing (non-bean)

```java
// Components.listing(PropertySet) → PropertyListingBundleBuilder → ListingBundle<PropertyBox>
ListingBundle<PropertyBox> bundle = Components.listing(BillModel.LISTING)
    .search("Search...")
    .withFilterPanel()
    .gridHeader("Bills")
    .fetch(q -> {  // returns Stream<PropertyBox> — lazy; never use .list() (eager) here
        return datastoreHelper.getDatastore()
            .query(BillModel.TARGET)
            .stream(BillModel.LISTING);
    })
    .build();
```

---

## `EntityFormPanel` (entity form with save/cancel)

`EntityFormPanel<T>` wraps a `BeanPropertyInputForm` in a panel with Save, Clear, and
optionally Cancel / Save-and-New buttons. It is the standard form component for
create/edit screens.

### FormLayout layout (default)

```java
import com.holonplatform.vaadin.flow.vaadinplus.components.EntityFormPanel;

EntityFormPanel<Bill> form = EntityFormPanel.bean(Bill.class)
    .saveButton(btn -> btn.text("Save bill"), bill -> {
        svc.save(bill);
        NotificationUtil.notificationSuccess("Bill saved");
        Navigator.get().navigateTo(BillListView.class);
    })
    .cancelButton(btn -> btn.text("Cancel"),
        () -> Navigator.get().navigateBack())
    .build();

// Populate for edit:
form.setBean(existingBill);

// Read back after save callback fires:
Bill updated = form.getBean();
```

### Div-based layout (grid-div, no FormLayout)

```java
EntityFormPanel<Bill> form = EntityFormPanel.beanDiv(Bill.class)
    .saveButton(btn -> {}, bill -> svc.save(bill))
    .clearButton(btn -> btn.text("Reset"))
    .build();
```

### BeanBuilder key options

```java
EntityFormPanel.bean(Bill.class)
    .layout(EntityFormPanel.LayoutMode.FORM)        // FORM (default) or GRID_DIV
    .responsiveSteps(steps -> steps                 // override responsive breakpoints
        .step("0", 1).step("40em", 2).step("60em", 3))
    .properties("vendorName", "invoiceDate", "status")  // show only these fields
    .required("vendorName", "Vendor name is required")  // add required validator
    .bind("status", Components.input.singleSelect(String.class)
        .items("PENDING_REVIEW", "APPROVED", "REJECTED").build())  // custom input for field
    .bind(BillModel.TOTAL_AMOUNT,
        Components.input.number(BigDecimal.class).build())         // bind by property
    .saveButton(btn -> btn.text("Save"), bill -> save(bill))
    .saveAndNewButton(btn -> btn.text("Save & new"), bill -> saveAndNew(bill))
    .clearButton(btn -> {})
    .cancelButton(btn -> {}, this::goBack)
    .noFooter()                            // hide the button footer (manage buttons yourself)
    .title("New Bill")                     // optional panel title
    .bordered(true)                        // draw card border
    .readOnly()                            // render all fields read-only
    .withBean(initialBean)                 // pre-populate before build()
    .withPostProcessor((layout, property, input) -> {
        // post-process any input after form is built
    })
    .autoRequiredIndicators(true)          // infer required from @NotNull/@NotBlank annotations
    .build();
```

### Interacting after build

```java
form.setBean(bean);          // populate the form
form.getBean();              // read bean (validates first — throws on invalid)
form.getBean(false);         // read bean without validating
form.validate();             // returns true if all inputs are valid
form.setReadOnly(true);      // toggle read-only mode
form.getForm();              // access the underlying PropertyInputForm
form.getSaveButton();        // Button
form.getClearButton();       // Button
```

---

## Master-Detail layout (`MasterDetailLayout`)

Use `Components.masterDetail(MyBean.class)` for the canonical list-on-left / detail-on-right
two-panel pattern with automatic responsive behaviour (desktop split, mobile sheet/navigation).

```java
import com.iyensoft.vaadin.flow.components.MasterDetailLayout;

MasterDetailLayout<Bill> layout = Components.<Bill>masterDetail(Bill.class)
    .master(master -> master
        .listing(svc::findAll)          // supplies the ListingBundle
        .columns("vendorName", "invoiceDate", "status")
        .search("Search bills...")
        .withEditAction(bill -> {})     // optional row edit action
    )
    .detail(detail -> detail
        .content(bill -> buildDetailView(bill))   // returns a Component for selected item
    )
    .withMobileSheet(Sheet.Side.BOTTOM) // mobile: open detail in a bottom sheet
    .withUrlSync(                        // optional: sync selected item id to URL
        bill -> String.valueOf(bill.getId()),
        id -> svc.findById(Long.valueOf(id)))
    .build();

add(layout);
```

**Desktop:** listing fills the left column, detail fills the right. Clicking a row syncs the
detail panel automatically.

**Mobile:** listing fills the screen; row click opens detail in a Sheet (slide-over).

### Lookup-entity combobox inside `EntityFormPanel`

When a form field is a combobox whose options come from a lookup entity (e.g. `Industry`,
`Country`, `Department`), bind the field via `.bind(...)` so items are loaded from a service call.
**Never hard-code a `String` list** — that list belongs in the database.

Use `.allowCustomValues(true)` on **every** lookup combobox so the user can type a new value that
does not yet exist in the table. The `onCustomValueSet` listener must call a service method that
inserts the new label into the lookup table (if absent) and returns the generated `id`, then
refresh the input items and set the new value.

```java
// Customer form where industry and country are creatable lookup comboboxes
Input<Long> industryInput = Components.input.singleSelect(Long.class)
    .label(Localizable.of("Industry", "customer.industry"))
    .items(industryService.findAll(), IndustryLookup::getId, IndustryLookup::getName)
    .allowCustomValues(true)
    .onCustomValueSet(newLabel -> {
        Long newId = industryService.findOrCreate(newLabel);
        industryInput.refresh(industryService.findAll(),
                IndustryLookup::getId, IndustryLookup::getName);
        industryInput.setValue(newId);
    })
    .build();

EntityFormPanel.bean(Customer.class)
    .bind("industryId", industryInput)
    .autoRequiredIndicators(true)   // validation from @NotNull on Customer.industryId
    .saveButton(
        btn -> btn.text(Localizable.of("Save", "action.save")),
        customer -> customerService.save(customer))
    .cancelButton(btn -> btn.text(Localizable.of("Cancel", "action.cancel")),
        () -> Navigator.get().navigateBack())
    .build();
```

For multi-select lookup fields use `Components.input.multiSelect(Long.class)` in the same pattern.

> **Rule:** There are no code-owned enums for domain values — every categorical value that appears
> in a combobox, select, or radio group **must** come from a database table via a lookup entity and
> service call. Every such field **must** use `.allowCustomValues(true)` so the user can freely
> enter new values; the listener persists the new entry to the lookup table before selecting it.
> Never use `Input.enumSelect` or `Input.enumOptionSelect` for domain values.

---

> ⚠️ **FORM RULE**: Do **not** use `Components.input.*` to assemble form fields manually inside a create / edit / detail screen. Use `EntityPanelForm` — it renders all bean fields automatically from `@Caption` and validates from bean annotations. `Components.input.*` is for **standalone** inputs: search bars, filter toolbars, login fields, OTP fields, and similar single-purpose controls outside a form context.

All inputs are accessed through `Components.input.*`. Every builder returns a
`ValidatableInput<T>` or one of its subtypes.

```java
import com.holonplatform.vaadin.flow.components.Components;

// Text
Input<String>   name   = Components.input.string().label("Name").build();
Input<String>   notes  = Components.input.stringArea().label("Notes").build();
Input<String>   pwd    = Components.input.password().label("Password").build();

// Numbers
Input<Integer>  qty    = Components.input.number(Integer.class).label("Qty").build();
Input<BigDecimal> amt  = Components.input.number(BigDecimal.class).label("Amount").build();

// Dates/Times
Input<LocalDate>     ld  = Components.input.localDate().label("Date").build();
Input<LocalDateTime> ldt = Components.input.localDateTime().label("Date & Time").build();
Input<LocalTime>     lt  = Components.input.localTime().label("Time").build();

// Boolean
Input<Boolean> active = Components.input.boolean_().label("Active").build();

// String single-select with items
Input<String> country = Components.input.singleSelect(String.class)
    .label("Country")
    .items("DE", "FR", "IT", "ES")
    .build();

// Multi-select checkboxes
Input<Set<String>> tags = Components.input.multiOptionSelect(String.class)
    .label("Tags")
    .items("Design", "Tech", "Finance")
    .build();

// Multi-select list
Input<Set<String>> perms = Components.input.multiListSelect(String.class)
    .label("Permissions")
    .items("read", "write", "admin")
    .build();
```

### Adding validators

```java
Input<String> email = Components.input.string()
    .label("Email")
    .required("Email is required")
    .withValidator(Validator.email())
    .build();
```

### Binding standalone inputs to form group

```java
PropertyInputGroup group = Components.input.propertyGroup(BillModel.LISTING)
    .build();

// Read/write as PropertyBox:
PropertyBox values = group.getValue();
group.setValue(existingBox);
```

---

## Layout builders

> ⚠️ **FORM RULE**: Do **not** build a form by placing `Input` fields inside a `FormLayout`. Use `EntityPanelForm` for all create / edit / detail screens. `Components.formLayout()` is only for non-form layouts (e.g. side-by-side filter panels or multi-column display regions).

> 📐 **Responsiveness — `ResponsiveDiv` for simple cases, CSS for complex cases.** For
> **simpler** responsive behaviour (mobile/desktop slot swaps, column counts, hiding/showing
> regions) prefer the component responsive APIs below (`ResponsiveDiv`,
> `FormLayout.responsiveSteps(...)`, `UIUtils` step maps, `MasterDetailLayout`,
> `mobileViewColumn`). For **complex** responsive behaviour (fine-grained breakpoints, spacing,
> sticky bars, presentation changes CSS expresses more cleanly) use plain CSS `@media` queries
> and styles in `src/main/resources/META-INF/resources/styles.css`, targeting a CSS class you
> add to the component.
> See `implement-from-html/references/css-extraction.md` §"Responsive breakpoints with `@media`".

Prefer Holon layout builders over raw Vaadin layout constructors.

```java
// VerticalLayout
VerticalLayout vl = Components.vl()
    .spacing(true).padding(true).fullWidth()
    .add(component1, component2)
    .build();

// HorizontalLayout
HorizontalLayout hl = Components.hl()
    .spacing(true).alignItems(FlexComponent.Alignment.CENTER)
    .add(comp1, comp2)
    .build();

// FormLayout with responsive steps
FormLayout form = Components.formLayout()
    .responsiveSteps(steps -> steps
        .step("0", 1)
        .step("40em", 2)
        .step("60em", 3))
    .add(nameInput, emailInput)
    .build();

// Holon Layout (vaadinplus — preferred view base class, more flexible than VerticalLayout)
Layout layout = Components.layout(comp1, comp2);

// Row/Column grid
var row = Components.row()
    .add(Components.column().add(nameInput).build(),
         Components.column().add(emailInput).build())
    .build();

// FlexBoxLayout
FlexBoxLayout fbl = Components.flexBoxLayout()
    .flexWrap(FlexLayout.WrapMode.WRAP)
    .add(comp1, comp2)
    .build();

// SplitLayout
var split = Components.splitLayout()
    .orientation(SplitLayout.Orientation.HORIZONTAL)
    .primaryStyle("min-width", "300px")
    .build();
```

### Predefined responsive step maps (from `UIUtils`)

```java
import com.holonplatform.vaadin.flow.components.utils.UIUtils;

// Use predefined responsive step sets in FormLayout builders:
UIUtils.FIXED_COLUMNS_SMALL      // xs:1, sm:2
UIUtils.FIXED_COLUMNS_MEDIUM     // xs:1, sm:2, md:3
UIUtils.FIXED_COLUMNS_LARGE      // xs:1, sm:2, md:3, lg:4
UIUtils.FLEXIBLE_COLUMNS         // fully fluid
```

---

## Buttons

```java
// Fluent builder
Button save = Components.button()
    .text("Save")
    .icon(VaadinIcon.CHECK)
    .primary()                      // LUMO primary theme
    .onClick(e -> handleSave())
    .build();

// Quick factory
Button cancel = Components.button("Cancel", e -> Navigator.get().navigateBack());
Button iconBtn = Components.button("Export", VaadinIcon.DOWNLOAD.create(), e -> export());
```

---

## Labels and text

```java
var title   = Components.h2().text("Invoices").build();
var caption = Components.span().text("12 records").className("muted").build();
var body    = Components.label().html("<b>Note:</b> required fields...").build();
var h4      = Components.title().text("Summary").build();   // h4 title style
```

---

## Tabs and Accordion

```java
// TabSheet
var tabs = Components.tabSheet()
    .tab("Details", detailsComponent)
    .tab("Contacts", contactsComponent)
    .build();

// Lazy tabs (content loaded on first activation)
var lazyTabs = Components.lazyTabs()
    .tab("Details", () -> buildDetails())
    .tab("Contacts", () -> buildContacts())
    .build();

// Accordion
var accordion = Components.accordion()
    .section("Section 1", section1Content)
    .section("Section 2", section2Content)
    .build();
```

---

## Side navigation (`SideNav`)

```java
var nav = Components.sideNav()
    .withItem("Dashboard", VaadinIcon.DASHBOARD, DashboardView.class)
    .withItem("Bills",     VaadinIcon.FILE_TEXT, BillListView.class)
    .withItem("Customers", VaadinIcon.USERS,     CustomerListView.class)
    .build();
```

---

## Cards and Panels

```java
// Panel — titled bordered container
var panel = Components.panel()
    .title("Customer Summary")
    .content(summaryContent)
    .build();

// Panel — titled bordered container
var panel = Components.panel()
    .title("Billing Details")
    .content(billingForm)
    .build();
```

---

## Alerts and Dialogs

### Inline alert (banner)

```java
import com.holonplatform.vaadin.flow.vaadinplus.components.Alert;

var alert = Components.alert(Alert.Variant.WARNING)
    .title("Attention")
    .description("This action cannot be undone.")
    .build();

add(alert);
```

### Alert modal (dialog-style)

```java
Components.alertModal(Alert.Variant.ERROR)
    .title("Validation error")
    .description("Please fix the highlighted fields.")
    .closeButton()
    .open();
```

### Alert dialog (confirm dialog)

```java
Components.alertDialog()
    .title("Delete bill?")
    .description("This will permanently remove the record.")
    .confirmButton(btn -> btn.text("Delete"), () -> {
        svc.delete(bill);
        NotificationUtil.notificationSuccess("Bill deleted.");
    })
    .cancelButton()
    .open();
```

---

## Notifications (`NotificationUtil`)

```java
import com.holonplatform.vaadin.flow.components.utils.NotificationUtil;

NotificationUtil.notificationSuccess("Bill saved successfully.");
NotificationUtil.notificationError("Failed to save bill. Please try again.");
NotificationUtil.notificationWarning("Draft saved — remember to submit.");

// Validation exception (shows all field errors):
NotificationUtil.notificationError(validationException);
```

---

## Sheet (slide-over panel)

```java
import com.holonplatform.vaadin.flow.vaadinplus.components.Sheet;

// Bottom sheet
var sheet = Components.sheet(Sheet.Side.BOTTOM)
    .title("Bill detail")
    .content(detailComponent)
    .build();

// Open / close
sheet.open();
sheet.close();

// Sheet stack (multiple sheets stacked)
var stack = Components.sheetStack(Sheet.Side.RIGHT);
stack.push(sheet1);
stack.push(sheet2);
stack.pop();
```

---

## Stepper / Timeline

```java
// Step progress indicator
var stepper = Components.stepper()
    .step("Organization").step("Contact").step("Financial").step("Review")
    .current(0)
    .build();

// Vertical timeline
var timeline = Components.timelineStepper()
    .step("Created", "2025-01-01", VaadinIcon.CIRCLE, done -> {})
    .step("Approved", "2025-01-03", VaadinIcon.CHECK_CIRCLE, done -> {})
    .build();
```

---

## Avatar and AvatarGroup

```java
var avatar = Components.avatar("AT")         // initials
    .name("Atelier Tremblay")                // tooltip
    .colorIndex(2)
    .build();

var group = Components.avatarGroup()
    .add(Components.avatar("AT").build())
    .add(Components.avatar("BG").build())
    .maxItemsVisible(3)
    .build();
```

---

## Breadcrumb

```java
var crumb = Components.breadcrumb()
    .item("CRM", CustomerListView.class)
    .item("Customers", CustomerListView.class)
    .item("New customer")
    .build();
```

---

## `FilterInputForm` (filter bar)

Use `FilterInputForm` when you need a standalone filter bar separate from the
`ListingBundle`'s built-in filter panel.

```java
FilterInputForm<FormLayout> filters = FilterInputForm.formLayout()
    .property(BillModel.STATUS,
        Components.input.singleSelect(String.class)
            .items("PENDING_REVIEW", "APPROVED", "REJECTED").build())
    .property(BillModel.VENDOR_NAME, Components.input.string().build())
    .build();

// Get the current QueryFilter from user input:
Optional<QueryFilter> activeFilter = filters.getFilter();

// Listen to filter changes:
filters.addFilterChangeListener(e ->
    bundle.listing().setDataProvider(...));
```

---

## Kanban board (`KanbanBoard`)

```java
import com.holonplatform.vaadin.flow.components.KanbanBoard;

var kanban = KanbanBoard.<Bill, String>builder()
    .columns(List.of(
        KanbanColumn.of("PENDING_REVIEW", "Pending review"),
        KanbanColumn.of("APPROVED", "Approved"),
        KanbanColumn.of("REJECTED", "Rejected")
    ))
    .cardRenderer(bill -> {
        var panel = Components.panel().title(bill.getVendorName()).build();
        return panel;
    })
    .itemIdentifierProvider(bill -> String.valueOf(bill.getId()))
    .itemColumnProvider(Bill::getStatus)
    .itemColumnUpdater(Bill::setStatus)
    .moveHandler((bill, newStatus) -> { svc.save(bill); return true; })
    .countProvider((col, filter) -> (long) svc.findAll(col.getId()).count())
    .build();

kanban.setItems(svc.findAll());
add(kanban.getComponent());
```

---

## Auth guards

```java
// Declarative — on every @Route that requires a permission
@Authenticate
@RolesAllowed("bills:view")
@Route(value = "bills", layout = MainLayout.class)
public class BillListView extends Layout { ... }

// Programmatic — for fine-grained per-action control
AuthContext auth = AuthContext.require();
if (!auth.isPermitted("bills:approve")) {
    approveButton.setVisible(false);
}

// Role check
if (auth.isPermitted(Permission.create("ROLE_ADMIN"))) { ... }
```

---

## CSS utility classes

Holon exposes CSS helpers in `com.holonplatform.vaadin.flow.components.css.*`:

```java
import com.holonplatform.vaadin.flow.components.css.*;

component.addClassName(FontSize.SMALL.className());
component.addClassName(FontWeight.BOLD.className());
component.addClassName(TextColor.SECONDARY.className());
component.addClassName(Shadow.S.className());
component.addClassName(BorderRadius.M.className());
component.addClassName(Overflow.AUTO.className());
component.addClassName(Padding.Uniform.S.className());
```

---

## `UIUtils` helpers

```java
import com.holonplatform.vaadin.flow.components.utils.UIUtils;

// Search input with Holon styling
Input<String> search = UIUtils.createSearchField();

// Heading
LabelBuilder<H4> heading = UIUtils.createH4("Customers");

// Change URL without page reload
UIUtils.setPageUrlWithoutReloading(UI.getCurrent(), "/bills/" + id);

// Clear container children
UIUtils.clearContainer(verticalLayout);

// Handle empty results in a container
UIUtils.handleNoRecordsFound(verticalLayout);
UIUtils.handleNoValuesFound(verticalLayout);
```

---

## Internationalisation (I18N)

Holon provides its own I18N stack via `LocalizationContext` (core) and
`LocalizationContextI18NProvider` / `LocalizationProvider` (Vaadin bridge).
**Never use raw Vaadin `I18NProvider` or Spring `MessageSource` directly.**

### 1. `Localizable` — the unit of translatable text

```java
import com.holonplatform.core.i18n.Localizable;

// message code only (falls back to code if no translation found):
Localizable key = Localizable.of("bill.vendorName.label");

// message code + inline default (used when no provider or key missing):
Localizable key = Localizable.of("bill.vendorName.label", "Vendor name");

// builder form (supports argument placeholders):
Localizable msg = Localizable.builder()
    .messageCode("bill.saved.msg")
    .message("Bill {0} saved successfully")
    .messageArguments(bill.getInvoiceNo())
    .build();
```

### 2. Building a `LocalizationContext`

```java
import com.holonplatform.core.i18n.LocalizationContext;
import com.holonplatform.core.i18n.MessageProvider;

// In a @Bean or @SpringBootApplication configuration class:
@Bean
public LocalizationContext localizationContext() {
    return LocalizationContext.builder()
        .withInitialSystemLocale()                          // start with JVM default locale
        .withMessageProvider(
            MessageProvider.fromProperties("i18n/messages") // src/main/resources/i18n/messages_en.properties, etc.
        )
        .build();
}
```

`MessageProvider.fromProperties(baseName)` loads `.properties` files from the classpath
using the standard `basename_<locale>.properties` convention.

### 3. Bridging into Vaadin (`LocalizationContextI18NProvider`)

```java
import com.holonplatform.vaadin.flow.i18n.LocalizationContextI18NProvider;

// Implement Vaadin's I18NProvider via the Holon LocalizationContext:
@Bean
public LocalizationContextI18NProvider i18nProvider(LocalizationContext ctx) {
    // Declare supported locales:
    return LocalizationContextI18NProvider.create(ctx,
        List.of(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN));
}
```

Register it so Vaadin picks it up:

```java
// application.properties
com.vaadin.i18n-provider=com.holonplatform.vaadin.flow.internal.i18n.DefaultLocalizationContextI18NProvider
```

Or register the bean as a `@Service` that implements `I18NProvider` — Vaadin auto-detects it.

### 4. Resolving messages at runtime (`LocalizationProvider`)

```java
import com.holonplatform.vaadin.flow.i18n.LocalizationProvider;

// Resolve against the current session locale:
Optional<String> label = LocalizationProvider.localize(
    Localizable.of("bill.vendorName.label", "Vendor name"));

// Convenience overload with inline default:
String text = LocalizationProvider.localize(
    "bill.vendorName.label", "Vendor name");

// Resolve with explicit locale:
String text = LocalizationProvider.getLocalization(
    Locale.FRENCH, "bill.vendorName.label", "Vendor name");
```

### 5. Localizable labels in component builders

Every Holon component builder that exposes `.label()`, `.placeholder()`, `.helperText()`,
or `.title()` accepts a `Localizable` directly — **always prefer the `Localizable` overload**
so the label is live-translated when the locale changes:

```java
Input<String> name = Components.input.string()
    .label(Localizable.of("customer.name.label", "Name"))
    .placeholder(Localizable.of("customer.name.placeholder", "Enter full name"))
    .helperText(Localizable.of("customer.name.helper", "As on the invoice"))
    .build();

Button save = Components.button()
    .text(Localizable.of("action.save", "Save"))
    .build();
```

### 6. Language switcher in the AppShell

```java
AppShellLayout.builder()
    .languages(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN)  // renders a locale selector in the navbar
    .configure(this);
```

When the user picks a locale the `LocalizationContext` is updated and all `Localizable`-bound
labels re-render automatically.

### 7. `properties` file conventions

Place files under `src/main/resources/i18n/`:

```
i18n/messages.properties          ← default / English fallback
i18n/messages_fr.properties       ← French
i18n/messages_de.properties       ← German
```

Key naming convention: `<feature>.<field>.<role>`, e.g.:

```properties
# messages.properties
bill.vendorName.label=Vendor name
bill.vendorName.placeholder=Enter vendor name
bill.status.label=Status
action.save=Save
action.cancel=Cancel
```

---

## Accessibility (A11Y)

Holon exposes accessibility attributes through its configurator interfaces.
**Never call raw Vaadin `getElement().setAttribute("aria-label", ...)` or
`setAriaLabel()` directly** — use the Holon configurator methods so labels
can also be `Localizable`-aware.

### 1. ARIA labels (`HasAriaLabelConfigurator`)

All interactive component builders (`Input`, `Button`, `Components.listing`, etc.)
implement `HasAriaLabelConfigurator`:

```java
// Plain string ARIA label:
Button deleteBtn = Components.button()
    .icon(VaadinIcon.TRASH)
    .ariaLabel("Delete bill")        // screen reader announces "Delete bill"
    .onClick(e -> confirmDelete())
    .build();

// Localizable ARIA label (preferred):
Button deleteBtn = Components.button()
    .icon(VaadinIcon.TRASH)
    .ariaLabel(Localizable.of("bill.action.delete.aria", "Delete bill"))
    .onClick(e -> confirmDelete())
    .build();

// ariaLabelledBy — reference another element's id:
Button deleteBtn = Components.button()
    .icon(VaadinIcon.TRASH)
    .ariaLabelledBy("delete-heading")
    .build();
```

### 2. Labels, placeholders, and helper text (all accessibility signals)

Every visible label, placeholder, and helper is also an a11y signal. Use `Localizable`
overloads consistently:

```java
Input<String> email = Components.input.string()
    .label(Localizable.of("customer.email.label", "Email address"))
    .placeholder(Localizable.of("customer.email.placeholder", "name@example.com"))
    .helperText(Localizable.of("customer.email.helper",
        "We'll use this for invoice delivery"))
    .required(Localizable.of("customer.email.required", "Email is required"))
    .build();
```

### 3. Icon-only buttons must always have an ARIA label

An icon-only button without a visible label is invisible to screen readers:

```java
// WRONG — screen reader cannot announce this:
Button bad = Components.button().icon(VaadinIcon.TRASH).build();

// CORRECT — always add ariaLabel when there is no text:
Button good = Components.button()
    .icon(VaadinIcon.TRASH)
    .ariaLabel(Localizable.of("action.delete.aria", "Delete"))
    .build();
```

### 4. Listing column headers

Column headers rendered by `ListingBundle` and `ItemListing` are visible text —
define them via `Localizable` to keep them translated:

```java
Components.<Bill>listing(Bill.class)
    .fetch(q -> svc.findAll())
    .columnHeader("vendorName",
        Localizable.of("bill.column.vendorName", "Vendor"))
    .columnHeader("invoiceDate",
        Localizable.of("bill.column.invoiceDate", "Invoice date"))
    .build();
```

### 5. Form sections and group labels

When grouping fields, use a visible heading so AT users understand the structure:

```java
var form = EntityFormPanel.bean(Bill.class)
    .title(Localizable.of("bill.form.title", "Bill details"))  // panel title = section heading
    .saveButton(btn -> btn
        .text(Localizable.of("action.save", "Save"))
        .ariaLabel(Localizable.of("bill.save.aria", "Save bill")), bill -> save(bill))
    .build();
```

### 6. Roles and live regions — when Holon has no equivalent

If you need an ARIA role or live-region attribute that Holon does not expose, **stop and
ask the developer** rather than reaching for raw Vaadin/HTML element APIs.

> **Rule:** If no Holon A11Y configurator covers the requirement, treat it the same as any
> missing Holon component — do not fall back silently; surface it to the developer.

---

## Timezone-aware display

In a SaaS application the server (and database) always operate in **UTC**, but users are
distributed globally. Without explicit timezone handling, every displayed timestamp appears
in UTC (e.g. US server time), which is wrong for a user in India (IST = UTC+5:30).

The correct pattern is: **store as `Instant` (UTC) → capture user `ZoneId` at session
start → convert `Instant → ZonedDateTime` for display → convert `LocalDateTime + ZoneId →
Instant` on save.**

### 1. Capture the browser timezone at session start

Call this once in `onAttach()` of your `MainLayout` (or the top-level `AppLayout`):

```java
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import java.time.ZoneId;

@Override
protected void onAttach(AttachEvent event) {
    super.onAttach(event);
    UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
        String tzId = details.getTimeZoneId(); // e.g. "Asia/Kolkata"
        try {
            VaadinSession.getCurrent().setAttribute("userTimezone", ZoneId.of(tzId));
        } catch (Exception ignored) {
            VaadinSession.getCurrent().setAttribute("userTimezone", ZoneId.of("UTC"));
        }
    });
}
```

> `retrieveExtendedClientDetails` is an asynchronous browser round-trip. Attribute is
> available on the **next** server interaction — it is safe for all normal view navigation
> that happens after the layout renders.

### 2. Helper: resolve the session timezone

```java
import java.time.ZoneId;
import com.vaadin.flow.server.VaadinSession;

public static ZoneId sessionZone() {
    ZoneId z = (ZoneId) VaadinSession.getCurrent().getAttribute("userTimezone");
    return z != null ? z : ZoneId.of("UTC");
}
```

### 3. Display an `Instant` in the user's timezone

```java
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;

Instant invoiceDate = invoice.getInvoiceDate();  // UTC from DB
ZonedDateTime local = invoiceDate.atZone(sessionZone());

String display = local.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm z"));
// India user sees:  "18-Aug-2026 15:30 IST"
// US (UTC) user:    "18-Aug-2026 10:00 UTC"
```

Bind this into a read-only label inside a `ListingBundle` column renderer or an
`EntityFormPanel` field decorator — do **not** store formatted strings in the bean.

### 4. Save a user-entered date back as UTC `Instant`

When `EntityFormPanel` / `Components.input.localDateTime()` returns a `LocalDateTime`
(no timezone embedded), re-attach the session zone before persisting:

```java
LocalDateTime picked = dateTimePicker.getValue(); // "18-Aug-2026 15:30" (no zone info)
Instant utc = picked.atZone(sessionZone()).toInstant();  // → 2026-08-18T10:00:00Z
invoice.setInvoiceDate(utc);
datastoreService.save(invoice);
```

### 5. Database column type

Flyway migrations must use `TIMESTAMPTZ` (Postgres) for every `Instant`-mapped column:

```sql
invoice_date  TIMESTAMPTZ NOT NULL,
created_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
last_modified_date TIMESTAMPTZ,
```

### 6. JVM startup flag

Always pass `-Duser.timezone=UTC` to the JVM (or set `TZ=UTC` in the container
environment) so the database driver does not silently shift timestamps using the server's
local timezone.

### 7. Locale-aware formatting with Holon `LocalizationContext`

Use the Holon `LocalizationContext` to resolve the user's locale, then combine it with
their `ZoneId` in a single `DateTimeFormatter` — this gives locale-correct formatting
(e.g. Indian English formats dates differently from US English) **and** the correct
timezone offset in one step:

```java
import com.holonplatform.core.i18n.LocalizationContext;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

Locale userLocale = LocalizationContext.require().getLocale(); // resolved by Holon I18N
DateTimeFormatter fmt = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM)  // e.g. "18-Aug-2026, 3:30:00 PM"
    .withLocale(userLocale)                   // locale-correct label order & separators
    .withZone(sessionZone());                 // offsets to user's timezone

String display = fmt.format(invoice.getInvoiceDate()); // Instant → formatted string
```

> Do **not** hard-code a pattern string like `"dd-MMM-yyyy HH:mm z"` — use
> `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)` so the format adapts to
> the user's locale automatically.

### 8. Timezone-aware Datastore range queries

When filtering by a user-supplied date (e.g. "show invoices for today"), convert the
user's local date bounds to `Instant` **before** passing them to the Datastore filter —
otherwise "today" is evaluated in UTC and misses the correct wall-clock window for users
in other timezones:

```java
import java.time.LocalDate;
import java.time.Instant;

LocalDate userDate = LocalDate.of(2026, 8, 18);  // user picked this date in the UI

Instant from = userDate.atStartOfDay(sessionZone()).toInstant();      // 00:00 IST → UTC
Instant to   = userDate.plusDays(1).atStartOfDay(sessionZone()).toInstant(); // 00:00 next day IST → UTC

// Holon Datastore filter using TemporalProperty<Instant>:
List<Invoice> results = helper.find(
    BillModel.INVOICE_DATE.goe(from).and(BillModel.INVOICE_DATE.lt(to))
);
```

> `TemporalProperty<Instant>` supports `.goe()`, `.loe()`, `.gt()`, `.lt()` — the same
> expression API as any other `PathProperty`. No `TemporalType` hint needed for `Instant`.

### Summary

| Layer | Rule |
|---|---|
| DB column | `TIMESTAMPTZ` (always UTC) |
| Bean field | `java.time.Instant` |
| JVM | `-Duser.timezone=UTC` |
| Session start | Capture `ZoneId` via `ExtendedClientDetails` in `MainLayout.onAttach` |
| Display | `instant.atZone(sessionZone())` → `DateTimeFormatter.ofLocalizedDateTime(MEDIUM).withLocale(LocalizationContext.require().getLocale()).withZone(sessionZone())` |
| Form input | `localDateTime.atZone(sessionZone()).toInstant()` before save |
| Date-range filter | Convert user's `LocalDate` bounds to `Instant` via `atStartOfDay(sessionZone()).toInstant()` |

---

## Vaadin Signals (Reactive State)

Vaadin Signals (`com.vaadin.flow.signals.*`) ship with Vaadin 25.1+ and have **no Holon
equivalent**. They are the right choice whenever the problem is **reactive state propagation**
rather than standard CRUD.

### Decision table — signals vs. standard patterns

| Situation | Right tool |
|-----------|-----------|
| Button click → save → refresh grid | Regular click listener + `listing.refresh()` |
| Loading spinner during an async call | `ValueSignal<Boolean>` + `Signal.effect()` |
| Selected-row index shared between two panels in the same view | `ValueSignal<Long>` + `Signal.effect()` |
| Live counter of connected users visible to everyone | `SharedNumberSignal` + `Signal.effect()` |
| Real-time value pushed to all sessions (price, flag, message) | `SharedValueSignal<T>` + `Signal.effect()` |
| Live feed / event log visible to all connected clients | `SharedListSignal<T>` + `Signal.effect()` |
| Derived label that auto-updates when source changes | `Signal.computed()` / `Signal.cached()` |
| Standard CRUD listing from the database | Datastore + service + `ListingBundle` |
| Form binding | `EntityFormPanel` + `BeanPropertySet` |

> **Prefer signals over click listeners for reactive UI state.** When a click (or any
> event) changes UI *state* — a loading flag, a component's visibility/enabled state, a
> status message, a selected row shared between panels, or a value seen across sessions —
> the handler should **set a signal** and let `Signal.effect(component, …)` propagate the
> change to every dependent component. Do **not** imperatively mutate several components
> inside the listener body. The click listener still exists (it is how the DOM event is
> received), but it becomes a one-line signal update instead of scattered UI wiring, and
> the reactive binding lives in the effect. A plain click listener with no reactive state
> (e.g. navigate away, call a service then `listing.refresh()`) needs no signal.

### 1. Session-local reactive state (`ValueSignal`)

`ValueSignal<T>` holds UI-local state. It is **non-serializable** and cannot be used inside
a signal transaction — use `SharedValueSignal` when that is needed.

```java
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

// Declare as a field in the view (never as a @Bean):
private final ValueSignal<Boolean> loading = new ValueSignal<>(false);
private final ValueSignal<String>  statusMsg = new ValueSignal<>("");

// Wire effects — auto-enabled on attach, auto-disabled on detach:
Signal.effect(this, () -> progressBar.setVisible(loading.peek()));
Signal.effect(this, () -> statusLabel.setText(statusMsg.peek()));

// Update from event handler (session lock is already held in a Vaadin listener):
saveButton.addClickListener(e -> {
    loading.set(true);
    statusMsg.set(Localizable.of("Saving…", "action.saving").fallbackMessage());
    try {
        service.save(currentBean);
        statusMsg.set(Localizable.of("Saved", "action.saved").fallbackMessage());
    } catch (Exception ex) {
        NotificationUtil.notificationError(Localizable.of("Save failed", "error.save"));
        log.error("Save failed", ex);
    } finally {
        loading.set(false);
    }
});
```

### 2. Cross-session counter (`SharedNumberSignal`)

Declare as an application-scoped `@Bean`. Inject via **constructor** into the view.

```java
// Config class (shared):
@Bean
public SharedNumberSignal activeUsersSignal() {
    return new SharedNumberSignal(0L);
}

// View:
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;

public class DashboardView extends ... {

    private final SharedNumberSignal activeUsers;

    public DashboardView(SharedNumberSignal activeUsers) {
        this.activeUsers = activeUsers;
        Signal.effect(this, () ->
            activeUsersLabel.setText(String.valueOf(activeUsers.peek())));
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        activeUsers.increment();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        activeUsers.decrement();
    }
}
```

### 3. Cross-session real-time value (`SharedValueSignal`)

```java
import com.vaadin.flow.signals.shared.SharedValueSignal;

// Config (@Bean):
@Bean
public SharedValueSignal<String> systemBanner() {
    return new SharedValueSignal<>(String.class, "");
}

// Admin view — update for all sessions:
bannerInput.addValueChangeListener(e ->
    systemBanner.set(e.getValue()));

// All views — reactive display:
Signal.effect(this, () -> {
    String msg = systemBanner.peek();
    bannerBar.setVisible(!msg.isBlank());
    bannerBar.setText(msg);
});
```

### 4. Real-time shared list (`SharedListSignal`)

Use a `SharedListSignal` for a live event feed that all connected users see update in
real-time without polling.

```java
import com.vaadin.flow.signals.shared.SharedListSignal;

@Bean
public SharedListSignal<String> activityFeed() {
    return new SharedListSignal<>(String.class);
}

// Producer (any view or service):
activityFeed.insertLast("User Alice updated Invoice #42");

// Consumer view (live-updates without refresh):
Signal.effect(this, () -> {
    List<String> entries = activityFeed.peek()
        .stream().map(node -> node.peek()).toList();
    feedListing.setItems(entries);
});
```

### 5. Computed / cached derived signal

```java
// Computed — re-evaluates on every read of the derived signal:
Signal<Integer> nameLength = () -> nameSignal.get().length();

// Cached — re-evaluates only when a dependency changes (memoized):
var uppercaseName = Signal.cached(Signal.computed(() -> nameSignal.get().toUpperCase()));
```

### Rules summary

| Rule | Detail |
|------|--------|
| `SharedXSignal` → `@Bean` | Declare as application-scoped singleton; inject via constructor |
| `ValueSignal` / `ListSignal` → view field | Session-local; never a `@Bean` or `static` field |
| Effect binding | Always `Signal.effect(component, …)` — not `Signal.unboundEffect(…)` — so lifecycle ties to attach/detach and session lock is managed automatically |
| Reading in effect | Use `.peek()` to read without creating a dependency; use `.get()` inside `Signal.computed()` or `Signal.cached()` callbacks |
| No transactions with `ValueSignal` | `ValueSignal` cannot participate in signal transactions; use `SharedValueSignal` when optimistic-lock semantics are needed |
| Signals ≠ database | Do not use shared signals as a substitute for persisted data; load records from the Datastore and use signals only for the reactive notification layer |
| Prefer signals over click-listener UI logic | A handler that changes reactive UI state sets a signal; `Signal.effect()` updates the components — never mutate several components imperatively inside the listener |
