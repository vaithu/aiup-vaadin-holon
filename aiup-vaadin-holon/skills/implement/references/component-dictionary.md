# Holon Vaadin Flow — Component Dictionary (Real API)

> **Source of truth**: derived directly from the Holon Vaadin Flow library source at
> `com.holonplatform.vaadin.flow.vaadinplus.components.*` (verified August 2026).
> Every snippet below compiles against the real library. Do **not** invent methods.

Skills MUST consult this file when choosing a component. If a Holon component exists for
the job, use it. If **no Holon component exists**, **stop immediately and ask the developer**
what component or approach to use — do not silently emit raw Vaadin.

> **Shared fluent methods**: every builder listed here also inherits the cross-cutting
> configurator methods (size, style, visibility, id, label, placeholder, helper text,
> tooltip, ARIA, icon, prefix/suffix, theme variants, focus, click/key listeners,
> read-only, required, value-change mode, autocomplete, clear button, regex pattern).
> See [`configurator-api.md`](configurator-api.md) for the full catalogue — use those
> methods instead of raw Vaadin `getElement()` / `setWidth` / `addClassName` calls.

---

## Package map

| Concern | Class / entry point |
|---------|-------------------|
| Fluent input builders | `com.holonplatform.vaadin.flow.components.Input` |
| Button builder | `com.holonplatform.vaadin.flow.components.builders.ButtonBuilder` |
| Button group | `Components.buttonGroup()` → `ButtonGroup` |
| Form layout builder | `com.holonplatform.vaadin.flow.components.builders.FormLayoutBuilder` |
| Form section (titled) | `FormSection.of("Title", fields...)` |
| Notification builder | `com.holonplatform.vaadin.flow.components.builders.NotificationBuilder` |
| Notification shortcuts | `com.holonplatform.vaadin.flow.components.utils.NotificationUtil` |
| Data grid / listing | `Components.listing(T.class)` → `ListingBundleBuilder<T>` |
| Entity form (bean) | `com.holonplatform.vaadin.flow.vaadinplus.components.EntityFormPanel` |
| Multi-step creation page | `Components.entityCreationForm()` + `Components.formStepCard()` |
| Multi-step wizard panel | `WizardFrame.builder()` → `WizardFrame` |
| Inline progress stepper | `Components.stepper()` → `FlowStepper` |
| Confirmation dialog | `com.holonplatform.vaadin.flow.vaadinplus.components.AlertDialog` |
| Modal alert notification | `Components.alertModal()` → `AlertModal` |
| Inline alert | `com.holonplatform.vaadin.flow.vaadinplus.components.Alert` |
| Empty state | `Components.empty()` → `Empty` |
| Slide-in panel | `Components.sheet()` → `Sheet` |
| Application shell | `Components.appShell()` → `AppShellLayout` |
| App bar (top navigation) | `com.holonplatform.vaadin.flow.vaadinplus.components.AppBar` |
| Page header | `Components.header(title)` → `Header` |
| Page footer | `Components.footer()` → `Footer` |
| Grid / list header | `Components.gridHeader(title)` → `GridHeader` |
| Breadcrumb navigation | `Components.breadcrumb()` → `Breadcrumb` |
| Side nav builder | `com.iyensoft.vaadin.flow.components.builders.SideNavBuilder` |
| Responsive layout | `com.holonplatform.vaadin.flow.vaadinplus.ResponsiveDiv` |
| Master-detail | `Components.masterDetail(T.class)` → `MasterDetailLayout<T>` |
| Separator / divider | `Components.separator()` → `Separator` |
| Status pill badge | `Components.statusBadge(label, variant)` → `StatusBadge` |
| Icon badge (round tinted) | `Components.iconBadge(icon, variant)` → `IconBadge` |
| Tag (label chip) | `new Tag(text)` or `new Tag(VaadinIcon, text)` |
| Filter chip group | `Components.chipGroup()` → `ChipGroup` with `Chip` items |
| Input group (addons) | `Components.inputGroup()` → `InputGroup` + `InputGroupText` |
| OTP input | `Components.inputOTP()` → `InputOTP` |
| HeroStrip card | `Components.heroStrip()` → `HeroStrip` |
| AR aging bar | `ArAgingBarBuilder.create()` → `ArAgingBar` |
| KPI / metric card | `Components.highlight(heading, value)` → `Highlight` |
| Hero-strip KPI card | `Components.heroStrip()` → `HeroStrip` |
| AR aging bar | `ArAgingBarBuilder.create()` → `ArAgingBar` |
| Live preview sidebar card | `Components.livePreviewCard(eyebrow)` → `LivePreviewCard` |
| Progress checklist | `Components.checklistPanel()` → `ChecklistPanel` |
| Sticky action bar | `Components.stickyActionBar()` → `StickyActionBar` |
| Timeline / audit log | `Components.timelineStepper()` → `TimelineStepper` |
| Carousel / slideshow | `Components.carousel()` → `Carousel` |
| Transfer list (shuttle) | `TransferList.builder()` → `TransferList` |
| Bulk item picker dialog | `BulkItemPickerDialog.builder()` → `BulkItemPickerDialog` |
| Inline spreadsheet (PO lines) | `Components.lineItemGrid()` → `LineItemGrid` |
| Dynamic query filter builder | `DynamicFilterPanel.of(MyBean.class)` → `DynamicFilterPanel<T>` |

