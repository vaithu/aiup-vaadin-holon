# Holon Stack Rules

This file is the authoritative allow/ban list for every construction skill in `aiup-vaadin-holon`.
Skills MUST read this file at the start of each code-generation task and enforce the rules below.

---

## LTS Version Pins

| Component | Artifact / Version | Source of truth |
|-----------|-------------------|-----------------|
| Java | 25 | — |
| Holon Core | `com.holon-platform.core:holon-core:10.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Auth | `com.holon-platform.core:holon-auth:10.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Holon JDBC Datastore BOM | `com.holon-platform.jdbc:holon-datastore-jdbc-bom:10.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Holon JDBC Datastore | `com.holon-platform.jdbc:holon-starter-jdbc-datastore:10.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Holon JPA Datastore | `com.holon-platform.jpa:holon-datastore-jpa-spring-boot:10.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Vaadin Flow BOM | `com.holon-platform.vaadin:holon-vaadin-flow-bom:10.0.1` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Vaadin Flow | `com.holon-platform.vaadin:holon-vaadin-flow:10.0.1` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Vaadin Flow Spring Boot | `com.holon-platform.vaadin:holon-vaadin-flow-spring-boot:10.0.1` | https://github.com/vaithu/holon-vaadin-flow |
| Vaadin Flow | `com.vaadin:vaadin-bom:25.2.1` | — |
| Spring Boot | `org.springframework.boot:spring-boot-starter:4.1.0` | — |
| Jakarta Servlet | `jakarta.servlet:jakarta.servlet-api:6.1.0` (Jakarta EE 11) | — |
| Flyway | `org.flywaydb:flyway-core:10.x` | — |
| PostgreSQL | `org.postgresql:postgresql:42.x` | — |
| JUnit | `org.junit.jupiter:junit-jupiter:5.x` | — |
| Testcontainers | `org.testcontainers:postgresql:latest` | — |
| Playwright | `com.microsoft.playwright:playwright:latest` | — |
| Vaadin Testbench Unit | `com.vaadin:vaadin-testbench-unit-junit5:latest` | — |

> **Holon 10.x source:** Holon 10.x artifacts target Vaadin 25.2 + Spring Boot 4.1 + Jakarta EE 11.
> Build from the fork at https://github.com/vaithu/holon-vaadin-flow (branch `modernize/java-20260526093038`)
> and install locally (`mvn install`) before using in application projects.

> **BOM usage:** Import `holon-vaadin-flow-bom` for Vaadin Flow types, `holon-datastore-jdbc-bom`
> for JDBC datastore, and `vaadin-bom` for Vaadin components. The artifact `com.holon-platform:holon-bom`
> does **not** exist — use the per-module BOMs listed above.

---

## Allowed Dependencies

Skills MAY emit imports from these packages without justification:

```
com.holon-platform.*                    — all Holon modules
com.holonplatform.*                     — (same, alternate package root used in older artifacts)
org.springframework.boot:spring-boot-starter
                                        — bootstrap runtime only
com.holon-platform.*:holon-spring-boot-*
                                        — Holon Spring Boot starters (auto-config)
org.flywaydb:flyway-core               — schema migrations
org.postgresql:postgresql              — JDBC driver
org.junit.jupiter:*                    — test layer
org.testcontainers:*                   — test layer
com.microsoft.playwright:*             — E2E test layer
com.vaadin:vaadin-testbench-unit-junit5
                                        — Vaadin Browserless tests
```

**Vaadin fallback** — Skills MAY also use:

```
com.vaadin:vaadin-core
com.vaadin.flow.component.*
```

**Only when Holon Vaadin Flow has no equivalent.** Every such use MUST be preceded by:

```java
// FALLBACK: no Holon equivalent for <describe the specific thing>
```

**Spring stereotypes** — Skills MAY emit `@Service`, `@Component`, and `@Repository`
**when the class needs to participate in Spring's lifecycle** (e.g. `@Transactional`,
`@EventListener`, `@Scheduled`, Spring Data callbacks). No `// FALLBACK:` comment is
required, but:

- Retrieving services via Holon `Context` remains the **preferred idiom**; reach for a
  Spring stereotype only when Spring lifecycle participation is actually needed.
- Dependencies MUST be supplied via **constructor injection** — `@Autowired` field or
  setter injection is banned (see below).

