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
- Data grids use `ListingBundle`; entity forms use `EntityPanelForm` (both `com.holonplatform.vaadin.flow.components`) — bare `PropertyListing` / `PropertyForm` only with `// FALLBACK: ListingBundle/EntityPanelForm cannot express <thing>`
- `com.vaadin.*` — **fallback only**, every use must be preceded by `// FALLBACK: no Holon equivalent for <thing>`
- `org.springframework.boot:spring-boot-starter` + Holon Spring Boot starters (bootstrap only)
- `org.springframework.stereotype.{Service,Component,Repository}` — permitted when the class needs Spring lifecycle (`@Transactional`, `@EventListener`, `@Scheduled`); Holon `Context` still preferred; inject via constructors
- `org.flywaydb.*`, `org.postgresql.*`, `org.junit.jupiter.*`, `org.testcontainers.*`, `com.microsoft.playwright.*`

### Banned — refuse to emit

- `com.holonplatform.core.property.PropertyBox` — **use `Bean` + `BeanPropertySet` exclusively**
- `jakarta.persistence.*` / `javax.persistence.*` — unless Holon JPA Datastore is explicitly required and justified
- `org.springframework.data.jpa.*`, `org.springframework.data.repository.*`
- `org.springframework.web.bind.annotation.*` (no Spring MVC — Vaadin is the UI)
- `org.springframework.beans.factory.annotation.Autowired` — use **constructor injection** (preferred: Holon `Context.get()`)
- `org.springframework.security.*` for auth — use Holon Auth instead

## Pre-Emit Checklist

- [ ] No `PropertyBox` in emitted code
- [ ] Data grids use `ListingBundle`; forms use `EntityPanelForm` (bare `PropertyListing` / `PropertyForm` only with `// FALLBACK:` justification)
- [ ] No `@Autowired` — dependencies injected via constructors
- [ ] `@Service` / `@Component` / `@Repository` used only when Spring lifecycle is required (else prefer Holon `Context`)
- [ ] No Spring MVC annotations
- [ ] No Spring Security imports (filter-chain only + `// FALLBACK:` if needed)
- [ ] No `jakarta.persistence` without Holon JPA Datastore + justification
- [ ] Every raw Vaadin fallback has `// FALLBACK: no Holon equivalent for <thing>`
- [ ] Services preferably retrieved via `Context.get()`; any Spring bean uses constructor injection
- [ ] Views are thin — persistence/business logic lives in a `*Service` class
- [ ] Bean state validated before `Datastore.save(...)`; errors surfaced via Vaadin `Notification`; SLF4J logging
- [ ] Auth guard (`@Permitted` or `AuthContext.require().isPermitted(...)`) on every route that needs a role
- [ ] `BeanPropertySet` constant is `public static final`
- [ ] Views use `@Route(value = "...", layout = MainLayout.class)` for the app shell
- [ ] Input toolbars use `FormLayout` with `setResponsiveSteps(...)` — not a bare `HorizontalLayout`
- [ ] Action buttons carry appropriate `ButtonVariant` (`LUMO_PRIMARY`, `LUMO_ERROR`, etc.)
- [ ] Error notifications use `NotificationVariant.LUMO_ERROR`; success notifications include a duration and position
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
9. **Layer 1 — Domain**: create or update JavaBean(s) with `@DataPath` / `@Identifier`, `@Caption(message, messageCode)` on **every** user-visible field, and `@NotNull` on mandatory fields; declare `BeanPropertySet`
10. **Layer 2 — Service**: implement Datastore-backed service class (prefer Holon `Context`; a `@Service` with constructor injection is allowed when Spring lifecycle is required)
11. **Layer 3 — View**: implement Holon Vaadin Flow view (`@Route(layout = MainLayout.class)`, `ListingBundle` for grids / `EntityPanelForm` for forms, `FormLayout` toolbar with responsive steps, `ButtonVariant` on all buttons, Holon Auth guards)
12. **Layer 4 — Theme**: ensure `src/main/resources/META-INF/resources/themes/<app-name>/styles.css` exists with Lumo token overrides; `MainLayout` carries `@StyleSheet("context://themes/<app-name>/styles.css")` (Vaadin 25 approach — no `@Theme` / frontend bundle required)
13. Run the Pre-Emit Checklist — fix any violation before proceeding
14. Verify the full implementation compiles successfully

## Resources

- [`references/bean-model.md`](references/bean-model.md) — `@DataPath`, `@Identifier`, `BeanPropertySet`
- [`references/datastore-patterns.md`](references/datastore-patterns.md) — query, save, delete idioms
- [`references/holon-vaadin-ui.md`](references/holon-vaadin-ui.md) — `Components.input.*`, `ListingBundle`, `EntityPanelForm`, responsive layout (`AppLayout`, `FormLayout`), button variants, theme CSS
- [`references/component-dictionary.md`](references/component-dictionary.md) — full Holon ↔ Vaadin component dictionary with mandatory I18N key conventions and A11Y (ARIA / WCAG 2.1 AA) rules per component
- [`references/security-patterns.md`](references/security-patterns.md) — `Realm`, `AuthContext`, `@Permitted`, role/permission model
- [`references/context-wiring.md`](references/context-wiring.md) — `Context.get()`, Holon Spring Boot auto-config, fallback policy
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for Vaadin 25 component docs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Platform API
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