---

## Quick-reference table

| Job | **Correct Holon API** | No Holon equivalent |
|-----|-----------------------|---------------------|
| Single-line text | `Input.string()` | — |
| Multi-line text area | `Input.stringArea()` | — |
| Integer | `Input.number(Integer.class)` | — |
| Decimal / BigDecimal | `Input.number(Double.class)` or `Input.number(BigDecimal.class)` | — |
| Local date | `Input.localDate()` | — |
| Local date-time | `Input.localDateTime()` | — |
| Local time | `Input.localTime()` | — |
| Boolean checkbox | `Input.boolean_()` | — |
| Boolean toggle/switch | `Input.boolean_().styleName("switch")` | — |
| Filterable combo (single) | `Input.singleSelect(String.class).items(...)` | — |
| Creatable filterable combo | `Input.singleSelect(Long.class).items(...).allowCustomValues(true).onCustomValueSet(v -> ...)` | — |
| Radio buttons | `Input.singleOptionSelect(String.class).items(...)` | — |
| List box (single) | `Input.singleListSelect(String.class).items(...)` | — |
| Checkbox group (multi) | `Input.multiOptionSelect(String.class).items(...)` | — |
| Multi list box | `Input.multiListSelect(String.class).items(...)` | — |
| OTP / PIN input | `Components.inputOTP().group(3).separator().group(3).onComplete(...).build()` | — |
| Input with addons (prefix/suffix) | `Components.inputGroup()` + `InputGroupText` | — |
| Action button | `ButtonBuilder.create().text("...").primary().build()` | — |
| Segmented / toggle button group | `Components.buttonGroup().content(...).build()` | — |
| Form layout | `FormLayoutBuilder.create().responsiveSteps(...).add(...).build()` | — |
| Titled form section | `FormSection.of("Title", field1, field2)` | — |
| Entity form (CRUD) | `EntityFormPanel.bean(MyBean.class)...noFooter().build()` | — |
| Multi-step creation page | `Components.entityCreationForm()` + `Components.formStepCard()` | — |
| Multi-step wizard (embedded) | `WizardFrame.builder().step("Label", panel).onFinish(...).build()` | — |
| Step progress indicator | `Components.stepper().steps(...).currentStep(0).build()` | — |
| Data grid | `Components.listing(T.class).columns(...).fetch(...).build()` | — |
| Dynamic query filter | `DynamicFilterPanel.of(MyBean.class)` | — |
| Inline document lines grid | `Components.lineItemGrid().withItemSuggestion(...).build()` | — |
| Transfer list (shuttle) | `TransferList.builder().availableItems(...).onTransfer(...).build()` | — |
| Bulk item picker dialog | `BulkItemPickerDialog.builder().items(...).onConfirm(...).build().open()` | — |
| Notification (quick) | `NotificationUtil.notificationSuccess("...")` | — |
| Notification (builder) | `NotificationBuilder.create().text("...").success().build().open()` | — |
| Confirmation dialog | `AlertDialog.builder().title("...").onConfirm(()->...).open()` | — |
| Modal alert notification | `Components.alertModal(Alert.Variant.SUCCESS).title("...").build().open()` | — |
| Inline alert | `Alert.builder(Alert.Variant.WARNING).title("...").build()` | — |
| Empty state | `Components.empty().icon(...).title("...").description("...").build()` | — |
| Slide-in panel (Sheet) | `Components.sheet(Sheet.Side.RIGHT).title("...").content(...).build()` | — |
| Stacked sheets | `Components.sheetStack(Sheet.Side.RIGHT)` | — |
| KPI metric card | `Components.highlight(heading, value)` → `Highlight` | — |
| Hero-strip KPI card | `Components.heroStrip()` → `HeroStrip` (header + tags + cells) | — |
| AR aging bar | `ArAgingBarBuilder.create()` → `ArAgingBar` (multi-segment bar card) | — |
| Status pill | `Components.statusBadge("Posted", StatusBadge.Variant.SUCCESS)` | — |
| Round icon badge | `Components.iconBadge(VaadinIcon.CHECK.create(), Alert.Variant.SUCCESS)` | — |
| Tag (label chip) | `new Tag(VaadinIcon.CLOCK, Localizable.of("Draft", "tag.draft"))` | — |
| Filter chip group | `ChipGroup.create().addChip(Chip.of("All", 10), true).onSelect(...).build()` | — |
| Audit log / timeline | `Components.timelineStepper().pageSize(20).onLoadMore(...).build()` | — |
| Carousel / slideshow | `Components.carousel().items(...).loop(true).build()` | — |
| Sidebar progress checklist | `Components.checklistPanel()` + `addItem("Step", "hint", ItemState.PENDING)` | — |
| Live preview sidebar card | `Components.livePreviewCard("Live preview")` → `LivePreviewCard` | — |
| Sticky action bar | `Components.stickyActionBar()` → `StickyActionBar` | — |
| Visual separator | `Components.separator().build()` (horizontal) / `.orientation(VERTICAL)` | — |
| App shell | `Components.appShell().navbarBrand("...").nav(nav).build()` | — |
| Top app bar | `new AppBar()` with `.addToStart()` / `.addToEnd()` / `.addToBottom()` | — |
| Page header | `Components.header("Title")` → `Header` (with breadcrumb, tabs, actions) | — |
| Page footer | `Components.footer()` → `Footer` (brand / navigation / actions / legal) | — |
| Grid header (with selection) | `Components.gridHeader("Title")` → `GridHeader` | — |
| Breadcrumb navigation | `Components.breadcrumb()` → `Breadcrumb` with `BreadcrumbItem` / `BreadcrumbPage` | — |
| Side nav | `SideNavBuilder.create().withNavItem(...).add().buildWrapper()` | — |
| Responsive slots | `ResponsiveDiv.configure(this).slotOnce(ViewMode.DESKTOP, ()->...).build()` | — |
| Master-detail | `Components.masterDetail(T.class)` → `MasterDetailLayout<T>` | — |
| File upload | — | ⚠️ **Stop and ask the developer** |
| Rich-text editor | — | ⚠️ **Stop and ask the developer** |