```java
@SpringBootApplication   // on the main application class only

@Service                 // permitted when Spring lifecycle is required
public class BillService {
    private final Datastore datastore;

    public BillService(Datastore datastore) {   // constructor injection — no @Autowired
        this.datastore = datastore;
    }
}
```

**Other Spring fallback** — Skills MAY use any other `org.springframework.*` core type
**only where Holon has no alternative**. The `// FALLBACK:` comment is required for those.

---

## Banned Imports

Skills MUST refuse to emit code containing any of the following. Verification:
`git grep -n '<import>' src/` — if any result exists outside a `// FALLBACK:` comment, it is a violation.

| Import / class | Reason | Correct alternative |
|---------------|--------|---------------------|
| `com.holonplatform.core.property.PropertyBox` | **Use `Bean` + `BeanPropertySet` exclusively** | Plain JavaBean + `BeanPropertySet<T>` |
| `jakarta.persistence.*` / `javax.persistence.*` | JPA annotations — not allowed unless Holon JPA Datastore is required and justified | `@DataPath` / `@Identifier` on plain JavaBean |
| `org.springframework.data.jpa.*` | Spring Data JPA — replaced by Holon Datastore | `Datastore` + `BeanPropertySet` |
| `org.springframework.data.repository.*` | Spring Data repositories | `Datastore` + `BeanPropertySet` |
| `org.springframework.web.bind.annotation.*` | Spring MVC REST — Vaadin IS the UI layer | n/a — no REST layer |
| `org.springframework.beans.factory.annotation.Autowired` | Field/setter injection — hides dependencies | **constructor injection** (preferred: Holon `Context.get()`) |
| `org.springframework.security.core.*` | Spring Security | Holon Auth: `Realm`, `Authenticator`, `AuthContext`, `Permission` |
| `org.springframework.security.config.*` | Spring Security config | Holon Auth bootstrap + filter-chain wiring only if Holon Auth requires it |
| `com.vaadin.flow.i18n.I18NProvider` and direct `UI.getCurrent().getTranslation(...)` usage | Keep localization consistent with Holon stack conventions | Holon Core i18n (`Localizable` + `LocalizationContext.require().getMessage(key, fallback)`) |
| `com.vaadin.flow.theme.Theme` (`@Theme`) | Superseded by `@StyleSheet` + static CSS in Vaadin 25; requires Vite bundle build | `@StyleSheet("context://themes/<app>/styles.css")` on `MainLayout`; CSS in `META-INF/resources/` |

> **Spring stereotypes note:** `@Service`, `@Component`, and `@Repository` are **permitted**
> (see *Allowed Dependencies*) when the class needs Spring lifecycle participation, but
> Holon `Context` wiring is still preferred and `@Autowired` remains banned — inject via
> constructors.

---

## Component Preferences

When Holon offers more than one component for the same job, Skills MUST emit the
preferred composite component below. The lower-level primitive is allowed **only** when
the preferred bundle cannot express the requirement, and every such use MUST be preceded
by `// FALLBACK: ListingBundle/EntityPanelForm cannot express <thing>`.

| UI concern | Preferred component | Do **not** use directly | Correct usage |
|-----------|---------------------|-------------------------|---------------|
| Data grid / table listing | `com.holonplatform.vaadin.flow.components.ListingBundle` | `PropertyListing` | `ListingBundle.builder(Bean.PROPERTIES).dataSource(ds, Bean.PROPERTIES.getDataPath()).build()` |
| Entity form (create / edit / detail) | `com.holonplatform.vaadin.flow.components.EntityPanelForm` | `PropertyForm` | `EntityPanelForm.builder(Bean.PROPERTIES).build()` + `setBean()` / `getBean()` |

> `ListingBundle` wraps a `PropertyListing`; `EntityPanelForm` wraps a `PropertyForm`.
> Both bind to a `BeanPropertySet<T>` and work with plain beans — never `PropertyBox`.

---

## Preferred Idioms

### Domain model

