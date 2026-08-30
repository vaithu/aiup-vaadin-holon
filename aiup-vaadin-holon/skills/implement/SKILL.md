---
name: implement
description: >
  Implements a use case using Holon Platform + Vaadin Flow: creates a plain JavaBean
  domain model, BeanPropertySet, Holon Datastore-backed service, Holon Vaadin Flow view
  with ListingBundle / EntityPanelForm, and Holon Auth security guards. Use when the user
  asks to "implement UC-XXX", "build the UI for", "create a Vaadin view", "implement
  this use case", or mentions Holon implementation, Datastore query, EntityPanelForm,
  ListingBundle, PropertyForm, PropertyListing, or Java web app backed by Holon.
---

# Implement Use Case

## Prerequisites

Before writing any code, verify that all three artifacts exist:

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-XXX-*.md` (the use case being implemented) | `/use-case-spec` |
| `docs/entity_model.md` | `/entity-model` |
| `src/main/resources/db/migration/V*.sql` (at least one Flyway script) | `/flyway-migration` |

If any file is missing, **stop** and tell the user which skill to run first:
> "`docs/use_cases/UC-XXX-*.md` not found — run `/use-case-spec UC-XXX` first." (if use case spec is missing)  
> "`docs/entity_model.md` not found — run `/entity-model` first." (if entity model is missing)  
> "No Flyway migration scripts found — run `/flyway-migration` first." (if no SQL migrations exist)

Do not attempt to infer or recreate the missing artifact.

## Instructions

Implement the use case `$ARGUMENTS` using the Holon Platform + Vaadin Flow stack.

Read the Constraints section and the Pre-Emit Checklist before writing any code.
If the Vaadin or JavaDocs MCP servers are configured, consult them for Holon API signatures;
otherwise rely on the reference documents in this skill's `references/` folder.

Do **not** create test classes — use the dedicated `datastore-test`, `holon-vaadin-test`,
and `playwright-test` skills for that.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.**

### Allowed

- `com.holon-platform.*` — all Holon modules
- Data grids use `Components.listing(T.class)` (→ `ListingBundleBuilder<T>`); entity forms use `EntityFormPanel.bean(T.class)` from `com.holonplatform.vaadin.flow.vaadinplus.components.EntityFormPanel` — bare `PropertyListing` / `PropertyForm` only with `// FALLBACK: ListingBundle/EntityFormPanel cannot express <thing>`
- Buttons use `ButtonBuilder.create()` from `com.holonplatform.vaadin.flow.components.builders.ButtonBuilder` with `.primary()` / `.error()` / `.secondary()` / `.tertiary()` — never raw `ButtonVariant`
- Notifications use `NotificationUtil` (`com.holonplatform.vaadin.flow.components.utils`) or `NotificationBuilder` (`com.holonplatform.vaadin.flow.components.builders`) — never raw Vaadin `Notification` with `NotificationVariant`
- `com.vaadin.*` — **BANNED**. If no Holon Vaadin Flow equivalent exists for a UI need, stop immediately and ask the developer what to use before generating any code.
- `org.springframework.boot:spring-boot-starter` + Holon Spring Boot starters (bootstrap only)
- `org.springframework.stereotype.{Service,Component,Repository}` — permitted when the class needs Spring lifecycle (`@Transactional`, `@EventListener`, `@Scheduled`); Holon `Context` still preferred; inject via constructors
- `org.flywaydb.*`, `org.postgresql.*`, `org.junit.jupiter.*`, `org.testcontainers.*`, `com.microsoft.playwright.*`
- `com.vaadin.flow.signals.*` — Vaadin Signals (`ValueSignal`, `ListSignal`, `SharedValueSignal`, `SharedListSignal`, `SharedNumberSignal`, `Signal`) when reactive state propagation is needed (see §"Vaadin Signals" in `references/holon-vaadin-ui.md` for the decision table)

### Banned — refuse to emit