> ⛔ **Common wrong names**: `Components.input.string()` → wrong; `EntityPanelForm` → wrong;
> `ListingBundle.builder(PROPERTIES)` → wrong; `Components.input.bigDecimal()` → wrong.
> Use only the correct APIs listed above.

---

## Component entries

> ⛔ **Raw strings are forbidden** in any user-visible text. Every `.label()`, `.text()`, `.placeholder()`,
> `.required()`, `.title()`, `.description()`, `.cancelText()`, `.confirmText()`, `.search()`, and `.ariaLabel()`
> call **must** use `Localizable.of("fallback", "message.key")`. All keys must exist in `messages.properties`.

### 1 · Text inputs

```java
// Single-line — com.holonplatform.vaadin.flow.components.Input
Input<String> name = Input.string()
    .label(Localizable.of("Account name",      "crm.customer.name"))
    .placeholder(Localizable.of("Enter name",  "crm.customer.name.placeholder"))
    .helperText(Localizable.of("Max 200 characters", "crm.customer.name.helper"))
    .required(Localizable.of("Name is required", "crm.customer.name.required"))
    .maxLength(200)
    .build();

// Multi-line text area
Input<String> notes = Input.stringArea()
    .label(Localizable.of("Internal notes",            "crm.customer.notes"))
    .placeholder(Localizable.of("Anything your team should know…", "crm.customer.notes.placeholder"))
    .build();
```