```java
import com.holonplatform.core.beans.DataPath;
import com.holonplatform.core.beans.Identifier;
import com.holonplatform.core.beans.NotNull;
import com.holonplatform.core.i18n.Caption;

@DataPath("order")
public class Order {

    @Identifier
    @Caption(message = "ID", messageCode = "order.id")
    private Long id;

    @NotNull
    @DataPath("customer_name")
    @Caption(message = "Customer Name", messageCode = "order.customerName")
    private String customerName;

    @NotNull
    @DataPath("total_amount")
    @Caption(message = "Total Amount (USD)", messageCode = "order.totalAmount")
    private BigDecimal totalAmount;

    @NotNull
    @Caption(message = "Status", messageCode = "order.status")
    private String status;          // "PENDING", "APPROVED", "REJECTED"

    // standard getters / setters
}
```

> `@Caption(message, messageCode)` is **required on every user-visible field**. The
> `BeanPropertySet` reads it at build time; `ListingBundle` and `EntityPanelForm` use
> it automatically for column headers and field labels — no `.columnHeader()` /
> `.propertyCaption()` calls needed in the view.

### Property set

```java
public static final BeanPropertySet<Order> PROPERTIES =
    BeanPropertySet.create(Order.class);
```

### Persistence (Datastore)

```java
// Retrieve Datastore from Holon Context — never @Autowired
Datastore ds = Context.get()
    .resource(Datastore.CONTEXT_KEY, Datastore.class)
    .orElseThrow(() -> new IllegalStateException("Datastore not available in Context"));

// Query
List<Order> orders = ds.query(PROPERTIES.getDataPath())
    .filter(PROPERTIES.property("status").eq("PENDING"))
    .list(PROPERTIES);

// Single record
Optional<Order> order = ds.query(PROPERTIES.getDataPath())
    .filter(PROPERTIES.property("id").eq(id))
    .findOne(PROPERTIES);

// Insert / update
ds.save(PROPERTIES.getDataPath(), order);   // Bean form — never PropertyBox

// Delete
ds.delete(PROPERTIES.getDataPath(),
    PROPERTIES.property("id").eq(id));
```

### UI (Holon Vaadin Flow)

Grids MUST use `ListingBundle`; forms MUST use `EntityPanelForm`. Both are Holon
Vaadin Flow composite components (package `com.holonplatform.vaadin.flow.components`)
bound to a `BeanPropertySet`. Reach for the lower-level `PropertyListing` /
`PropertyForm` primitives only when the bundle cannot express the need — see
**Component Preferences** below.

```java
import com.holonplatform.vaadin.flow.components.ListingBundle;

@Route("orders")
@Permitted("orders:view")
public class OrderListView extends VerticalLayout {

    public OrderListView() {
        Datastore ds = Context.get()
            .resource(Datastore.CONTEXT_KEY, Datastore.class).orElseThrow();

        ListingBundle<Order> bundle = ListingBundle
            .builder(Order.PROPERTIES)
            .dataSource(ds, Order.PROPERTIES.getDataPath())
            .build();

        add(bundle);
    }
}
```

```java
// Detail / form view
import com.holonplatform.vaadin.flow.components.EntityPanelForm;

EntityPanelForm<Order> form = EntityPanelForm.builder(Order.PROPERTIES).build();
form.setBean(existingOrder);            // populate from bean
Order updated = form.getBean();         // read back
```

```java
// Input component
Input<String> nameInput = Components.input.string()
    .label("Customer Name")
    .required()
    .build();
```

### Localization (Holon I18N)

- Localize every user-facing UI string (labels, button text, notifications, dialog captions,
  validation messages) with **Holon i18n**.
- Do not introduce Vaadin-specific i18n wiring (`I18NProvider`, direct `getTranslation(...)`).
- **Embed I18N in the bean:** annotate every user-visible field with
  `@Caption(message = "<fallback>", messageCode = "<domain>.<field>")` from
  `com.holonplatform.core.i18n.Caption`. `BeanPropertySet` reads these at startup and
  `ListingBundle` / `EntityPanelForm` resolve labels automatically — no `.columnHeader()` /
  `.propertyCaption()` overrides needed in view code.
- Keep translation keys stable and domain-scoped (e.g. `bill.vendorName`, `bill.approve`),
  and keep fallback text next to the key for readability in generated code snippets.
- **Always use** `Localizable.of(fallback, key)` inside component builders and
  `LocalizationContext.require().getMessage(key, fallback)` outside builders (e.g. in
  notification text, dialog text). Never embed raw English literals in user-facing strings.