- `com.holonplatform.core.property.PropertyBox` — **use `Bean` + `BeanPropertySet` exclusively**
- `jakarta.persistence.*` / `javax.persistence.*` — unless Holon JPA Datastore is explicitly required and justified
- `org.springframework.data.jpa.*`, `org.springframework.data.repository.*`
- `org.springframework.web.bind.annotation.*` (no Spring MVC — Vaadin is the UI)
- `org.springframework.beans.factory.annotation.Autowired` — use **constructor injection**
- `com.vaadin.flow.component.*` / `com.vaadin:vaadin-core` — **BANNED** (raw Vaadin core components); stop and ask the developer if no Holon equivalent exists

## Pre-Emit Checklist

- [ ] No `PropertyBox` in emitted code
- [ ] **Feature packages**: bean, model, service, and view all live in `com.example.<app>.<feature>`; no `domain/`, `service/`, or `ui/` layer packages
- [ ] Services use `BeanDatastoreHelper<T>` for all persistence operations; raw `Datastore` / `BeanDatastore` query chains used only when `BeanDatastoreHelper` has no equivalent (with `// FALLBACK:` comment)
- [ ] A `<Entity>Model` interface exists with `BeanPropertySet` and typed `PathProperty` constants — no raw string property names in service code
- [ ] Data grids use `Components.listing(T.class)` (→ `ListingBundleBuilder`); forms use `EntityFormPanel.bean(T.class)` — **not** `EntityPanelForm`, not `EntityFormPanel.builder()`; bare `PropertyListing` / `PropertyForm` only with `// FALLBACK:` justification; **no** `FormLayout` + individual `Input` fields for any form screen
- [ ] **Lazy loading**: `ListingBundle` fetch callbacks call `svc.findSlice(q.getOffset(), q.getLength(), ...)` — **never** `svc.findAll()` inside a fetch callback; service `findSlice` delegates to `BeanDatastoreHelper.findSlice(offset, length, ...)` passing the query offset and length
- [ ] All form validation constraints (`@NotNull`, `@NotBlank`, etc.) are declared on the bean — no manual `.required(...)` / `.withValidator(...)` calls inside `EntityPanelForm`; use `.autoRequiredIndicators(true)` on the form builder
- [ ] No Java enum types used for domain values — every categorical value (status, type, tier, category) is represented as a `Long` FK referencing a lookup table; each lookup combobox uses `.allowCustomValues(true)` with an `onCustomValueSet` listener that calls `svc.findOrCreate(label)` to persist new entries
- [ ] No `@Autowired` — dependencies injected via constructors
- [ ] `@Service` / `@Component` / `@Repository` used only when Spring lifecycle is required (else prefer Holon `Context`)
- [ ] No Spring MVC annotations
- [ ] No Spring Security imports (filter-chain only + justification if needed)
- [ ] No `jakarta.persistence` without Holon JPA Datastore + justification
- [ ] No raw Vaadin core components (`com.vaadin.flow.component.*`) — if no Holon equivalent exists, **stop and ask the developer** before writing any code
- [ ] Vaadin Signals (`com.vaadin.flow.signals.*`) are used **only** when the problem is reactive state propagation — session-local UI flags (`ValueSignal`), cross-session live values / counters / feeds (`SharedValueSignal`, `SharedNumberSignal`, `SharedListSignal`) — **not** for standard CRUD that comes from the Datastore; `SharedXSignal` instances are declared as `@Bean` (application-scoped) and injected via constructor; `ValueSignal` / `ListSignal` are view-field-level only; all effects use `Signal.effect(component, …)` — never `Signal.unboundEffect(…)` in views
- [ ] Services use constructor injection for `Datastore` and other dependencies; no `@Autowired`, no `Context.get()` in application code
- [ ] Views are thin — persistence/business logic lives in a `*Service` class
- [ ] Bean state validated before `Datastore.save(...)`; errors surfaced via `NotificationUtil.notificationError(...)` or `NotificationBuilder.create().error()...build().open()` (both from `com.holonplatform.vaadin.flow`); SLF4J logging
- [ ] Auth guard (`@Authenticate` + `@RolesAllowed` on the class, plus `AuthContext.require().isPermitted(...)` in `beforeEnter`) on every route that needs a role
- [ ] `BeanPropertySet` constant is `public static final`
- [ ] Views use `@Route(value = "...", layout = MainLayout.class)` for the app shell
- [ ] Input toolbars use `FormLayoutBuilder.create().responsiveSteps(...).add(...).build()` — not a bare `HorizontalLayout`
- [ ] Action buttons use semantic variant methods on `ButtonBuilder.create()`: `.primary()`, `.error()`, `.secondary()`, `.tertiary()` — **not** raw `ButtonVariant.LUMO_*` enum
- [ ] Notifications use `NotificationUtil.notificationSuccess/Error(...)` or `NotificationBuilder.create().success()/error()...build().open()` — both from `com.holonplatform.vaadin.flow`; **not** raw Vaadin `Notification` with `NotificationVariant`
- [ ] Visual tokens (colours, fonts, spacing) live in `src/main/resources/META-INF/resources/styles.css` (loaded via `@StyleSheet("styles.css")` on `AppShellConfigurator`), not hard-coded in Java
- [ ] **I18N** — every user-visible string uses `Localizable.of(fallback, key)` or `LocalizationContext.require().getMessage(key, fallback)`; no raw literals, no Vaadin `getTranslation(...)`
- [ ] **`@Caption`** — every user-visible bean field has `@Caption(value = "<fallback>", messageCode = "<domain>.<field>")`; mandatory fields also have `@NotNull`; `ListingBundle`/`EntityPanelForm` resolve labels automatically from these annotations
- [ ] **A11Y** — every input has a visible `.label(...)`, icon-only buttons have `aria-label`, grids have `aria-label` + translated column headers (from `@Caption`) + empty-state text, dialogs restore focus on close, skip-to-content link present in `MainLayout`
- [ ] All `Instant`-typed bean fields map to `TIMESTAMPTZ` columns in the Flyway migration; no `LocalDateTime` used for stored timestamps
- [ ] User timezone is captured at session start in `MainLayout.onAttach` via `ExtendedClientDetails`; a `sessionZone()` helper resolves it from `VaadinSession`; all displayed `Instant` values are converted to `ZonedDateTime` using the session zone; all user-entered date-time values are converted back to `Instant` with `localDateTime.atZone(sessionZone()).toInstant()` before save
- [ ] Displayed timestamps use `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(LocalizationContext.require().getLocale()).withZone(sessionZone())` — no hard-coded pattern strings for date formatting
- [ ] Date-range Datastore filters convert user-supplied `LocalDate` bounds to `Instant` via `localDate.atStartOfDay(sessionZone()).toInstant()` before passing to `TemporalProperty` filter expressions
- [ ] Full compilation verified (`./mvnw compile` or `./gradlew compileJava`)

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Read the entity model from `docs/entity_model.md`
3. Read [`references/bean-model.md`](references/bean-model.md) — JavaBean conventions
4. Read [`references/datastore-patterns.md`](references/datastore-patterns.md) — Datastore query/save/delete
5. Read [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) — UI component patterns
6. Read [`references/component-dictionary.md`](references/component-dictionary.md) — Holon ↔ Vaadin component map with I18N keys and A11Y rules
7. Read [`references/configurator-api.md`](references/configurator-api.md) — fluent builder/configurator methods every Holon builder already inherits (size, style, visibility, label, tooltip, ARIA, icon, click/focus/key listeners, read-only/required, etc.)
8. Read [`references/security-patterns.md`](references/security-patterns.md) — Holon Auth patterns
9. Read [`references/context-wiring.md`](references/context-wiring.md) — Holon Context wiring
10. Read [`references/navigation.md`](references/navigation.md) — `Navigator.get()`, `@QueryParameter`, `@OnShow`, URL sync
11. Read [`references/error-handling.md`](references/error-handling.md) — validation errors, optimistic-lock conflicts, global error views
12. Read [`references/audit-wiring.md`](references/audit-wiring.md) — populating audit fields in the service layer
13. Read [`references/app-configuration.md`](references/app-configuration.md) — `application.yml`, BOM imports, JVM flags
14. Read [`references/architecture.md`](references/architecture.md) — package-by-feature layout, co-location rules, identity vs domain separation, when to add a service layer
15. Check existing code for patterns and conventions
16. **Feature package**: determine the feature name from the use case (e.g. `customer`, `invoice`). All classes produced in steps below go in `com.example.<app>.<feature>` — bean, model, service, and view in the **same** package. Genuinely cross-cutting classes (application shell, auth config, `AuditUtil`, `AuditedBean`) go in `com.example.<app>.shared`. **Never create `domain/`, `service/`, or `ui/` layer packages** — they break co-location and make the feature-package rule unenforceable.
17. **Domain + Model**: create or update JavaBean(s) with `@DataPath` / `@Identifier`, `@Caption(value, messageCode)` on **every** user-visible field, and all Jakarta Bean Validation constraints (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, etc.) on mandatory/constrained fields; add the five **audit & version fields** (`createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate`, `version`) as documented in [`references/bean-model.md`](references/bean-model.md) §"Audit & Version fields" — these are **mandatory** on every domain bean; the bean must implement `AuditedBean` (see [`references/audit-wiring.md`](references/audit-wiring.md)); create a companion `<Entity>Model` interface with `BeanPropertySet` and typed `PathProperty` constants for every bean field (exclude audit fields from `LISTING_SUBSET` and `FORM_SUBSET`). **Infer lookup entities**: if the use case or entity model contains any dropdown / combobox or categorical field (status, type, tier, category, industry, country, department, etc.), **always** create a dedicated lookup JavaBean + Model + Service + Flyway migration for each — there are no Java enum types for domain values; link the parent entity via a `Long` foreign-key `id` field. Every lookup service must expose a `findOrCreate(String label)` method (see `references/datastore-patterns.md`) so users can freely type new values.
18. **Service**: implement service class using `BeanDatastoreHelper<T>` in the same feature package; call `AuditUtil.stampCreate(bean)` before INSERT and `AuditUtil.stampUpdate(bean)` before UPDATE (see [`references/audit-wiring.md`](references/audit-wiring.md)); use `BeanDatastoreHelper` methods for all queries; fall back to raw `BeanDatastore` only when `BeanDatastoreHelper` has no equivalent method (inject `Datastore` via constructor; a `@Service` with constructor injection is allowed when Spring lifecycle is required)
19. **View**: implement Holon Vaadin Flow view in the same feature package (`@Route(layout = MainLayout.class)`, `Components.listing(T.class)` for grids / `EntityFormPanel.bean(T.class)` for forms — **never** assemble `FormLayout` + individual `Input` fields; bind each lookup-entity field as a **creatable combobox**: `Input.singleSelect(Long.class).items(svc.findAll(), ...).allowCustomValues(true).onCustomValueSet(v -> { Long id = svc.findOrCreate(v); ... })` — see `references/component-dictionary.md` §5 and `references/holon-vaadin-ui.md` "Lookup-entity combobox"; all validation comes from bean annotations + `.autoRequiredIndicators(true)`, semantic button variants (`.primary()`, `.error()`, etc.) on all buttons, Holon Auth guards); use `Navigator.get().navigateTo(...)` for all programmatic navigation — see [`references/navigation.md`](references/navigation.md); catch `ValidationException` and `DataAccessException` in save callbacks — see [`references/error-handling.md`](references/error-handling.md); **for reactive UI-local state** (loading flag, form mode toggle, selected-row state shared between panels) use `ValueSignal<T>` + `Signal.effect(component, …)`; **for real-time cross-session state** (live counters, shared values, live feeds) use `SharedNumberSignal` / `SharedValueSignal<T>` / `SharedListSignal<T>` declared as `@Bean` and injected via constructor — see [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) §"Vaadin Signals"
20. **Error views**: ensure `NotFoundView`, `InternalErrorView`, and `AccessDeniedView` exist in `shared` (see [`references/error-handling.md`](references/error-handling.md))
21. **Theme**: load Lumo (or Aura) via `@StyleSheet(Lumo.STYLESHEET)` on the `AppShellConfigurator` class, then add `@StyleSheet("styles.css")` for custom Lumo token overrides; put the overrides in `src/main/resources/META-INF/resources/styles.css` using `html { --lumo-*: ...; }` (Vaadin 25.2 — **do not** use the deprecated `@Theme("<app-name>")` annotation). **Responsiveness — `ResponsiveDiv` for simple cases, CSS for complex cases**: for **simpler** responsive behaviour (mobile/desktop slot swaps, column counts, hide/show regions) prefer the component responsive APIs (`ResponsiveDiv`, `FormLayout.responsiveSteps(...)`, `MasterDetailLayout`, `mobileViewColumn`); for **complex** responsive behaviour (fine-grained breakpoints, spacing, sticky bars, presentation changes that CSS expresses more cleanly) use pure CSS `@media` queries and styles in `styles.css` targeting CSS classes added to the components — see [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) §"Layout builders" and `../implement-from-html/references/css-extraction.md` §"Responsive breakpoints with `@media`"
22. **Timezone**: ensure `MainLayout.onAttach` calls `UI.getCurrent().getPage().retrieveExtendedClientDetails(...)` and stores the resulting `ZoneId` in `VaadinSession` as `"userTimezone"`; add a `sessionZone()` static helper; verify all `Instant` display conversions and all save-path `LocalDateTime → Instant` conversions are present — see `references/holon-vaadin-ui.md` §"Timezone-aware display"
23. Run the Pre-Emit Checklist — fix any violation before proceeding
24. Verify the full implementation compiles successfully