**A11Y**: always call `.label(...)` — never rely on placeholder alone; `.required(msg)` sets `aria-required` automatically.

---

### 2 · Number inputs

```java
Input<Integer>    qty    = Input.number(Integer.class).label(Localizable.of("Employees",    "crm.customer.employees")).build();
Input<Double>     price  = Input.number(Double.class).label(Localizable.of("Price",         "crm.product.price")).build();
Input<BigDecimal> amount = Input.number(BigDecimal.class).label(Localizable.of("Credit limit", "crm.customer.creditLimit")).build();
```

---

### 3 · Date / time inputs

```java
Input<LocalDate>     date = Input.localDate().label(Localizable.of("First contact date", "crm.customer.firstContactDate")).build();
Input<LocalDateTime> dt   = Input.localDateTime().label(Localizable.of("Created at",          "crm.common.createdAt")).build();
Input<LocalTime>     time = Input.localTime().label(Localizable.of("Start time",              "crm.common.startTime")).build();
```

---

### 4 · Boolean / toggle

```java
// Standard checkbox
Input<Boolean> active = Input.boolean_().label(Localizable.of("Active", "crm.common.active")).build();

// Toggle switch (CSS class "switch")
Input<Boolean> sync = Input.boolean_().label(Localizable.of("Sync with LinkedIn", "crm.customer.syncLinkedIn")).styleName("switch").build();
```

---

### 5 · Select inputs

```java
// Filterable single-select (ComboBox) — items from a lookup service
Input<Long> statusId = Input.singleSelect(Long.class)
    .items(statusService.findAll(), StatusLookup::getId, StatusLookup::getName)
    .label(Localizable.of("Status", "crm.common.status"))
    .build();

// Creatable filterable combo — user can pick an existing lookup value OR type a new one.
// Use for ALL categorical fields (status, tier, industry, country, department, etc.).
// The custom value listener saves the new string to the lookup table and refreshes the selection.
Input<Long> industryId = Input.singleSelect(Long.class)
    .items(industryService.findAll(), IndustryLookup::getId, IndustryLookup::getName)
    .label(Localizable.of("Industry", "crm.customer.industry"))
    .allowCustomValues(true)
    .onCustomValueSet(newLabel -> {
        Long newId = industryService.findOrCreate(newLabel); // saves if absent, returns id
        industryId.refresh(industryService.findAll(), IndustryLookup::getId, IndustryLookup::getName);
        industryId.setValue(newId);
    })
    .build();

// Radio buttons (single-option select)
Input<String> segment = Input.singleOptionSelect(String.class)
    .items("SMB", "Mid-market", "Enterprise")
    .label(Localizable.of("Segment", "crm.customer.segment"))
    .build();

// Checkbox group (multi-option select)
Input<Set<String>> perms = Input.multiOptionSelect(String.class)
    .items("Read", "Write", "Delete")
    .label(Localizable.of("Permissions", "crm.user.permissions"))
    .build();
```

---

### 6 · Buttons

```java
// com.holonplatform.vaadin.flow.components.builders.ButtonBuilder
var save = ButtonBuilder.create()
    .text(Localizable.of("Save customer", "crm.action.save"))
    .primary()
    .onClick(e -> handleSave())
    .build();

var delete = ButtonBuilder.create().text(Localizable.of("Delete", "crm.action.delete")).error().build();
var cancel = ButtonBuilder.create().text(Localizable.of("Cancel", "crm.action.cancel")).tertiary().build();

// Icon-only — ariaLabel is mandatory (screen readers); it must also be localizable
var searchBtn = ButtonBuilder.create()
    .icon(VaadinIcon.SEARCH)
    .icon()
    .ariaLabel(Localizable.of("Search", "crm.action.search"))
    .build();

// Pre-configured delete preset
var del = ButtonBuilder.create().preset(ButtonPreset.DELETE).onClick(e -> ...).build();
```

