# Context Wiring Reference

In the Holon Platform stack, dependencies are **preferably** resolved through the
**Holon `Context`** API rather than Spring's `@Autowired` field injection. Spring
stereotypes (`@Service` / `@Component` / `@Repository`) are permitted when a class needs
to participate in Spring's lifecycle, but injection is always done via **constructors**.

---

## Core principle

> **Never** use `@Autowired` — inject dependencies through **constructors**.  
> **Prefer** retrieving resources via `Context.get().resource(key, type)`.  
> `@Service` / `@Component` / `@Repository` are allowed **only** when the class needs
> Spring lifecycle features (`@Transactional`, `@EventListener`, `@Scheduled`, Spring Data
> callbacks).

The always-permitted Spring annotation is `@SpringBootApplication` on the main class.
All other wiring goes through Holon starters and the Holon `Context`, or — when Spring
lifecycle is genuinely required — through constructor-injected Spring beans.

---

## Holon Context API

```java
import com.holonplatform.core.Context;
import com.holonplatform.core.ContextScope;

// Retrieve a resource (throws if absent)
Datastore ds = Context.get()
    .resource(Datastore.CONTEXT_KEY, Datastore.class)
    .orElseThrow(() -> new IllegalStateException("Datastore not available"));

// Retrieve a custom service
BillService svc = Context.get()
    .resource(BillService.CONTEXT_KEY, BillService.class)
    .orElseThrow();

// Register a resource manually (typically done via @Bean returning the instance)
Context.get().scope(ContextScope.APPLICATION)
    .registerResource(BillService.CONTEXT_KEY, new BillService());
```

---

## Holon Spring Boot auto-configuration

The Holon Spring Boot starters automatically register Holon resources into both the
Spring `ApplicationContext` and the Holon `Context`:

| Starter | Registers in Context |
|---------|---------------------|
| `holon-spring-boot-jdbc-datastore` | `Datastore` (JDBC) |
| `holon-spring-boot-jpa-datastore` | `Datastore` (JPA) |
| `holon-vaadin-flow-spring-boot` | Vaadin integration, `AuthContext` |
| `holon-spring-boot` (core) | `Context` scopes, Spring integration |

No manual `Context.get().scope(...).registerResource(...)` is needed for Holon's own
managed resources.

---

## Service registration pattern

Define services as plain classes with a `CONTEXT_KEY` constant. Register them via a
`@Bean` method in the main application class or a `@Configuration` class.

```java
// In the service class
public class BillService {
    public static final String CONTEXT_KEY = BillService.class.getName();
    // implementation...
}

// In @SpringBootApplication or @Configuration
@Bean
public BillService billService() {
    return new BillService();           // Spring manages lifecycle;
                                        // Holon auto-config may also publish it to Context
}
```

If the Spring bean is not automatically published to the Holon Context (check Holon
starter documentation), register it explicitly:

```java
@Bean
public BillService billService() {
    BillService svc = new BillService();
    // publish to Holon Context manually if starter doesn't do it automatically
    Context.get().scope(ContextScope.APPLICATION)
        .registerResource(BillService.CONTEXT_KEY, svc, BillService.class);
    return svc;
}
```

---

## When Spring fallback is allowed

Use `org.springframework.*` types **only** when Holon has no equivalent.
Required comment:

```java
// FALLBACK: no Holon equivalent for <describe what you need>
```

**Allowed Spring fallbacks (examples):**

| Spring type | When allowed |
|-------------|-------------|
| `@SpringBootApplication` | Main class annotation — always permitted, no comment needed |
| `@Configuration` | When grouping `@Bean` definitions for Holon-registered resources |
| `@Bean` | To return a resource that Holon starters will pick up |
| `@Scheduled` | Scheduled jobs (Holon has no scheduler equivalent) |
| `@Transactional` | When Holon `Datastore.withTransaction()` cannot span the required boundary |
| `SecurityFilterChain` | HTTP filter-chain wiring when Holon Auth requires it (see security-patterns.md) |

**Never allowed (no exception):**

- `@Autowired` — always use constructor injection (preferably resolving from `Context.get()`)
- `@RestController`, `@GetMapping`, etc. — Vaadin IS the UI; there is no REST layer

---

## When to use Spring stereotypes vs. Holon `Context`

`@Service`, `@Component`, and `@Repository` are **permitted**, but Holon `Context` remains
the default. Choose as follows:

| Situation | Use |
|-----------|-----|
| Plain Datastore-backed service, no Spring lifecycle needed | **Holon `Context`** — plain class + `CONTEXT_KEY`, retrieved via `Context.get()` |
| Service method must be `@Transactional` | **`@Service`** with a `@Transactional` method, constructor-injected `Datastore` |
| Class listens to Spring events (`@EventListener`) | **`@Component`** with constructor injection |
| Scheduled job (`@Scheduled`) | **`@Component`** / `@Service` with constructor injection |
| Spring Data callback / repository infrastructure | **`@Repository`** with constructor injection |

**Rules that always apply to stereotype-annotated classes:**

- Dependencies are injected via the **constructor** — never `@Autowired` fields or setters.
- Keep the class single-responsibility; do not fold UI or unrelated concerns into it.
- Prefer exposing the service through Holon `Context` as well, so views can resolve it uniformly.

```java
@Service                                  // permitted: needs @Transactional
public class BillService {

    private final Datastore datastore;

    public BillService(Datastore datastore) {   // constructor injection — no @Autowired
        this.datastore = datastore;
    }

    @Transactional
    public void approve(Long billId) {
        // multi-statement unit of work spanning several Datastore calls
    }
}
```

---

## Initialization order

Because Holon resources are registered during Spring Boot startup, use a
`CommandLineRunner` or `ApplicationReadyEvent` listener for any bootstrapping that
requires a fully configured Context:

```java
@Bean
public ApplicationListener<ApplicationReadyEvent> appReady() {
    return event -> {
        // Safe to use Context.get() here — all starters have run
        Datastore ds = Context.get()
            .resource(Datastore.CONTEXT_KEY, Datastore.class).orElseThrow();
        // seed data, warm caches, etc.
    };
}
```