- See [`../skills/implement/references/component-dictionary.md`](../skills/implement/references/component-dictionary.md)
  for the full key naming convention and per-component I18N examples.

### Accessibility (A11Y — WCAG 2.1 AA)

The stack targets **WCAG 2.1 AA**. Skills MUST satisfy the following for every screen emitted:

- Every input has a visible label via `.label(Localizable.of(...))` — never use placeholder as the sole label. When the input is rendered from an `EntityPanelForm`, the label comes from the bean's `@Caption` annotation automatically.
- Every required field calls `.required(Localizable.of(...))` so `aria-required="true"` is set automatically. For bean-bound forms, annotate the field with `@NotNull` so `EntityPanelForm` applies this automatically.
- Every `Components.button()` has either visible text via `.text(Localizable.of(...))` or an explicit `aria-label` for icon-only buttons.
- Data grids (`ListingBundle`) have: `aria-label` on the grid element, column headers auto-resolved from `@Caption` on bean fields (override with `.columnHeader(prop, Localizable)` only when a view-specific label differs), and a translated empty-state message via `.setEmptyStateText(Localizable)`.
- Confirmation dialogs (`ConfirmDialog`) restore focus to the trigger element on close.
- `MainLayout` contains a visually-hidden skip-to-content link as its first focusable element.
- `SideNav` has `aria-label` set to the translated navigation landmark name.
- Error feedback uses `NotificationVariant.LUMO_ERROR` (sets `role="alert"`) or `ConfirmDialog` with a descriptive header.
- No CSS suppresses `:focus-visible` outline styles; no `tabindex` value > 0.
- See [`../skills/implement/references/component-dictionary.md`](../skills/implement/references/component-dictionary.md)
  for the per-component A11Y checklist.

### Security (Holon Auth)

```java
// Realm bootstrap (one @Bean in a @SpringBootApplication class or @Configuration)
@Bean
public Realm realm() {
    return Realm.builder()
        .withAuthenticator(AccountCredentialsAuthenticator.create(accountProvider()))
        .withAuthorizer(Authorizer.create())
        .withDefaultAuthorization()
        .build();
}

// Route-level guard
@Route("bills/approve")
@Permitted("bills:approve")
public class BillApprovalView extends VerticalLayout { ... }

// Programmatic guard inside a view
if (!AuthContext.require().isPermitted("bills:approve")) {
    approveButton.setVisible(false);
}

// Check the current authenticated principal
Optional<String> username = AuthContext.require()
    .getAuthentication()
    .map(Authentication::getName);
```

### Context wiring

```java
// Register a service in Context (in a Holon Spring Boot auto-config or @Bean)
Context.get().scope(ContextScope.APPLICATION)
    .registerResource(BillService.CONTEXT_KEY, new BillService());

// Retrieve it anywhere
BillService svc = Context.get()
    .resource(BillService.CONTEXT_KEY, BillService.class)
    .orElseThrow();
```

---

## Design Principles

Generated code MUST follow these principles in addition to the stack rules above:

- **Single Responsibility** — one service per aggregate/use-case concern; one view per screen.
  Do not create "god" services or views that mix persistence, business rules, and UI wiring.
- **Thin views, rich services** — `@Route` views handle layout, binding, and user events only.
  All persistence and business logic lives in a service class; views call the service.
- **Dependency inversion via injection** — depend on `Datastore` / service abstractions passed
  through constructors (or resolved from Holon `Context`), never on globals or `@Autowired` fields.
- **Immutability where practical** — service dependencies are `private final`; prefer returning
  new/updated beans over mutating shared state.
- **Small, cohesive methods** — extract query/save/validation steps into named private methods
  rather than long constructors or event handlers.
- **No duplication** — share a single `BeanPropertySet<T>` constant per bean; reuse services
  across views instead of re-querying the Datastore inline.

## Validation & Error Handling

- **Validate at the boundary** — validate bean state in the service before `Datastore.save(...)`.
  Prefer Holon `Validator` / `PropertyBox`-free bean validation over ad-hoc `if` checks.
- **Bind form validation in the UI** — `PropertyForm` / `Input` components declare `.required()`
  and validators so invalid input is caught before submit; never rely on the DB to reject bad data.
