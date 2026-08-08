---
name: jpa-to-domain-service
description: Create a Spring JPA Repository (marker only — no custom methods) and a Holon-powered Service class for a JPA entity. Use when asked to add data-access or service layer for an entity, create a repository, or wire up CRUD operations.
argument-hint: "[EntityName or package path]"
---

# JPA → Domain Repository + Service

Create the domain layer for the entity (or entities) identified by $ARGUMENTS.
Each entity produces **two artefacts** placed in the `domain` sub-package of the entity's package:

1. **`<Entity>Repository`** — a Spring Data JPA repository with **no custom methods**.
   Its only purpose is to register the entity with Spring Data's JPA context and provide
   standard inherited operations (`findById`, `findAll`, `save`, `delete`, `count`, etc.)
   that the service may use for reads.
2. **`<Entity>Service`** — a Spring `@Service` that:
   - uses **`BeanDatastoreHelper<T>`** (Holon Platform) for all **write** operations
     (`save`, `delete`, `deleteById`)
   - delegates **read** operations to the repository (`findById`, `findAll`, paginated
     `findAll(Pageable)`)
   - wraps every write in `@Transactional`
   - logs every operation via SLF4J `log`

---

## Prerequisite

Before generating the domain layer, the entity's **Holon property model** must already exist
(produced by the `jpa-to-holon-bean-properties` skill). The service imports the `*Model`
interface from the `model` sub-package for any future Datastore query extensions.

If the `*Model` interface does not yet exist, run `jpa-to-holon-bean-properties` first.

---

## Package layout

```
com.iyensoft.<module>/
  <Entity>.java                   ← JPA entity (already exists)
  model/
    <Entity>Model.java            ← Holon property model (prerequisite)
  domain/
    <Entity>Repository.java       ← NEW: empty Spring Data JPA repository
    <Entity>Service.java          ← NEW: Holon BeanDatastoreHelper service
```

---

## Step 1 — Create the Repository

The repository **must have zero custom methods**. All data access logic lives in the service.
The repository exists solely as a Spring Data integration point.

```java
package <entity.package>.domain;

import <entity.package>.<Entity>;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link <Entity>}.
 *
 * <p><strong>No custom query methods.</strong> All data-access logic is
 * implemented in {@link <Entity>Service} using Holon Platform's
 * {@code BeanDatastoreHelper} for writes and the inherited repository
 * methods for reads. Adding query methods here is explicitly forbidden —
 * use {@code Datastore} + {@code <Entity>Model.PROPERTY_SET} instead.
 */
@Repository
public interface <Entity>Repository extends JpaRepository<<Entity>, Long> {
    // intentionally empty — see <Entity>Service for all data-access logic
}
```

---

## Step 2 — Create the Service