## Resources

- [`references/bean-model.md`](references/bean-model.md) — `@DataPath`, `@Identifier`, `BeanPropertySet`
- [`references/datastore-patterns.md`](references/datastore-patterns.md) — query, save, delete idioms
- [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) — `Components.input.*`, `ListingBundle`, `EntityPanelForm`, responsive layout (`AppLayout`, `FormLayout`), button variants, theme CSS, **Vaadin Signals (reactive state)** — `ValueSignal`, `SharedValueSignal`, `SharedNumberSignal`, `SharedListSignal`, `Signal.effect()`
- [`references/component-dictionary.md`](references/component-dictionary.md) — authoritative Holon component dictionary with correct class names, package paths, real API snippets, I18N conventions, and A11Y rules — **read this before emitting any component**
- [`references/configurator-api.md`](references/configurator-api.md) — the fluent configurator mix-ins (`ComponentConfigurator`, `HasSizeConfigurator`, `HasStyleConfigurator`, `HasLabelConfigurator`, `HasTooltipConfigurator`, `HasAriaLabelConfigurator`, `ClickNotifierConfigurator`, `InputConfigurator`, …) that every Holon builder already inherits — use these fluent methods instead of raw Vaadin `getElement()` / `setWidth` / `addClassName` calls
- [`references/security-patterns.md`](references/security-patterns.md) — `Realm`, `AuthContext`, `@Authenticate`, `@RolesAllowed`, role/permission model
- [`references/context-wiring.md`](references/context-wiring.md) — `Context.get()`, Holon Spring Boot auto-config, fallback policy
- [`references/navigation.md`](references/navigation.md) — `Navigator.get()`, `@QueryParameter`, `@OnShow`, URL sync, lifecycle
- [`references/error-handling.md`](references/error-handling.md) — `ValidationException`, `DataAccessException`, optimistic-lock, global error views
- [`references/audit-wiring.md`](references/audit-wiring.md) — `AuditUtil`, `AuditedBean`, populating audit fields in the service layer
- [`references/app-configuration.md`](references/app-configuration.md) — `application.yml`, BOM imports, JVM timezone flag, Holon autoconfiguration notes
- [`references/architecture.md`](references/architecture.md) — package-by-feature layout, co-location rules, identity vs domain separation, when to introduce a service layer, ArchUnit enforcement
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for Vaadin 25 component docs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Platform API
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers

## Related skills

- **`/ai-assistant UC-XXX`** — after implementing the use case, layer on an AI-powered chat
  assistant (natural-language querying over this use case's data) using the free
  `vaadin-ai-core-flow` module and a Holon Datastore-backed custom `AIController` /
  `DatabaseProvider`.