- **Surface errors to the user** — catch expected failures in the view and show a Vaadin
  `Notification` (error variant); never swallow exceptions or print stack traces to the user.
- **Fail fast on missing resources** — resolving a `Context` resource uses `.orElseThrow(...)`
  with a descriptive `IllegalStateException`, not a silent `null`.
- **No empty catch blocks** — log (via SLF4J) and either rethrow or convert to a user-facing message.

## Transaction Boundaries

- **Boundary lives in the service, not the view** — a single service method is the unit of work;
  views never manage transactions.
- **Prefer Holon `Datastore.requireTransaction()` / `withTransaction(...)`** for multi-statement
  units of work; a lone `save`/`delete` runs in its own implicit transaction.
- **Use Spring `@Transactional` only** when a transaction must span multiple Datastore/service
  calls that Holon's transaction API cannot wrap — apply it on the service method, and the class
  may then be a `@Service` with constructor injection.
- **Keep transactions short** — no user interaction, remote calls, or long loops inside a
  transactional boundary.

## Naming & Package Conventions

- **Packages** — `domain` (beans), `service` (Datastore-backed services), `ui` (views),
  `security` (Realm/auth config), under a single application root package.
- **Class-name suffixes** — beans use the entity name (`Bill`); services end in `Service`
  (`BillService`); views end in `View` (`BillListView`, `BillDetailView`); auth config ends in
  `RealmConfig` / `Config`.
- **Constants** — the `BeanPropertySet<T>` constant is `public static final PROPERTIES`;
  the service `Context` key is `public static final String CONTEXT_KEY`.
- **Routes** — `@Route` values are lowercase, hyphenated nouns (`"bills"`, `"bills/approve"`).
- **Logging** — use SLF4J (`LoggerFactory.getLogger(...)`); never `System.out` / `System.err`.



- [ ] No `PropertyBox` in emitted code (grep: `PropertyBox`)
- [ ] Data grids use `ListingBundle`; bare `PropertyListing` only with a `// FALLBACK:` justification (grep: `PropertyListing`)
- [ ] Forms use `EntityPanelForm`; bare `PropertyForm` only with a `// FALLBACK:` justification (grep: `PropertyForm`)
- [ ] No `@Autowired` — dependencies injected via constructors (grep: `@Autowired`)
- [ ] `@Service` / `@Component` / `@Repository` used only when Spring lifecycle is required (else prefer Holon `Context`)
- [ ] No Spring MVC (`@RestController`, `@GetMapping`, etc.)
- [ ] No Spring Security imports (unless filter-chain only, with `// FALLBACK:` comment)
- [ ] No `jakarta.persistence` / `javax.persistence` unless Holon JPA Datastore + justification comment
- [ ] No Spring Data JPA / Spring Data Repository
- [ ] Every raw Vaadin use has `// FALLBACK: no Holon equivalent for <thing>`
- [ ] Services preferably retrieved via `Context.get()`; any Spring bean uses constructor injection
- [ ] Auth guards present on every `@Route` that requires a role/permission
- [ ] BeanPropertySet constant is `public static final`
- [ ] Views are thin — persistence/business logic lives in a `*Service` class
- [ ] Bean state validated in the service before `Datastore.save(...)`; form inputs declare validators
- [ ] Errors surfaced via Vaadin `Notification`; no empty catch blocks; SLF4J logging (no `System.out`)
- [ ] Multi-statement units of work run inside a transaction boundary owned by the service
- [ ] Packages follow `domain` / `service` / `ui` / `security`; classes use the standard suffixes
- [ ] Flyway migration uses sequences (not `SERIAL` / `IDENTITY`) for PKs
- [ ] **I18N** — every user-visible string uses `Localizable.of(fallback, key)` or `LocalizationContext.require().getMessage(key, fallback)`; no raw literals; no `getTranslation(...)`
- [ ] **`@Caption`** — every user-visible bean field carries `@Caption(message = "<fallback>", messageCode = "<domain>.<field>")`; mandatory fields also carry `@NotNull`
- [ ] **A11Y** — every input has `.label(...)`, required fields call `.required(...)`, icon-only buttons have `aria-label`, grids have `aria-label` + translated column headers + empty-state text, dialogs restore focus on close, skip-to-content link in `MainLayout`, no `:focus-visible` suppression in CSS