Variants: `.primary()` · `.secondary()` · `.tertiary()` · `.error()` · `.success()` · `.contrast()`  
Sizes: `.small()` · `.normal()` · `.large()`  
Shortcut: `Components.button().text("Save").primary().build()` (delegates to `ButtonBuilder`).

---

### 7 · Form layout

> 📐 **Responsiveness — `ResponsiveDiv` for simple cases, CSS for complex cases.** For simpler
> responsive layout prefer the component responsive APIs (`ResponsiveDiv`, `responsiveSteps(...)`);
> use plain CSS `@media` queries in `styles.css` for complex responsive behaviour. See
> `css-extraction.md` §"Responsive breakpoints with `@media`".

```java
// com.holonplatform.vaadin.flow.components.builders.FormLayoutBuilder
var layout = FormLayoutBuilder.create()
    .responsiveSteps(
        new ResponsiveStep("0",     1),   // mobile
        new ResponsiveStep("500px", 2),   // tablet
        new ResponsiveStep("900px", 3))   // desktop
    .add(firstName, lastName, email)
    .add(2, address)                      // spans 2 columns
    .build();
```

---

### 8 · EntityFormPanel (entity form / CRUD)

> **Class name**: `EntityFormPanel` — from `com.holonplatform.vaadin.flow.vaadinplus.components.EntityFormPanel`  
> **Not** `EntityPanelForm`. **Not** `EntityFormPanel.builder(...)`. Entry point: `EntityFormPanel.bean(MyBean.class)`.

```java
EntityFormPanel<Customer> form = EntityFormPanel.bean(Customer.class)
    .properties("name", "industry", "street", "city", "country")
    .autoLabels(true)       // resolves labels from @Caption(value, messageCode) on bean fields
    // bind creatable combobox for lookup-entity fields (user can pick or type a new value):
    .bind("industryId", Input.singleSelect(Long.class)
        .items(industryService.findAll(), IndustryLookup::getId, IndustryLookup::getName)
        .label(Localizable.of("Industry", "crm.customer.industry"))
        .allowCustomValues(true)
        .onCustomValueSet(v -> {
            Long id = industryService.findOrCreate(v);
            // the bind callback refreshes the field after the listener runs
        })
        .build())
    .bind("countryId", Input.singleSelect(Long.class)
        .items(countryService.findAll(), CountryLookup::getId, CountryLookup::getName)
        .label(Localizable.of("Country", "crm.customer.country"))
        .allowCustomValues(true)
        .onCustomValueSet(v -> {
            Long id = countryService.findOrCreate(v);
        })
        .build())
    // responsive columns via lambda:
    .responsiveSteps(steps -> steps.mobile(1).tablet(2).desktop(3))
    // required fields with validation message:
    .required("name",    Localizable.of("Account name is required", "crm.customer.name.required"))
    .required("country", Localizable.of("Country is required",      "crm.customer.country.required"))
    // wizard / multi-step mode — no Save/Cancel footer:
    .noFooter()
    .build();

// Lifecycle
form.setBean(existingCustomer);
boolean valid = form.validate();
Customer saved = form.getBean(true);    // true = validate before returning
```

**A11Y**: `autoLabels(true)` reads `@Caption` annotations — always annotate every user-visible bean field with `@Caption(value = "fallback", messageCode = "domain.field")`.

---

### 9 · Multi-step creation form

Used for "New Customer"-style full-page forms with sticky action bar and step progress.  
See `NewCustomerDemoView` for the complete, compiling reference.

```java
// 1. One EntityFormPanel per step (noFooter)
EntityFormPanel<OrgBean> orgPanel = EntityFormPanel.bean(OrgBean.class)
    .properties("name", "legalName", "type", "industry")
    .autoLabels(true).noFooter().build();

// 2. Wrap each panel in a FormStepCard
FormStepCard step1 = Components.formStepCard()
    .stepNumber(1).totalSteps(4)
    .title(Localizable.of("Organization",                    "crm.step.org.title"))
    .subtitle(Localizable.of("The legal entity you\u2019re adding", "crm.step.org.subtitle"))
    .content(orgPanel)
    .build();

// 3. Assemble the full page scaffold
EntityCreationForm creationForm = Components.entityCreationForm()
    .title(Localizable.of("New customer",  "crm.page.newCustomer.title"))
    .draftBadge(Localizable.of("Unsaved draft", "crm.badge.unsavedDraft"))
    .steps(step1, step2, step3, step4)
    .headerAction(discardBtn, saveBtn)
    .barAction(discardBarBtn, saveBarBtn)
    .build();
```

