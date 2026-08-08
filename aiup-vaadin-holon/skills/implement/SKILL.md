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

### Banned — refuse to emit

- `com.holonplatform.core.property.PropertyBox` — **use `Bean` + `BeanPropertySet` exclusively**
- `jakarta.persistence.*` / `javax.persistence.*` — unless Holon JPA Datastore is explicitly required and justified
- `org.springframework.data.jpa.*`, `org.springframework.data.repository.*`
- `org.springframework.web.bind.annotation.*` (no Spring MVC — Vaadin is the UI)
- `org.springframework.beans.factory.annotation.Autowired` — use **constructor injection** (preferred: Holon `Context.get()`)
- `com.vaadin.flow.component.*` / `com.vaadin:vaadin-core` — **BANNED** (raw Vaadin core components); stop and ask the developer if no Holon equivalent exists

## Pre-Emit Checklist

- [ ] No `PropertyBox` in emitted code
- [ ] **Feature packages**: bean, model, service, and view all live in `com.example.<app>.<feature>`; no `domain/`, `service/`, or `ui/` layer packages
- [ ] Services use `BeanDatastoreHelper<T>` for all persistence operations; raw `Datastore` / `BeanDatastore` query chains used only when `BeanDatastoreHelper` has no equivalent (with `// FALLBACK:` comment)
- [ ] A `<Entity>Model` interface exists with `BeanPropertySet` and typed `PathProperty` constants — no raw string property names in service code
- [ ] Data grids use `Components.listing(T.class)` (→ `ListingBundleBuilder`); forms use `EntityFormPanel.bean(T.class)` — **not** `EntityPanelForm`, not `EntityFormPanel.builder()`; bare `PropertyListing` / `PropertyForm` only with `// FALLBACK:` justification
- [ ] No `@Autowired` — dependencies injected via constructors
- [ ] `@Service` / `@Component` / `@Repository` used only when Spring lifecycle is required (else prefer Holon `Context`)
- [ ] No Spring MVC annotations
- [ ] No Spring Security imports (filter-chain only + justification if needed)
- [ ] No `jakarta.persistence` without Holon JPA Datastore + justification
- [ ] No raw Vaadin core components (`com.vaadin.flow.component.*`) — if no Holon equivalent exists, **stop and ask the developer** before writing any code
- [ ] Services preferably retrieved via `Context.get()`; any Spring bean uses constructor injection
- [ ] Views are thin — persistence/business logic lives in a `*Service` class
- [ ] Bean state validated before `Datastore.save(...)`; errors surfaced via Vaadin `Notification`; SLF4J logging
- [ ] Auth guard (`@Permitted` or `AuthContext.require().isPermitted(...)`) on every route that needs a role
- [ ] `BeanPropertySet` constant is `public static final`
- [ ] Views use `@Route(value = "...", layout = MainLayout.class)` for the app shell
- [ ] Input toolbars use `FormLayoutBuilder.create().responsiveSteps(...).add(...).build()` — not a bare `HorizontalLayout`
- [ ] Action buttons use semantic variant methods on `ButtonBuilder.create()`: `.primary()`, `.error()`, `.secondary()`, `.tertiary()` — **not** raw `ButtonVariant.LUMO_*` enum
- [ ] Notifications use `NotificationUtil.notificationSuccess/Error(...)` or `NotificationBuilder.create().success()/error()...build().open()` — both from `com.holonplatform.vaadin.flow`; **not** raw Vaadin `Notification` with `NotificationVariant`
- [ ] Visual tokens (colours, fonts, spacing) live in `themes/<app-name>/styles.css`, not hard-coded in Java
- [ ] **I18N** — every user-visible string uses `Localizable.of(fallback, key)` or `LocalizationContext.require().getMessage(key, fallback)`; no raw literals, no Vaadin `getTranslation(...)`
- [ ] **`@Caption`** — every user-visible bean field has `@Caption(message = "<fallback>", messageCode = "<domain>.<field>")`; mandatory fields also have `@NotNull`; `ListingBundle`/`EntityPanelForm` resolve labels automatically from these annotations
- [ ] **A11Y** — every input has a visible `.label(...)`, icon-only buttons have `aria-label`, grids have `aria-label` + translated column headers (from `@Caption`) + empty-state text, dialogs restore focus on close, skip-to-content link present in `MainLayout`
- [ ] Full compilation verified (`./mvnw compile` or `./gradlew compileJava`)

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Read the entity model from `docs/entity_model.md`
3. Read [`references/bean-model.md`](references/bean-model.md) — JavaBean conventions
4. Read [`references/datastore-patterns.md`](references/datastore-patterns.md) — Datastore query/save/delete
5. Read [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) — UI component patterns
6. Read [`references/component-dictionary.md`](references/component-dictionary.md) — Holon ↔ Vaadin component map with I18N keys and A11Y rules
7. Read [`references/security-patterns.md`](references/security-patterns.md) — Holon Auth patterns
7. Read [`references/context-wiring.md`](references/context-wiring.md) — Holon Context wiring
8. Check existing code for patterns and conventions
9. **Feature package**: determine the feature name from the use case (e.g. `customer`, `invoice`). All classes produced in steps below go in `com.example.<app>.<feature>` — bean, model, service, and view in the **same** package. Genuinely cross-cutting classes (application shell, auth config) go in `com.example.<app>.shared`. **Never create `domain/`, `service/`, or `ui/` layer packages** — they break co-location and make the feature-package rule unenforceable.
10. **Domain + Model**: create or update JavaBean(s) with `@DataPath` / `@Identifier`, `@Caption(message, messageCode)` on **every** user-visible field, and `@NotNull` on mandatory fields; create a companion `<Entity>Model` interface with `BeanPropertySet` and typed `PathProperty` constants for every bean field
11. **Service**: implement service class using `BeanDatastoreHelper<T>` in the same feature package — use `BeanDatastoreHelper` methods for all queries; fall back to raw `BeanDatastore` only when `BeanDatastoreHelper` has no equivalent method (prefer Holon `Context`; a `@Service` with constructor injection is allowed when Spring lifecycle is required)
12. **View**: implement Holon Vaadin Flow view in the same feature package (`@Route(layout = MainLayout.class)`, `Components.listing(T.class)` for grids / `EntityFormPanel.bean(T.class)` for forms, `FormLayoutBuilder` toolbar with responsive steps, semantic button variants (`.primary()`, `.error()`, etc.) on all buttons, Holon Auth guards)
13. **Theme**: ensure `src/main/resources/META-INF/resources/themes/<app-name>/styles.css` exists with Lumo token overrides; `MainLayout` carries `@StyleSheet("context://themes/<app-name>/styles.css")` (Vaadin 25 approach — no `@Theme` / frontend bundle required)
14. Run the Pre-Emit Checklist — fix any violation before proceeding
15. Verify the full implementation compiles successfully

## Resources

- [`references/bean-model.md`](references/bean-model.md) — `@DataPath`, `@Identifier`, `BeanPropertySet`
- [`references/datastore-patterns.md`](references/datastore-patterns.md) — query, save, delete idioms
- [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) — `Components.input.*`, `ListingBundle`, `EntityPanelForm`, responsive layout (`AppLayout`, `FormLayout`), button variants, theme CSS
- [`references/component-dictionary.md`](references/component-dictionary.md) — authoritative Holon component dictionary with correct class names, package paths, real API snippets, I18N conventions, and A11Y rules — **read this before emitting any component**
- [`references/security-patterns.md`](references/security-patterns.md) — `Realm`, `AuthContext`, `@Permitted`, role/permission model
- [`references/context-wiring.md`](references/context-wiring.md) — `Context.get()`, Holon Spring Boot auto-config, fallback policy
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for Vaadin 25 component docs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Platform API
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