```java
package <entity.package>.domain;

import <entity.package>.<Entity>;
import <entity.package>.model.<Entity>Model;
import com.holonplatform.core.beans.BeanDatastoreHelper;
import com.holonplatform.core.query.BeanProjection;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QuerySort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Domain service for {@link <Entity>}.
 *
 * <p>Write operations delegate to {@link BeanDatastoreHelper}.
 * Read operations use the inherited JPA repository for scalar / paginated access,
 * and {@code BeanProjection.of(<Entity>.class)} for lazy server-side streaming —
 * the stream always returns actual bean instances, never {@code PropertyBox}.
 *
 * <p>{@code @Transactional(readOnly = true)} at class level; write methods override
 * with a full read-write transaction.
 */
@Service
@Transactional(readOnly = true)
public class <Entity>Service {

    private static final Logger log = LoggerFactory.getLogger(<Entity>Service.class);

    // ── CRUD delegate ─────────────────────────────────────────────────────────
    private final BeanDatastoreHelper<<Entity>> helper;
    private final <Entity>Repository repository;

    public <Entity>Service(com.holonplatform.core.datastore.Datastore datastore,
                           <Entity>Repository repository) {
        this.helper     = BeanDatastoreHelper.of(datastore, <Entity>.class);
        this.repository = repository;
    }

    // ── Read operations (JPA repository — scalar / paginated) ─────────────────

    public Optional<<Entity>> findById(Long id) {
        return repository.findById(id);
    }

    /** Do not use on large tables — prefer {@link #findAll(Pageable)} or {@link #streamAll()}. */
    public List<<Entity>> findAll() {
        return repository.findAll();
    }

    public Page<<Entity>> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public long count() {
        return repository.count();
    }

    // ── Streaming read operations (Holon Datastore — lazy, server-side) ───────

    /**
     * Stream all entities as actual bean instances using a lazy server-side cursor.
     * The result set is <strong>never loaded into heap</strong>.
     *
     * <p><strong>Lazy-loading rules:</strong>
     * <ul>
     *   <li>The <em>caller</em> must be inside an active {@code @Transactional} context
     *       so the JPA session stays open for lazy-association access on the returned beans.</li>
     *   <li>Always close the stream with try-with-resources — the cursor holds a
     *       JDBC connection until closed.</li>
     * </ul>
     *
     * @return a lazy {@code Stream<<Entity>>} — close after use
     */
    public Stream<<Entity>> streamAll() {
        return helper.getDatastore()
                .query(<Entity>Model.TARGET)
                .stream(BeanProjection.of(<Entity>.class));
    }

    /**
     * Stream entities matching a filter/sort as actual bean instances.
     * Same lazy-loading rules apply as for {@link #streamAll()}.
     *
     * <p>Example:
     * <pre>{@code
     * try (Stream<<Entity>> s = service.stream(
     *         <Entity>Model.ACTIVE.eq(true), <Entity>Model.NAME.asc())) {
     *     s.forEach(e -> process(e));
     * }
     * }</pre>
     *
     * @param filter Holon query filter (e.g. {@code <Entity>Model.ACTIVE.eq(true)})
     * @param sort   Holon query sort   (e.g. {@code <Entity>Model.NAME.asc()})
     * @return a lazy {@code Stream<<Entity>>} — close after use
     */
    public Stream<<Entity>> stream(QueryFilter filter, QuerySort sort) {
        return helper.getDatastore()
                .query(<Entity>Model.TARGET)
                .filter(filter)
                .sort(sort)
                .stream(BeanProjection.of(<Entity>.class));
    }

    // ── Write operations ──────────────────────────────────────────────────────

    @Transactional
    public <Entity> save(<Entity> entity) {
        log.info("Saving <entity>: {}", entity);
        return helper.save(entity).getResult().orElse(entity);
    }

    @Transactional
    public void delete(<Entity> entity) {
        log.info("Deleting <entity> id={}", entity.getId());
        helper.delete(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting <entity> id={}", id);
        findById(id).ifPresent(helper::delete);
    }
}
```

/**
 * Domain service for {@link <Entity/>}.
 *
 * <p>Write operations are delegated to Holon Platform's {@link BeanDatastoreHelper}.
 * Read operations use the inherited Spring Data JPA repository methods for scalar
 * lookups, and the Holon {@link Datastore} for lazy server-side streaming.</p>
 *
 * <p>{@code @Transactional(readOnly = true)} is set at class level; individual
 * write methods override it with a read-write transaction.</p>
 */
@Service
@Transactional(readOnly = true)
public class <Entity/>Service {

    private static final Logger log = LoggerFactory.getLogger(<Entity>Service.class);

    // ── CRUD delegate + streaming datastore ───────────────────────────────────
    private final BeanDatastoreHelper<<Entity>> helper;
    private final <Entity>Repository repository;
    private final Datastore datastore;  // kept for lazy streaming queries

    public <Entity>Service(Datastore datastore, <Entity>Repository repository) {
        this.datastore  = datastore;
        this.helper     = BeanDatastoreHelper.of(datastore, <Entity>.class);
        this.repository = repository;
    }

    // ── Read operations (JPA repository — scalar / paginated) ─────────────────

    public Optional<<Entity>> findById(Long id) {
        return repository.findById(id);
    }

    public List<<Entity>> findAll() {
        return repository.findAll();
    }

    public Page<<Entity>> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public long count() {
        return repository.count();
    }

    // ── Streaming read operations (Holon Datastore — lazy, server-side) ───────

    /**
     * Stream all entities as {@link PropertyBox} objects using a lazy server-side
     * cursor. The stream is backed by the Holon Datastore and does NOT load the full
     * result set into heap — safe for large tables.
     *
     * <p><strong>Lazy-loading rules:</strong>
     * <ul>
     *   <li>Call this method only inside an active {@code @Transactional} context
     *       so the JPA session stays open for the lifetime of the stream.</li>
     *   <li>Do <em>not</em> access Lombok-generated lazy collections
     *       (e.g. {@code customer.getContacts()}) on entities returned by this stream
     *       outside of the originating transaction — that triggers a
     *       {@code LazyInitializationException}.</li>
     *   <li>If the caller needs lazy associations, use
     *       {@link #findAll()} or {@link #findAll(Pageable)} instead — those return
     *       fully managed JPA entities within the transaction boundary.</li>
     * </ul>
     *
     * @return a lazy {@code Stream<PropertyBox>} — close it (or use try-with-resources)
     *         to release the underlying cursor
     */
    public Stream<PropertyBox> streamAll() {
        return datastore.query(<Entity>Model.TARGET)
                .stream(<Entity>Model.PROPERTY_SET);
    }