---

### 10 · Data grid / listing

> Entry point: `Components.listing(T.class)` — returns a `ListingBundleBuilder<T>`.  
> **Not** `ListingBundle.builder(PROPERTIES)`. Result exposes `.toolbar()`, `.grid()`, `.footer()`.

```java
var bundle = Components.listing(Customer.class)
    .columns("id", "name", "industry", "city", "country", "status")
    .pageSizes(10, 25, 50)
    .defaultPageSize(10)
    .fetch((q, text, sort) -> customerService.fetch(q.getOffset(), q.getLimit(), text))
    .search(Localizable.of("Search customers…", "crm.customer.list.searchPlaceholder"))
    .build();

// Three-part layout (all pre-wired)
add(bundle.toolbar(),   // page-size selector
    bundle.grid(),      // data grid
    bundle.footer());   // pagination bar
```

---

### 11 · Notifications

Both APIs are in Holon — use whichever fits:

```java
// Quick shortcuts — com.holonplatform.vaadin.flow.components.utils.NotificationUtil
NotificationUtil.notificationSuccess(Localizable.of("Customer saved!",            "crm.notify.customerSaved"));
NotificationUtil.notificationError(Localizable.of("Please fix validation errors",  "crm.notify.validationError"));

// Full builder — com.holonplatform.vaadin.flow.components.builders.NotificationBuilder
NotificationBuilder.create()
    .text(Localizable.of("Operation completed", "crm.notify.operationCompleted"))
    .success()
    .topCenter()
    .duration(3000)
    .build()
    .open();
// Variants: .success() · .error() · .warning() · .contrast() · .primary()
```

---

### 12 · Confirmation dialog

```java
// com.holonplatform.vaadin.flow.vaadinplus.components.AlertDialog
AlertDialog.builder()
    .title(Localizable.of("Discard changes?",                "crm.dialog.discard.title"))
    .description(Localizable.of("All unsaved changes will be lost.", "crm.dialog.discard.description"))
    .headerIcon(VaadinIcon.TRASH.create(), Alert.Variant.DESTRUCTIVE)
    .cancelText(Localizable.of("Keep editing", "crm.dialog.discard.cancel"))
    .confirmText(Localizable.of("Discard",     "crm.dialog.discard.confirm"))
    .variant(Alert.Variant.DESTRUCTIVE)
    .onConfirm(() -> navigator.navigateTo(CustomerListView.class))
    .build()
    .open();
// Variants: Alert.Variant.DEFAULT · DESTRUCTIVE · WARNING · SUCCESS · INFO
```

Focus is restored automatically on close — satisfies A11Y dialog requirement.

---

### 13 · Inline alert

```java
// com.holonplatform.vaadin.flow.vaadinplus.components.Alert
Alert alert = Alert.builder(Alert.Variant.WARNING)
    .title(Localizable.of("Unsaved changes",                       "crm.alert.unsaved.title"))
    .description(Localizable.of("Changes will be lost if you navigate away.", "crm.alert.unsaved.description"))
    .build();
```

---

### 14 · Application shell

