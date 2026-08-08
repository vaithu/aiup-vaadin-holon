# Context Wiring Reference

In the Holon Platform stack, all dependencies are wired via **Spring constructor injection**.
`@Autowired` is banned. The Holon Spring Boot starters register the `Datastore` and other
Holon resources as Spring beans — inject them the same way you inject any other dependency.

---

## Core principle

> **Inject everything via constructors.** Never use `@Autowired`, `Context.get()` to resolve
> application-level services, or field injection.

`@Service` / `@Component` / `@Repository` are allowed **only** when the class needs Spring
lifecycle features (`@Transactional`, `@EventListener`, `@Scheduled`). Otherwise, define
services as plain classes registered via `@Bean`.

---

## Holon Spring Boot auto-configuration

The Holon Spring Boot starters automatically register Holon resources as Spring beans:

| Starter | Registers as Spring bean |
|---------|--------------------------|
| `holon-spring-boot-jdbc-datastore` | `Datastore` (JDBC) |
| `holon-spring-boot-jpa-datastore` | `Datastore` (JPA) |
| `holon-vaadin-flow-spring-boot` | Vaadin integration, `AuthContext` |

No `Context.get().scope(...).registerResource(...)` is needed for Holon's own managed resources.
No `CONTEXT_KEY` constants are needed on service classes.

---

## Service registration pattern

Define services as plain classes with constructor injection of `Datastore`. Register them
via a `@Bean` method in the main application class or a `@Configuration` class.

```java
// BillService.java — plain class, no Holon/Spring annotations needed
public class BillService {

    private final BeanDatastoreHelper<Bill> helper;

    public BillService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
    }

    public List<Bill> findAll() {
        return helper.findAll().toList();
    }
}

// In @SpringBootApplication or @Configuration
@Bean
public BillService billService(Datastore datastore) {
    return new BillService(datastore);   // Datastore is auto-registered by the Holon starter
}
```

Views and other beans receive the service via constructor injection too:

```java
@Route("bills")
@Permitted("bills:view")
public class BillListView extends VerticalLayout {

    public BillListView(BillService svc) {   // Spring injects the @Bean
        add(ListingBundle.builder(BillModel.PROPERTY_SET).items(svc::findAll).build());
    }
}
```

---

## When Spring stereotypes are allowed

| Situation | Use |
|-----------|-----|
| Plain Datastore-backed service, no Spring lifecycle needed | Plain class + `@Bean` |
| Service method must be `@Transactional` | `@Service` with constructor-injected `Datastore` |
| Class listens to Spring events (`@EventListener`) | `@Component` with constructor injection |
| Scheduled job (`@Scheduled`) | `@Component` / `@Service` with constructor injection |

```java
@Service                                  // permitted: needs @Transactional
public class BillService {

    private final BeanDatastoreHelper<Bill> helper;

    public BillService(Datastore datastore) {   // constructor injection — no @Autowired
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
    }

    @Transactional
    public void approve(Long billId) {
        // multi-statement unit of work spanning several Datastore calls
    }
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
| `@Configuration` | When grouping `@Bean` definitions |
| `@Bean` | To declare a service or resource managed by Spring |
| `@Scheduled` | Scheduled jobs (Holon has no scheduler equivalent) |
| `@Transactional` | When `BeanDatastoreHelper.withTransaction()` cannot span the required boundary |
| `SecurityFilterChain` | HTTP filter-chain wiring when Holon Auth requires it (see security-patterns.md) |

**Never allowed (no exception):**

- `@Autowired` — always use constructor injection
- `Context.get()` to look up application services — use constructor injection instead
- `@RestController`, `@GetMapping`, etc. — Vaadin IS the UI; there is no REST layer