    /**
     * Stream entities filtered and sorted by the caller's criteria.
     * Uses a lazy server-side cursor — same lazy-loading rules as {@link #streamAll()}.
     *
     * <p>Example usage:
     * <pre>{@code
     * try (Stream<PropertyBox> s = service.stream(
     *         <Entity>Model.ACTIVE.eq(true),
     *         <Entity>Model.NAME.asc())) {
     *     s.forEach(box -> ...);
     * }
     * }</pre>
     *
     * @param filter a Holon {@code QueryFilter} (e.g. {@code <Entity>Model.FIELD.eq(value)})
     * @param sort   a Holon {@code QuerySort}   (e.g. {@code <Entity>Model.FIELD.asc()})
     * @return a lazy {@code Stream<PropertyBox>}
     */
    public Stream<PropertyBox> stream(
            com.holonplatform.core.query.QueryFilter filter,
            com.holonplatform.core.query.QuerySort sort) {
        return datastore.query(<Entity>Model.TARGET)
                .filter(filter)
                .sort(sort)
                .stream(<Entity>Model.PROPERTY_SET);
    }

    // ── Write operations ──────────────────────────────────────────────────────

    @Transactional
    public <Entity> save(<Entity> entity) {
        log.info("Saving <entity>: {}", entity);
        return helper.save(entity).getResult().orElse(entity);
    }