**`AppShellConfigurator` (theme activation — one per app, typically on `@SpringBootApplication`):**

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.StyleSheet;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@StyleSheet(Lumo.STYLESHEET)                                    // Lumo base theme — always first
@StyleSheet("styles.css")                                       // custom overrides in src/main/resources/META-INF/resources/styles.css
@SpringBootApplication
public class CrmApplication implements AppShellConfigurator {
    public static void main(String[] args) { SpringApplication.run(CrmApplication.class, args); }
}
```

**`MainLayout` (no `@StyleSheet` here — stylesheets are declared on `AppShellConfigurator` above):**

```java
public class MainLayout extends AppLayout {
    public MainLayout() {
        var nav = SideNavBuilder.create()                       // com.iyensoft.vaadin.flow.components.builders.SideNavBuilder
            .withNavItem(Localizable.of("Customers", "nav.customers"), CustomerListView.class, VaadinIcon.USER.create()).add()
            .withNavItem(Localizable.of("Settings",  "nav.settings"),  SettingsView.class,     VaadinIcon.COG.create()).add()
            .buildWrapper();

        var shell = Components.appShell()                       // → AppShellLayout (com.holonplatform.vaadin.flow.vaadinplus.components)
            .navbarBrand(Localizable.of("MiniCRM", "app.brand"), "v1.0")
            .nav(nav)
            .build();

        addToDrawer(shell);
    }
}
```

---

## I18N rules

> ⛔ **Raw string literals in user-visible text are a build-review failure.** Every method below MUST receive `Localizable.of("fallback", "message.key")`.

| Context | Mandatory pattern |
|---------|------------------|
| Bean field labels | `@Caption(value = "fallback", messageCode = "domain.field")` on the field; `EntityFormPanel.autoLabels(true)` resolves it |
| Input label | `Input.string().label(Localizable.of("Account name", "crm.customer.name")).build()` |
| Input placeholder | `Input.string().placeholder(Localizable.of("Enter name", "crm.customer.name.placeholder")).build()` |
| Input helper / required | `.helperText(Localizable.of(...))` / `.required(Localizable.of(...))` |
| Button text | `ButtonBuilder.create().text(Localizable.of("Save", "crm.action.save")).build()` |
| Icon-button aria-label | `.ariaLabel(Localizable.of("Search", "crm.action.search"))` |
| Notification text | `NotificationUtil.notificationSuccess(Localizable.of("Saved", "crm.notify.saved"))` |
| Dialog title / description | `.title(Localizable.of(...))` / `.description(Localizable.of(...))` |
| Dialog cancel / confirm | `.cancelText(Localizable.of(...))` / `.confirmText(Localizable.of(...))` |
| Step title / subtitle | `.title(Localizable.of(...))` / `.subtitle(Localizable.of(...))` |
| Page / creation form title | `.title(Localizable.of(...))` on `EntityCreationForm` builder |
| Nav items | `.withNavItem(Localizable.of("Customers", "nav.customers"), ...)` |
| Search placeholder | `.search(Localizable.of("Search…", "crm.customer.list.searchPlaceholder"))` |
| App brand | `.navbarBrand(Localizable.of("MiniCRM", "app.brand"), version)` |
| Tag text | `new Tag(Localizable.of("Draft", "tag.draft"))` — always use Localizable constructor |
| StatusBadge / Chip / Highlight | Pass `Localizable.of(...)` to `setText()` / constructor where available |

All keys must exist in `src/main/resources/messages.properties`.  
**Never** use `UI.getCurrent().getTranslation(...)` or implement `I18NProvider`.

---

## A11Y rules

> ⛔ **A11Y violations are a build-review failure.** Every rule below is mandatory.

| Rule | Implementation |
|------|---------------|
| Every input has a visible label | Always call `.label(Localizable.of(...))` — never use placeholder as the only label |
| Required fields | `.required("field", Localizable.of(...))` on `EntityFormPanel`, or `.required(Localizable.of(...))` on `Input` — Holon sets `aria-required` automatically |
| Icon-only buttons | Always call `.ariaLabel(Localizable.of(...))` — raw string aria labels are also banned |
| Data grid | Pass `ariaLabel` in listing configuration |
| Lookup selects | Every `singleSelect` / `singleOptionSelect` bound to a lookup entity — including creatable combos with `.allowCustomValues(true)` — must call `.label(Localizable.of(...))` — no bare combobox without a label |
| Skip-to-content | Add a skip link as the first child of `MainLayout` |
| Heading hierarchy | Use correct `<h1>`→`<h6>` rank; don't use headings for visual styling |
| Dialogs | `AlertDialog` and `AlertModal` restore focus automatically on close — no manual `focus()` needed |
| Color alone | Never convey state by color alone; pair with an icon or text label |
| `Chip` / `ChipGroup` | Each `Chip` renders as `<button type="button" aria-pressed="...">` — state is communicated correctly; `ChipGroup` carries `role="group"` and a localizable `aria-label` |
| `Separator` (meaningful) | Default — carries `role="separator"` and `aria-orientation`; use `.decorative(true)` only for purely visual dividers |
| `Empty` state | Renders with `role="status"` so screen readers politely announce it when the collection becomes empty |
| `Breadcrumb` | Renders as `<nav aria-label="Breadcrumb">`; current page item carries `aria-current="page"` automatically |
| `Carousel` | Wraps in `role="region"` with a localizable `aria-label`; Prev/Next buttons carry localizable `aria-label` |
| `FlowStepper` / `TimelineStepper` | Pass a localizable `ariaLabel` on the builder; individual step states are managed by the web component |
| `InputOTP` | Rendered as an accessible composite; set `pattern("[0-9]")` for digit-only inputs |
| `ButtonGroup` | Carries `role="group"` with a localizable `ariaLabel` |
| `InputGroup` | Carries `role="group"` with a localizable `ariaLabel` |


---

### 15 · HeroStrip — gradient header card with tags and metric cells

> Class: `com.holonplatform.vaadin.flow.vaadinplus.components.HeroStrip`  
> Builder: `com.holonplatform.vaadin.flow.components.builders.HeroStripBuilder`  
> Entry point: `Components.heroStrip()` or `HeroStripBuilder.create()`

Use `HeroStrip` as the visual hero card on a customer / order / entity detail page —
typically the very first component in the detail panel. It combines a thumbnail, entity
name, tag pills (status, tier) and up to five KPI metric cells.

```java
HeroStrip strip = Components.heroStrip()
    .variant(HeroStrip.Variant.DEFAULT)     // DEFAULT | INFO | SUCCESS | WARNING | DANGER | VIOLET
    .header(h -> h
        .name(customer.getName())            // entity display name
        .meta(customer.getCustomerNumber())  // secondary label below name
        .starred(false))                     // optional star toggle
    .tag(customer.getTier().getLabel(),  HeroStrip.TagVariant.CONTRAST)
    .tag(customer.getCustomerStatus().getLabel(), HeroStrip.TagVariant.INFO)
    .cell(c -> c
        .header("Account health")
        .content(customer.getAccountHealth() != null ? customer.getAccountHealth() : "—"))
    .cell(c -> c
        .header("Open AR")
        .content(customer.getOpenAr() != null ? customer.getOpenAr().toPlainString() : "—")
        .valueVariant(HeroStrip.ValueVariant.DANGER))    // DEFAULT | DANGER | SUCCESS | WARNING
    .cell(c -> c.header("ARR").content("$120k"))
    .build();
