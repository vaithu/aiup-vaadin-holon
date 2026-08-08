# HTML Region → Holon Vaadin Component Mapping

> **Source of truth**: verified against `com.holonplatform.vaadin.flow.demo.ui.views.*` (August 2026).
> Every API below compiles. Do **not** invent methods or use class names that differ from this table.

Use this table to select the correct Holon Vaadin Flow component for each HTML region
identified in a mockup. If no Holon equivalent exists, **stop immediately and ask the developer**.

---

## Layout / shell regions

| HTML pattern | Correct Holon component | Notes |
|-------------|-------------------------|-------|
| Full creation page (multi-step form + sticky bar) | `Components.entityCreationForm()...build()` → `EntityCreationForm` | See `NewCustomerDemoView` |
| Step card inside creation page | `Components.formStepCard().stepNumber(n).totalSteps(N).title("...").content(...).build()` → `FormStepCard` | |
| App shell / main layout | `Components.appShell().navbarBrand("...").nav(nav).build()` → `AppShellLayout` | |
| Side nav | `SideNavBuilder.create().withNavItem("Label", View.class, VaadinIcon.X.create()).add().buildWrapper()` | |
| Responsive viewport slots (mobile/desktop) | `ResponsiveDiv.configure(this).slotOnce(ViewMode.DESKTOP, ()->...).build()` | |
| Breadcrumb | `BreadcrumbItem` / `BreadcrumbPage` (from `com.holonplatform.vaadin.flow.vaadinplus.components`) | |
| Tab bar | ⚠️ No Holon equivalent | **Stop and ask the developer** |
| Card / panel | Plain `Div` with CSS class — no Holon wrapper needed | |
| Footer / sticky action bar | Included in `EntityCreationForm` (`barAction(...)`) or manual `Div` with CSS | |

---

## Data display regions

| HTML pattern | Correct Holon component |
|-------------|------------------------|
| Data table / grid | `Components.listing(T.class).columns(...).fetch((q,text,sort)->...).build()` — layout: `bundle.toolbar()` + `bundle.grid()` + `bundle.footer()` |
| Paginated list | Same `Components.listing(T.class)` with `.pageSizes(...)` and `.defaultPageSize(...)` |
| Read-only detail | `EntityFormPanel.bean(T.class)...readOnly().build()` + `form.setBean(bean)` |
| Master-detail split | `MasterDetailLayout<T>` from `com.iyensoft.vaadin.flow.components` — see `MasterDetailDemoV2` |
| Timeline / activity feed | ⚠️ No Holon equivalent — **stop and ask the developer** |

---

## Form / input regions

> All input builders are accessed via `com.holonplatform.vaadin.flow.components.Input` (static methods).
> **Not** `Components.input.*` — that namespace does **not** exist.

| HTML element | Inferred type | Correct Holon input |
|-------------|--------------|---------------------|
| `<input type="text">` | `String` | `Input.string().label("...").build()` |
| `<textarea>` | `String` | `Input.stringArea().label("...").build()` |
| `<input type="number">` (integer) | `Integer` | `Input.number(Integer.class).label("...").build()` |
| `<input type="number">` (decimal) | `Double` / `BigDecimal` | `Input.number(Double.class).label("...").build()` |
| `<input type="date">` | `LocalDate` | `Input.localDate().label("...").build()` |
| `<input type="datetime-local">` | `LocalDateTime` | `Input.localDateTime().label("...").build()` |
| `<input type="email">` | `String` | `Input.string().label("...").build()` (+ email validator) |
| `<input type="checkbox">` | `Boolean` | `Input.boolean_().label("...").build()` |
| Toggle / switch | `Boolean` | `Input.boolean_().label("...").styleName("switch").build()` |
| `<select>` (single) | `String` | `Input.singleSelect(String.class).items(...).label("...").build()` |
| `<select>` (enum) | `MyEnum` | `Input.enumSelect(MyEnum.class).label("...").build()` |
| Radio button group | `String` | `Input.singleOptionSelect(String.class).items(...).label("...").build()` |
| `<select multiple>` / checkbox group | `Set<String>` | `Input.multiOptionSelect(String.class).items(...).label("...").build()` |
| `<input type="file">` | — | ⚠️ **Stop and ask the developer** |
| Complete `<form>` with multiple fields | bean | `EntityFormPanel.bean(MyBean.class).properties(...).autoLabels(true).bind("field", input).noFooter().build()` |

---

## Action / button regions

| HTML pattern | Correct Holon API |
|-------------|------------------|
| Primary action (`class="btn-primary"`, `type="submit"`) | `ButtonBuilder.create().text("...").primary().onClick(...).build()` |
| Destructive / danger button | `ButtonBuilder.create().text("...").error().onClick(...).build()` |
| Secondary / cancel button | `ButtonBuilder.create().text("...").secondary().onClick(...).build()` |
| Ghost / tertiary button | `ButtonBuilder.create().text("...").tertiary().onClick(...).build()` |
| Icon-only button | `ButtonBuilder.create().icon(VaadinIcon.X).icon().ariaLabel("Close").build()` |
| Pre-configured delete | `ButtonBuilder.create().preset(ButtonPreset.DELETE).onClick(...).build()` |
| Shortcut | `Components.button().text("...").primary().build()` — delegates to `ButtonBuilder` |

> ⛔ **Wrong**: `Components.button().text("...").themeVariants(ButtonVariant.LUMO_PRIMARY)` — do not use raw Vaadin `ButtonVariant`.
> Use semantic variant methods: `.primary()`, `.error()`, `.secondary()`, `.tertiary()`.

---

## Feedback / overlay regions

| HTML pattern | Correct Holon API |
|-------------|------------------|
| Success toast | `NotificationUtil.notificationSuccess("...")` or `NotificationBuilder.create().text("...").success().build().open()` |
| Error toast | `NotificationUtil.notificationError("...")` or `NotificationBuilder.create().text("...").error().build().open()` |
| Warning toast | `NotificationBuilder.create().text("...").warning().build().open()` |
| Confirmation dialog | `AlertDialog.builder().title("...").description("...").confirmText("...").variant(Alert.Variant.DESTRUCTIVE).onConfirm(()->...).open()` |
| Inline alert / banner | `Alert.builder(Alert.Variant.WARNING).title("...").description("...").build()` |
| Loading spinner | ⚠️ No Holon equivalent — **stop and ask the developer** |

> `NotificationUtil` is `com.holonplatform.vaadin.flow.components.utils.NotificationUtil` — it IS in Holon.
> `AlertDialog` is `com.holonplatform.vaadin.flow.vaadinplus.components.AlertDialog`.

---

## Mapping decision rules

1. **Use exact API** — `EntityFormPanel.bean(T.class)` (NOT `EntityPanelForm`, NOT `EntityFormPanel.builder()`);
   `Components.listing(T.class)` (NOT `ListingBundle.builder(PROPERTIES)`);
   `Input.string()` (NOT `Components.input.string()`).
2. **Feature packages** — all emitted classes go in `com.example.<app>.<feature>`. No `domain/`, `service/`, or `ui/` layer packages.
3. **No Holon equivalent → stop and ask** — do not emit raw Vaadin or unofficial helper classes without explicit developer approval.
4. **Verify with MCP** — use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) or JavaDocs MCP when uncertain.
5. **Unclear mapping** — state the ambiguity and ask the user to confirm before proceeding.