    @Transactional
    public void delete(<Entity> entity) {
        log.info("Deleting <entity> id={}", entity.getId());
        helper.delete(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting <entity> id={}", id);
        findById(id).ifPresent(helper::delete);
    }
}
```

---

## Step 3 — Logging the meaningful field

Replace the generic `entity.toString()` in log statements with the entity's most descriptive
field (usually `name`, `code`, `title`, `number`, or similar). For example:

- `Customer` → `log.info("Saving customer: {}", customer.getName())`
- `Invoice` → `log.info("Saving invoice: {}", invoice.getInvoiceNumber())`
- `Product` → `log.info("Saving product: {}", product.getName())`

If no obvious descriptor exists, use the class name and id: `"{} id={}", entity.getClass().getSimpleName(), entity.getId()`.

---

## Step 4 — Compile gate

Run:
```
./mvnw compile
```
Fix every error before declaring the entity done.

**Common errors and fixes:**

| Error | Fix |
|---|---|
| `cannot find symbol: BeanDatastoreHelper` | Check import — use `com.holonplatform.core.datastore.beans.BeanDatastoreHelper` |
| `cannot find symbol: Datastore` | Import `com.holonplatform.core.datastore.Datastore` (not Spring's `DataSource`) |
| `No qualifying bean of type 'Datastore'` | Holon's auto-configuration registers the bean — ensure `holon-starter-vaadin-flow-saas` is on the classpath |
| `findAll(Pageable)` not found | Import `org.springframework.data.domain.Pageable` |

---

## Lazy-loading and Streaming

### When to use which read method

| Method | Backing API | Loads into heap? | Returns | Use when |
|---|---|---|---|---|
| `findById(id)` | JPA repo | single row | `Optional<Entity>` | looking up one record |
| `findAll()` | JPA repo | **all rows** | `List<Entity>` | small tables only |
| `findAll(Pageable)` | JPA repo | one page | `Page<Entity>` | paginated grids |
| `streamAll()` | Holon Datastore | **lazy cursor** | `Stream<Entity>` | large tables, exports, batch |
| `stream(filter, sort)` | Holon Datastore | **lazy cursor** | `Stream<Entity>` | filtered bulk reads |

> **`PropertyBox` is never returned from service methods.**
> `BeanProjection.of(Entity.class)` is used instead — the Holon Datastore maps each row
> directly into the JPA bean so callers always work with typed entity instances.

### Lazy-loading rules (must follow — never violate)

1. **Keep the transaction alive** for the full lifetime of the stream.
   Annotate the *calling* method with `@Transactional(readOnly = true)`.

   ```java
   // ✅ correct — caller opens a transaction that outlives the stream
   @Transactional(readOnly = true)
   public void exportActiveCustomers(OutputStream out) {
       try (Stream<Customer> s = customerService.stream(
               CustomerModel.ACTIVE.eq(true), CustomerModel.NAME.asc())) {
           s.forEach(c -> writeRow(out, c));
       }
   }
   ```

2. **Lazy JPA associations on streamed beans** (e.g. `customer.getContacts()`) can be
   accessed **only while inside the active transaction** opened by the caller. Never
   pass a streamed entity to a detached context (async thread, response DTO built after
   the transaction closes).

3. **Always close the stream** — use try-with-resources.
   `BeanProjection` is backed by a Holon cursor that holds a JDBC connection.

   ```java
   // ✅ cursor closed automatically
   try (Stream<Customer> s = service.streamAll()) {
       s.forEach(c -> process(c));
   }
   ```

4. **`findAll()` on large tables is forbidden** — use `findAll(Pageable)` or `streamAll()`.

### Streaming with a filter/sort

```java
try (Stream<Customer> s = customerService.stream(
        CustomerModel.ACTIVE.eq(true)
            .and(CustomerModel.TIER.in(Customer.Tier.GOLD, Customer.Tier.PLATINUM)),
        CustomerModel.NAME.asc())) {

    s.forEach(customer -> {
        // customer is a real Customer JPA bean — all scalar fields populated
        process(customer.getName(), customer.getTier());
    });
}
```

---

## Rules and constraints

- **Zero custom methods in the repository** — if a query cannot be expressed with the inherited
  JPA repository methods, add a method to the *service* using `helper.getDatastore()` +
  `<Entity>Model.TARGET` + `BeanProjection.of(<Entity>.class)`.
- **All writes go through `BeanDatastoreHelper`** — never call `repository.save()` for writes.
- **`BeanProjection.of(Entity.class)` is always used for streaming** — never return `PropertyBox`
  from service methods; callers always receive typed entity instances.
- **`@Transactional(readOnly = true)`** at class level, **`@Transactional`** on each write method.
- **One `*Repository` + one `*Service` per entity**, both in `<entity-package>.domain`.
- **Log every write** at `INFO` level with a meaningful field (name, number, code…).
- **No separate `Datastore` field** — use `helper.getDatastore()` to keep the constructor lean.
- **`findAll()` on large tables is forbidden** — use `findAll(Pageable)` or `streamAll()`.
- **Streaming callers must be `@Transactional`** — the JPA session must be open for the duration.
- **Always close streams** — try-with-resources; the cursor holds a JDBC connection.

---

## Adding custom queries to the service (when needed)

When a use case requires a query that `findAll()` cannot cover, add it directly to the service
using the `*Model` interface. **Never add it to the repository.**

```java
// Example: find active customers in a given tier — add this to CustomerService
public List<Customer> findActiveByTier(Customer.Tier tier) {
    return repository.findAll().stream()
        .filter(c -> c.isActive() && tier.equals(c.getTier()))
        .toList();
}

// Or using the Holon Datastore for server-side filtering (preferred for large data sets):
// Inject Datastore separately and query via CustomerModel.PROPERTY_SET
public List<PropertyBox> findActiveByTierBoxes(Customer.Tier tier) {
    return datastore.query(CustomerModel.TARGET)
        .filter(CustomerModel.ACTIVE.eq(true).and(CustomerModel.TIER.eq(tier)))
        .sort(CustomerModel.NAME.asc())
        .stream(CustomerModel.PROPERTY_SET)
        .collect(Collectors.toList());
}
```

---

## Full example: `Customer`

### `CustomerRepository.java`
```java
package com.iyensoft.crm.domain;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // intentionally empty
}
```

### `CustomerService.java` (key methods)
```java
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final BeanDatastoreHelper<Customer> helper;
    private final CustomerRepository repository;

    public CustomerService(Datastore datastore, CustomerRepository repository) {
        this.helper     = BeanDatastoreHelper.of(datastore, Customer.class);  // com.holonplatform.core.datastore.beans
        this.repository = repository;
    }

    // Streaming — BeanProjection maps rows to Customer beans; never returns PropertyBox
    // import: com.holonplatform.core.query.BeanProjection
    public Stream<Customer> streamAll() {
        return helper.getDatastore()
                .query(CustomerModel.TARGET)
                .stream(BeanProjection.of(Customer.class));
    }

    public Stream<Customer> stream(QueryFilter filter, QuerySort sort) {
        return helper.getDatastore()
                .query(CustomerModel.TARGET)
                .filter(filter)
                .sort(sort)
                .stream(BeanProjection.of(Customer.class));
    }

    @Transactional
    public Customer save(Customer customer) {
        log.info("Saving customer: {}", customer.getName());
        return helper.save(customer).getResult().orElse(customer);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting customer id={}", id);
        findById(id).ifPresent(helper::delete);
    }
}
```