```

`TagVariant` options: `DEFAULT` · `INFO` · `SUCCESS` · `WARNING` · `DANGER` · `CONTRAST`  
`ValueVariant` options: `DEFAULT` · `DANGER` · `SUCCESS` · `WARNING`

---

### 16 · ArAgingBar — proportional multi-segment bar card

> Class: `com.holonplatform.vaadin.flow.vaadinplus.components.ArAgingBar`  
> Builder: `com.holonplatform.vaadin.flow.components.builders.ArAgingBarBuilder`  
> Entry point: `ArAgingBarBuilder.create()` or `ArAgingBarBuilder.create(ArAgingBar.Variant)`

Use `ArAgingBar` to visualise AR aging buckets (0-30d / 31-60d / 61-90d / 90d+) or any
proportional multi-segment breakdown (pipeline stages, quote status, etc.).

```java
ArAgingBar bar = ArAgingBarBuilder.create()
    .header(h -> h
        .title("AR Aging")
        .variant(ArAgingBar.Variant.WARNING))
    .content(c -> c
        .segment("0–30d",  "$12,400", 55.0, ArAgingBar.Variant.SUCCESS)
        .segment("31–60d", "$6,200",  28.0, ArAgingBar.Variant.WARNING)
        .segment("61–90d", "$2,800",  13.0, ArAgingBar.Variant.DANGER)
        .segment("90d+",   "$880",     4.0, ArAgingBar.Variant.DANGER))
    .footer(f -> f
        .left("Total: $22,280")
        .center("Avg days: 38")
        .right("3 invoices overdue"))
    .build();
```

`Variant` options: `DEFAULT` · `SUCCESS` · `WARNING` · `DANGER` · `INFO`
