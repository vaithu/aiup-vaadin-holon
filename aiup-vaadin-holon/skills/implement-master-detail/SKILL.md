---
name: implement-master-detail
description: >
  Implements a full master-detail CRM-style view using Holon Platform + Vaadin Flow,
  following the Acme CRM HTML mockup patterns (desktop list, desktop detail, desktop
  new-form, mobile list, mobile detail, mobile new-form). Use when the user asks to
  "implement a list/detail view", "build the customer screen", "create a master-detail
  layout", "implement a new-entity form with live preview", or references any of the
  Acme CRM mockup screens. Consult html-to-holon-component-map.md as the primary
  guide for translating every mockup region into the correct Holon component.
---

# Implement Master-Detail View

## Prerequisites

Before writing any code, verify that all three artifacts exist:

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-XXX-*.md` (the use case being implemented) | `/use-case-spec` |
| `docs/entity_model.md` | `/entity-model` |
| `src/main/resources/db/migration/V*.sql` (at least one Flyway script) | `/flyway-migration` |

If any file is missing, **stop** and tell the user which skill to run first:
> "`docs/use_cases/UC-XXX-*.md` not found — run `/use-case-spec UC-XXX` first."  
> "`docs/entity_model.md` not found — run `/entity-model` first."  
> "No Flyway migration scripts found — run `/flyway-migration` first."

Do not attempt to infer or recreate the missing artifact.

## Instructions

Implement the use case `$ARGUMENTS` as a master-detail CRM view using the Holon Platform
+ Vaadin Flow stack, faithfully reproducing the layout and component patterns from the
Acme CRM HTML mockups.

Read the Constraints section and the Pre-Emit Checklist before writing any code.
**Always read [`references/html-to-holon-component-map.md`](references/html-to-holon-component-map.md)
first** — it is the authoritative guide for which Holon component maps to each mockup
region.

Do **not** create test classes — use the dedicated `datastore-test`, `holon-vaadin-test`,
and `playwright-test` skills for that.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.**

### Allowed

- `com.holon-platform.*` — all Holon modules
- Data grids use `Components.listing(T.class)` (→ `ListingBundleBuilder<T>`); `ListingBundle<T>` and `Components.listing` are in core `com.holonplatform.vaadin.flow.components` — **not** `vaadinplus`
- Entity forms use `EntityFormPanel.bean(T.class)` from `com.holonplatform.vaadin.flow.vaadinplus.components.EntityFormPanel` — **not** `EntityPanelForm`, not `EntityFormPanel.builder()`
- Multi-step create forms use `Components.entityCreationForm()` + `Components.formStepCard()`
- Master-detail layout: canonical entry point is `com.iyensoft.vaadin.flow.components.builders.MasterDetailBuilder.create(T.class)` → `MasterDetailLayout<T>` (`com.iyensoft.vaadin.flow.components`). Configure existing instances via `MasterDetailConfigurator.configure(layout, T.class)` (`com.iyensoft.vaadin.flow.components.builders`). `Components.masterDetail(T.class)` may exist as a facade alias (referenced in Javadoc prose) but is not the verified canonical path — prefer `MasterDetailBuilder.create(T.class)`.
- `MasterDetailLayout`, `MasterDetailAccent`, `Panel`, `DetailSyncAware` and **all** master-detail builders (`MasterDetailBuilder`, `MasterDetailConfigurator`, `LazyTabsBuilder`, `AppShellLayoutBuilder`, etc.) are in `com.iyensoft.vaadin.flow.components(.builders)` — **not** `vaadinplus`.
- Presentational components (`HeroStrip`, `ArAgingBar`, `EntityFormPanel`, `EntityCreationForm`, `FormStepCard`, `TimelineStepper`, `StickyActionBar`, `LivePreviewCard`, `ChecklistPanel`, `StatusBadge`, `Tag`, `Chip`, `ChipGroup`, `Sheet`, `SheetStack`, `AppShellLayout`, `AppBar`, `Header`, `Footer`, `Alert`, `Breadcrumb`, `Pagination`, `Carousel`, `IconBadge`, `InputGroup`, `InputOTP`, `LineItemGrid`, `TotalsCard`, `TransferList`, `Highlight`, `Empty`, `GridHeader`, `WizardFrame`, `DynamicFilterPanel`, `BulkItemPickerDialog`, `CollaborationFormSupport`, etc.) are in `com.holonplatform.vaadin.flow.vaadinplus.components`. `KeyValueItem`, `KeyValueList`, `KeyValuePair(s)`, `BeanToMap`, `Layout`, `ResponsiveDiv` are one level up in `com.holonplatform.vaadin.flow.vaadinplus`.
- Hero card uses `Components.heroStrip()` (Javadoc-confirmed facade) → `HeroStrip` (`com.holonplatform.vaadin.flow.vaadinplus.components`)
- AR aging visualisation uses `ArAgingBarBuilder.create()` → `ArAgingBar` (`com.holonplatform.vaadin.flow.vaadinplus.components`)
- Buttons use `ButtonBuilder.create()` with `.primary()` / `.error()` / `.secondary()` / `.tertiary()` — never raw `ButtonVariant.LUMO_*`
- Notifications use `NotificationUtil` or `NotificationBuilder` — never raw Vaadin `Notification`
- `com.vaadin.flow.signals.*` — permitted only for reactive state propagation (see `holon-stack.md`)
- `org.springframework.boot:spring-boot-starter` + Holon Spring Boot starters (bootstrap only)
- `org.springframework.stereotype.{Service,Component,Repository}` — only when Spring lifecycle is required

### Banned — refuse to emit

- `com.holonplatform.core.property.PropertyBox` — use `Bean` + `BeanPropertySet` exclusively
- `jakarta.persistence.*` / `javax.persistence.*` — unless Holon JPA Datastore is explicitly required and justified
- `org.springframework.data.jpa.*`, `org.springframework.data.repository.*`
- `org.springframework.web.bind.annotation.*` — no Spring MVC
- `org.springframework.beans.factory.annotation.Autowired` — use constructor injection
- `com.vaadin.flow.component.*` / `com.vaadin:vaadin-core` — raw Vaadin core; stop and ask the developer if no Holon equivalent exists

## Pre-Emit Checklist

- [ ] No `PropertyBox` in emitted code
- [ ] All classes (bean, model, service, view) in `com.example.<app>.<feature>` — no layer packages
- [ ] Services use `BeanDatastoreHelper<T>`; `findSlice(offset, length, …)` used in every fetch callback — `findAll()` never called inside a fetch lambda
- [ ] `<Entity>Model` interface with `public static final BeanPropertySet` and typed `PathProperty` constants
- [ ] **List view**: `MasterDetailLayout` with `ListingBundle` master + `HeroStrip` + `lazyTabs()` detail
- [ ] **Detail panel**: `HeroStrip` hero, `ArAgingBar` for AR aging, `TimelineStepper` for activity, `ListingBundle` for sub-entity tables, `EntityFormPanel.readOnly()` for KV sections
- [ ] **New-entity form**: `EntityCreationForm` + `FormStepCard` per section, `StickyActionBar`, `LivePreviewCard`, `ChecklistPanel`
- [ ] **Mobile**: mobile sheet is wired on the builder — `MasterDetailBuilder.create(T.class).viewMode(ViewMode.MOBILE).withMobileSheet(Sheet.Side.BOTTOM)` (`withMobileSheet` is a `MasterDetailConfigurator` method, `ViewMode` from `com.iyensoft.vaadin.flow.enums.ViewMode`; `Sheet.Side` from `com.holonplatform.vaadin.flow.vaadinplus.components.Sheet`) — no separate mobile view class
- [ ] All categorical fields (`statusId`, `tierId`, `regionId`, `industryId`, `countryId`, …) are `Long` FKs bound via `Input.singleSelect(Long.class).allowCustomValues(true).onCustomValueSet(…)`
- [ ] `StatusBadge` used for status columns; `Tag` used for tier/category badges — no raw string rendering
- [ ] Every `ListingBundle` calls `.emptyState()` and `.noResultsState()`
- [ ] No `@Autowired` — constructor injection only
- [ ] All user-visible strings use `Localizable.of(fallback, key)`; every bean field has `@Caption(value, messageCode)`
- [ ] Every input has `.label(Localizable.of(…))`; icon-only buttons have `.ariaLabel(Localizable.of(…))`
- [ ] Auth guard: `@Authenticate` + `@RolesAllowed` on every `@Route`; `AuthContext.require().isPermitted(…)` in `beforeEnter` for action-level checks
- [ ] Action buttons use `.primary()` / `.secondary()` / `.tertiary()` / `.error()` — never raw `ButtonVariant`
- [ ] Notifications via `NotificationUtil.notificationSuccess/Error(…)` or `NotificationBuilder`
- [ ] Visual tokens in `src/main/resources/META-INF/resources/styles.css` — not hard-coded in Java
- [ ] All `Instant` fields map to `TIMESTAMPTZ`; timezone captured in `MainLayout.onAttach` via `ExtendedClientDetails`; displayed timestamps formatted via `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(sessionZone())`
- [ ] Full compilation verified (`./mvnw compile` or `./gradlew compileJava`)
- [ ] **File-set complete**: every file listed in "Emitted File Set (deterministic)" has been created — no file omitted
- [ ] **No separate mobile view classes**: confirm zero `*Mobile*` or `*mobile*` class names exist in the emitted set; mobile behaviour is handled by responsive Holon components on the single desktop view class

## Emitted File Set (deterministic)

Every run of this skill for a customer-like feature **must** produce exactly the following files,
co-located in `com.example.<app>.<feature>` (no layer sub-packages). The class names below are
derived from the reference implementation in `demo/crm-minimal/src/main/java/com/example/crm/customer/`.

### Domain + model

| File | Purpose |
|---|---|
| `<Entity>.java` | Plain JavaBean with `@DataPath` / `@Identifier` / `@Caption`; Jakarta Bean Validation constraints |
| `<Entity>Model.java` | `public static final BeanPropertySet<Entity> BEAN = BeanPropertySet.create(Entity.class)` + typed `PathProperty` constants |
| `<Entity>Status.java` | Enum (or `@DataPath` bean) for the status lookup |
| `<Entity>StatusLookupModel.java` | `BeanPropertySet` + `PathProperty` constants for status lookup rows |
| `<Entity>Tier.java` | Enum (or `@DataPath` bean) for the tier lookup |
| `<Entity>TierLookupModel.java` | `BeanPropertySet` + `PathProperty` constants for tier lookup rows |

### Service layer

| File | Purpose |
|---|---|
| `<Entity>Service.java` | `BeanDatastoreHelper<Entity>`-backed; exposes `findSlice`, `findById`, `save`, `delete` |
| `<Entity>StatusLookupService.java` | `BeanDatastoreHelper<EntityStatus>`-backed; exposes `findSlice` for status lookup |
| `<Entity>TierLookupService.java` | `BeanDatastoreHelper<EntityTier>`-backed; exposes `findSlice` for tier lookup |

### View layer (one class per concern — desktop + mobile in the same class)

| File | Mockup(s) mirrored | Key components |
|---|---|---|
| `<Entity>ListView.java` | `customer-list.html` + `customer-list.mobile.html` | `ListingBundle`, `ChipGroup`, `StatusBadge`, `Tag` |
| `<Entity>MasterDetailView.java` | `customer-detail.html` + `customer-detail.mobile.html` | `MasterDetailLayout` (+ `.withMobileSheet`), `HeroStrip`, `lazyTabs`, `ArAgingBar`, `TimelineStepper` |
| `New<Entity>View.java` | `customer-new.html` + `customer-new.mobile.html` | `EntityCreationForm`, `FormStepCard`, `StickyActionBar`, `LivePreviewCard`, `ChecklistPanel` |

### Shared shell (create once per app, reuse across features)

| File | Purpose |
|---|---|
| `shared/<App>AppLayout.java` | `AppShellLayout` with side rail, global search, notifications, user chip |
| `shared/DashboardView.java` | Default landing view wired to `<App>AppLayout` |
| `shared/NotFoundView.java` | 404 error view |
| `shared/InternalErrorView.java` | 500 error view |
| `shared/AccessDeniedView.java` | 403 error view |

### Resources

| File | Purpose |
|---|---|
| `src/main/resources/META-INF/resources/styles.css` | CSS custom properties / design tokens (no hard-coded colours in Java) |
| `src/main/resources/messages.properties` | All `Localizable` message keys |
| `src/main/resources/db/migration/V*.sql` | Flyway DDL for the entity + lookup tables (created by `/flyway-migration`) |

> **Rule**: mobile screens share the **same** Java class as the desktop screen. Never emit a
> `<Entity>MobileListView`, `<Entity>MobileDetailView`, `New<Entity>MobileView`, or any other
> `*Mobile*`-named class. Mobile behaviour is achieved through responsive Holon components
> (`MasterDetailLayout.withMobileSheet`, `ChipGroup`, `HeroStrip` responsive CSS, `StickyActionBar`).

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Read the entity model from `docs/entity_model.md`
3. **Read [`references/html-to-holon-component-map.md`](references/html-to-holon-component-map.md)** — identify the Holon component for every UI region in the mockup before writing a single line of code
4. Read [`../implement/references/bean-model.md`](../implement/references/bean-model.md) — JavaBean conventions
5. Read [`../implement/references/datastore-patterns.md`](../implement/references/datastore-patterns.md) — Datastore query/save/delete
6. Read [`../implement/references/holon-vaadin-ui.md`](../implement/references/holon-vaadin-ui.md) — UI component patterns
7. Read [`../implement/references/component-dictionary.md`](../implement/references/component-dictionary.md) — Holon component dictionary with correct class names, package paths, real API snippets, I18N and A11Y rules
8. Read [`../implement/references/configurator-api.md`](../implement/references/configurator-api.md) — fluent builder/configurator mix-ins
9. Read [`../implement/references/security-patterns.md`](../implement/references/security-patterns.md) — Holon Auth patterns
10. Read [`../implement/references/context-wiring.md`](../implement/references/context-wiring.md) — Holon Context wiring
11. Read [`../implement/references/navigation.md`](../implement/references/navigation.md) — `Navigator.get()`, `@QueryParameter`, `@OnShow`, URL sync
12. Read [`../implement/references/error-handling.md`](../implement/references/error-handling.md) — validation errors, optimistic-lock, global error views
13. Read [`../implement/references/audit-wiring.md`](../implement/references/audit-wiring.md) — `AuditUtil`, audit field stamping
14. Read [`../implement/references/app-configuration.md`](../implement/references/app-configuration.md) — `application.yml`, BOM imports, JVM flags
15. Read [`../implement/references/architecture.md`](../implement/references/architecture.md) — package-by-feature layout, co-location rules
16. Check existing code for patterns and conventions
17. **Feature package**: all classes go in `com.example.<app>.<feature>` — bean, model, service, view co-located
18. **Domain + Model**: JavaBean(s) with `@DataPath` / `@Identifier`, `@Caption(value, messageCode)` on every user-visible field, Jakarta Bean Validation constraints on the bean
19. **Service**: `BeanDatastoreHelper<T>`-backed service; `AuditUtil.stampCreate` / `stampUpdate` before persistence
20. **List view**: `MasterDetailLayout` — master side is a `ListingBundle` with `ChipGroup` filter, `StatusBadge` / `Tag` column renderers; detail side opens via URL sync on desktop and `Sheet` on mobile
21. **Detail panel**: `HeroStrip` at top → `lazyTabs()` → per-tab cards: `EntityFormPanel.readOnly()` for fields, `ArAgingBar` for aging, `ListingBundle` for sub-tables, `TimelineStepper` for activity
22. **New-entity form**: `EntityCreationForm` + one `FormStepCard` per logical section + `StickyActionBar` + right-panel `LivePreviewCard` + `ChecklistPanel`
23. **Error views**: ensure `NotFoundView`, `InternalErrorView`, `AccessDeniedView` exist in `shared`
24. **Theme**: `@StyleSheet(Lumo.STYLESHEET)` + `@StyleSheet("styles.css")` on `AppShellConfigurator`; custom tokens in `styles.css`
25. **Timezone**: `MainLayout.onAttach` → `ExtendedClientDetails` → `ZoneId` stored in `VaadinSession`; `sessionZone()` helper used wherever timestamps are displayed
26. Run the Pre-Emit Checklist — fix every violation before proceeding
27. Verify full compilation

## Resources

- [`references/html-to-holon-component-map.md`](references/html-to-holon-component-map.md) — **start here**: maps every Acme CRM mockup region (shell, list, detail, new-form — desktop + mobile) to the exact Holon component and API call; includes 10 global rules enforced on every view
- [`../implement/references/bean-model.md`](../implement/references/bean-model.md) — `@DataPath`, `@Identifier`, `BeanPropertySet`
- [`../implement/references/datastore-patterns.md`](../implement/references/datastore-patterns.md) — query, save, delete idioms
- [`../implement/references/holon-vaadin-ui.md`](../implement/references/holon-vaadin-ui.md) — `Components.input.*`, `ListingBundle`, `EntityFormPanel`, responsive layout, button variants, theme CSS
- [`../implement/references/component-dictionary.md`](../implement/references/component-dictionary.md) — authoritative Holon component dictionary with correct class names, package paths, real API snippets, I18N conventions, A11Y rules
- [`../implement/references/configurator-api.md`](../implement/references/configurator-api.md) — fluent configurator mix-ins inherited by every Holon builder
- [`../implement/references/security-patterns.md`](../implement/references/security-patterns.md) — `Realm`, `AuthContext`, `@Authenticate`, `@RolesAllowed`
- [`../implement/references/context-wiring.md`](../implement/references/context-wiring.md) — `Context.get()`, Holon Spring Boot auto-config
- [`../implement/references/navigation.md`](../implement/references/navigation.md) — `Navigator.get()`, `@QueryParameter`, `@OnShow`, URL sync
- [`../implement/references/error-handling.md`](../implement/references/error-handling.md) — `ValidationException`, `DataAccessException`, optimistic-lock, global error views
- [`../implement/references/audit-wiring.md`](../implement/references/audit-wiring.md) — `AuditUtil`, `AuditedBean`, audit field stamping
- [`../implement/references/app-configuration.md`](../implement/references/app-configuration.md) — `application.yml`, BOM imports, JVM timezone flag
- [`../implement/references/architecture.md`](../implement/references/architecture.md) — package-by-feature layout, co-location rules, ArchUnit enforcement
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for Vaadin 25 component docs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Platform API
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
