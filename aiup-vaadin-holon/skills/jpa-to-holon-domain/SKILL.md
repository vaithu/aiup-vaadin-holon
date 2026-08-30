---
name: jpa-to-holon-domain
description: >
  Convert a Spring JPA entity into a full Holon Platform domain layer in one step:
  annotates the entity with Holon meta-annotations and Jakarta Validation constraints,
  creates the BeanPropertySet companion model interface, generates I18N resource bundles,
  and produces a Spring Data JPA Repository (marker only) plus a Holon BeanDatastoreHelper
  Service with paginated reads and lazy streaming. Use when asked to "add the domain layer",
  "create a repository and service", "bridge JPA with Holon", "add property set", or
  "prepare an entity for Holon Datastore / PropertyInputForm / PropertyListing".
argument-hint: "[EntityName or package path]"
---

# JPA → Holon Platform Domain Layer

Convert the JPA entity (or entities) identified by $ARGUMENTS into a complete Holon Platform
domain layer. Each entity produces **five artefacts** in a single pass:

1. **The entity itself** — annotated with `@Caption` (I18N), `@Ignore`, and Jakarta Validation
   constraints (`@NotBlank`, `@NotNull`, `@Size`).
2. **`<Entity>Model`** — a companion interface in the `model` sub-package holding the
   `BeanPropertySet<T>`, typed `PathProperty<V>` constants, and named sub-sets for listing
   and form usage.
3. **Resource bundles** — `messages.properties` (caption labels) and
   `ValidationMessages.properties` (validation error texts) under `src/main/resources/`.
4. **`<Entity>Repository`** — a Spring Data JPA repository with **no custom methods**.
5. **`<Entity>Service`** — a Spring `@Service` that uses `BeanDatastoreHelper<T>` for all
   writes, the repository for paginated/scalar reads, and a lazy Holon Datastore cursor for
   streaming.

---

## Background: How Holon bridges JPA automatically

`holon-jpa-bean-processors` (pulled in transitively by `holon-starter-vaadin-flow-saas`) registers
`BeanPropertyPostProcessor` implementations that read JPA annotations at introspection time:

| JPA annotation | Holon effect |
|---|---|
| `@Id` | property marked as **identifier** |
| `@Column(nullable = false)` | property marked as **required / not-null** |
| `@Column(updatable = false)` | property marked as **read-only** |
| `@Column(name = "...")` | column name registered as property path |
| `@Transient` | property **excluded** from the BeanPropertySet |
| `@OneToMany`, `@ManyToMany` | collection — add `@Ignore` explicitly |
| `@ManyToOne`, `@OneToOne` | FK reference — add `@Ignore`; expose FK id separately |
| `@Embedded` / `@Embeddable` | nested bean — Holon flattens to `<field>.<nestedField>` paths |
| `@EnumType.STRING` | **migrate to lookup table** — replace the enum field with a `Long` FK (`<field>Id`) referencing a new `<field>` lookup table; create a lookup bean + model + Flyway migration |

---

## Package layout

```
com.example.<module>/
  <Entity>.java                   ← JPA entity (already exists — annotated in Step 1)
  model/
    <Entity>Model.java            ← NEW: Holon property model (Step 2)
  domain/
    <Entity>Repository.java       ← NEW: empty Spring Data JPA repository (Step 4)
    <Entity>Service.java          ← NEW: Holon BeanDatastoreHelper service (Step 5)
src/main/resources/
  messages.properties             ← NEW/appended: caption labels (Step 3)
  ValidationMessages.properties   ← NEW/appended: validation errors (Step 3)
```

---

## Step 1 — Read and annotate the entity

### 1a. Catalogue the entity fields

Build a mental table before touching anything:

| Field | JPA type | Holon mapping | Action needed |
|---|---|---|---|
| `id` (from `BaseEntity`) | `@Id Long` | identifier, read-only | none |
| `createdAt`, `updatedAt` | `@Column(updatable=false)` | read-only | none |
| `version` | `@Version` | read-only | none |
| scalar String | `@Column(length=N, nullable=false)` | mapped | `@NotBlank` + `@Size(max=N)` + `@Caption` |
| scalar String | `@Column(length=N)` | mapped | `@Size(max=N)` + `@Caption` |
| non-String scalar | `@Column(nullable=false)` | mapped | `@NotNull` + `@Caption` |
| enum | `@Enumerated(STRING)` | **migrate to lookup table** | Replace field with `Long <field>Id`; add `@NotNull` + `@Caption`; create lookup bean + Flyway migration |
| `@Embedded` value object | `@Embedded` | nested bean paths | `@Caption` on nested fields inside `@Embeddable` |
| `@ManyToOne` / `@OneToOne` | FK reference | **exclude** | `@Ignore` on the object field |
| `@OneToMany` / `@ManyToMany` | collection | **exclude** | `@Ignore` on the collection field |

### 1b. Add `@Caption` with I18N `messageCode`

```java
import com.holonplatform.core.i18n.Caption;

// Pattern: @Caption(value = "Human Label", messageCode = "<module>.<entity>.<fieldName>.caption")
@Caption(value = "Account ID", messageCode = "crm.customer.accountId.caption")
@Column(name = "account_id", ...)
private String accountId;
```

**messageCode naming convention:** `<module>.<entity>.<fieldName>.caption`
- `module` = the Java sub-package name (e.g. `crm`, `inventory`, `finance`)
- `entity` = lower-case entity class name
- `fieldName` = camelCase field name

For `@Embedded` sub-objects, annotate the **nested fields** inside the `@Embeddable` class
with their own `messageCode`, e.g. `common.address.street.caption`. Do not add `@Caption`
to the embedding field in the parent entity.

### 1c. Add Jakarta Validation annotations

**Annotation order on each field:** `@NotBlank` / `@NotNull` → `@Size` → `@Caption` → JPA annotations.

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// String, nullable=false → @NotBlank + @Size if length declared
@NotBlank(message = "{crm.customer.accountId.notBlank}")
@Size(max = 20, message = "{crm.customer.accountId.size}")
@Caption(value = "Account ID", messageCode = "crm.customer.accountId.caption")
@Column(name = "account_id", length = 20, nullable = false)
private String accountId;

// String, nullable=true + length declared → @Size only
@Size(max = 30, message = "{crm.customer.taxId.size}")
@Caption(value = "Tax ID", messageCode = "crm.customer.taxId.caption")
@Column(name = "tax_id", length = 30)
private String taxId;

// Non-String, nullable=false → @NotNull
@NotNull(message = "{crm.customer.typeId.notNull}")
@Caption(value = "Type", messageCode = "crm.customer.typeId.caption")
@Column(name = "type_id", nullable = false)
private Long typeId;    // FK to customer_type lookup table — enum migrated to lookup table
```

**Validation message key format:** `{<module>.<entity>.<fieldName>.<constraintType>}`
The `{...}` braces are required — Jakarta Validation looks them up in `ValidationMessages.properties`.
`constraintType` = `notBlank` | `notNull` | `size`.

### 1d. Add `@Ignore` to excluded fields

```java
import com.holonplatform.core.beans.Ignore;

@Ignore
@OneToMany(...)
private List<Child> children = new ArrayList<>();

@Ignore
@ManyToOne(...)
@JoinColumn(name = "owner_id")
private Employee owner;
```

**Never remove JPA annotations** — the ORM layer must continue to work.

---

## Step 2 — Create the `*Model` interface

**Path:** `<entity-package>/model/<EntityName>Model.java`

```java
package <entity.package>.model;

import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertySet;
// + type imports (BigDecimal, LocalDate, Instant, Long for lookup FKs...)

@SuppressWarnings("rawtypes")
public interface <EntityName>Model {

    /** Datastore target — matches @Table(name="...") on the entity */
    DataTarget<String> TARGET = DataTarget.named("<table_name>");

    /** Full BeanPropertySet introspected from the annotated entity bean. */
    BeanPropertySet<<EntityName>> PROPERTY_SET = BeanPropertySet.create(<EntityName>.class);

    // ── BaseEntity ────────────────────────────────────────────────────────────
    PathProperty<Long>    ID         = PROPERTY_SET.property("id",        Long.class);
    PathProperty<Instant> CREATED_AT = PROPERTY_SET.property("createdAt", Instant.class);
    PathProperty<Instant> UPDATED_AT = PROPERTY_SET.property("updatedAt", Instant.class);

    // ── <section> ─────────────────────────────────────────────────────────────
    PathProperty<<Type>> <CONSTANT> = PROPERTY_SET.property("<fieldName>", <Type>.class);
    // ... one per mapped field (SCREAMING_SNAKE_CASE matching the field name)

    // ── Sub-sets ──────────────────────────────────────────────────────────────

    /** Grid columns (≤ 6–8 for readability). */
    PropertySet LISTING = PropertySet.builderOf(ID, <F1>, <F2>, ...).build();

    /** Create / edit form fields — excludes read-only audit columns. */
    PropertySet FORM = PropertySet.builderOf(<F1>, <F2>, ...).build();
}
```

**Constant naming:** SCREAMING_SNAKE_CASE matching the field name (`accountId` → `ACCOUNT_ID`).
**Embedded paths:** use dot notation: `PROPERTY_SET.property("billingAddress.city", String.class)`.
**Sub-sets:** use raw `PropertySet` (no type parameter).

---

## Step 3 — Create / update the resource bundles

Both files live at `src/main/resources/`. **Append** new entries — never overwrite existing ones.

### `messages.properties`

```properties
# ── Customer ──────────────────────────────────────────────────────────────────
crm.customer.accountId.caption=Account ID
crm.customer.name.caption=Name
crm.customer.type.caption=Type
```

Add the following to `application.properties` once (idempotent):
```properties
spring.messages.basename=messages
```

### `ValidationMessages.properties`

```properties
# ── Customer validation ───────────────────────────────────────────────────────
crm.customer.accountId.notBlank=Account ID is required
crm.customer.accountId.size=Account ID must be at most 20 characters
crm.customer.name.notBlank=Name is required
crm.customer.type.notNull=Type is required
```

---

## Step 4 — Create the Repository

The repository **must have zero custom methods**. All data-access logic lives in the service.

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
 * {@code BeanDatastoreHelper} for writes and inherited repository methods for reads.
 * Adding query methods here is explicitly forbidden — use {@code Datastore} +
 * {@code <Entity>Model.PROPERTY_SET} in the service instead.
 */
@Repository
public interface <Entity>Repository extends JpaRepository<<Entity>, Long> {
    // intentionally empty — see <Entity>Service for all data-access logic
}
```

---

## Step 5 — Create the Service

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
 * Paginated / scalar reads delegate to the Spring Data JPA repository.
 * Streaming reads use a lazy Holon Datastore cursor via {@code BeanProjection} —
 * they never return {@code PropertyBox}.
 *
 * <p>{@code @Transactional(readOnly = true)} at class level; write methods override
 * with a full read-write transaction.
 */
@Service
@Transactional(readOnly = true)
public class <Entity>Service {

    private static final Logger log = LoggerFactory.getLogger(<Entity>Service.class);

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
     *   <li>The caller must be inside an active {@code @Transactional} context.</li>
     *   <li>Always close the stream with try-with-resources — the cursor holds a
     *       database connection until closed.</li>
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
     * Same lazy-loading rules as {@link #streamAll()}.
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
        log.info("Saving <entity>: {}", entity.<descriptiveField>());
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

**Log field:** replace `<descriptiveField>()` with the most readable field (`getName()`,
`getCode()`, `getTitle()`, etc.). If none exists, use `entity.getId()`.

---

## Step 6 — Compile gate

Run:
```
./mvnw compile
```
Fix **every** compile error before moving to the next entity.

| Common error | Fix |
|---|---|
| `cannot find symbol: BeanDatastoreHelper` | Import `com.holonplatform.core.datastore.beans.BeanDatastoreHelper` |
| `cannot find symbol: Datastore` | Import `com.holonplatform.core.datastore.Datastore` (not Spring's `DataSource`) |
| `No qualifying bean of type 'Datastore'` | Ensure `holon-starter-vaadin-flow-saas` is on the classpath |
| `findAll(Pageable)` not found | Import `org.springframework.data.domain.Pageable` |

---

## Step 7 — Repeat for `@Embeddable` classes

For embedded value objects (like `Address`, `Money`):
- Add `@Caption` and `@Size` / `@NotBlank` to each field inside the `@Embeddable`
- Add the new keys to both resource bundles
- The embedding entity needs **no changes** — Holon flattens the nested bean automatically

---

## Lazy-loading and streaming rules (must follow — never violate)

| Method | Backing API | Loads into heap? | Returns | Use when |
|---|---|---|---|---|
| `findById(id)` | JPA repo | single row | `Optional<Entity>` | looking up one record |
| `findAll()` | JPA repo | **all rows** | `List<Entity>` | small tables only |
| `findAll(Pageable)` | JPA repo | one page | `Page<Entity>` | paginated grids |
| `streamAll()` | Holon Datastore | **lazy cursor** | `Stream<Entity>` | large tables, exports |
| `stream(filter, sort)` | Holon Datastore | **lazy cursor** | `Stream<Entity>` | filtered bulk reads |

1. **Keep the transaction alive** for the full lifetime of the stream — annotate the *calling*
   method with `@Transactional(readOnly = true)`.
2. **Always close streams** — use try-with-resources; the cursor holds a database connection.
3. **`findAll()` on large tables is forbidden** — use `findAll(Pageable)` or `streamAll()`.
4. **`PropertyBox` is never returned from service methods** — `BeanProjection.of(Entity.class)`
   maps each row directly to a typed entity instance.

---

## Adding custom queries to the service (when needed)

When a use case requires a query that the inherited repository methods cannot cover, add it
directly to the service using the `*Model` interface. **Never add custom methods to the repository.**

```java
// Server-side filtered query using the Holon Datastore (preferred for large data sets)
// Return Stream<T> — lazy cursor; caller (e.g. a UI fetch callback) must close it.
public Stream<Customer> findActiveByTier(Customer.Tier tier) {
    return stream(
        CustomerModel.ACTIVE.eq(true).and(CustomerModel.TIER.eq(tier)),
        CustomerModel.NAME.asc());
}
```

---

## Constraints

- **Never remove or change JPA annotations** — the JPA persistence layer must keep working.
- **Zero custom methods in the repository** — all query logic lives in the service.
- **All writes go through `BeanDatastoreHelper`** — never call `repository.save()` for writes.
- **One `*Model` interface** per entity, in `<entity-package>.model`.
- **One `*Repository` + one `*Service`** per entity, both in `<entity-package>.domain`.
- **`@Transactional(readOnly = true)`** at class level; **`@Transactional`** on each write method.
- **Log every write** at `INFO` level with a meaningful field (name, number, code…).
- **No separate `Datastore` field** — use `helper.getDatastore()` to keep the constructor lean.
- **Append** to resource bundles — never overwrite entries from other entities.
- **Compile gate** — build must be clean after every entity conversion.
- **Never name a business field `version`** — `BaseEntity` declares `@Version Long version` and
  Lombok will generate a conflicting `getVersion(): Long`. Use descriptive names like
  `revisionLabel` or `schemaVersion` for business-version strings.
- **Primitives** (`boolean active`) cannot be null — no `@NotNull` needed; skip validation.
- **`BaseEntity` fields** (`id`, `createdAt`, `updatedAt`, `version`) — no `@Caption` or validation.
- **FK-object references** (`@ManyToOne`) — `@Ignore` only; expose the FK `Long` id via a separate
  `PathProperty<Long>` constant in the model if Datastore filtering by FK is needed.

---

## Full example: `Customer`

### Annotated field (order: validation → @Caption → JPA):
```java
@NotBlank(message = "{crm.customer.accountId.notBlank}")
@Size(max = 20, message = "{crm.customer.accountId.size}")
@Caption(value = "Account ID", messageCode = "crm.customer.accountId.caption")
@Column(name = "account_id", length = 20, nullable = false, unique = true, updatable = false)
private String accountId;
```

### `CustomerModel.java`
```java
package com.iyensoft.crm.model;

@SuppressWarnings("rawtypes")
public interface CustomerModel {
    DataTarget<String>      TARGET      = DataTarget.named("customer");
    BeanPropertySet<Customer> PROPERTY_SET = BeanPropertySet.create(Customer.class);
    PathProperty<Long>      ID          = PROPERTY_SET.property("id",        Long.class);
    PathProperty<String>    ACCOUNT_ID  = PROPERTY_SET.property("accountId", String.class);
    PathProperty<String>    NAME        = PROPERTY_SET.property("name",      String.class);
    PathProperty<CustomerType> TYPE     = PROPERTY_SET.property("type",      CustomerType.class);
    PropertySet LISTING = PropertySet.builderOf(ID, ACCOUNT_ID, NAME, TYPE).build();
    PropertySet FORM    = PropertySet.builderOf(ACCOUNT_ID, NAME, TYPE).build();
}
```

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
package com.iyensoft.crm.domain;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final BeanDatastoreHelper<Customer> helper;
    private final CustomerRepository repository;

    public CustomerService(Datastore datastore, CustomerRepository repository) {
        this.helper     = BeanDatastoreHelper.of(datastore, Customer.class);
        this.repository = repository;
    }

    public Stream<Customer> streamAll() {
        return helper.getDatastore()
                .query(CustomerModel.TARGET)
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

### `messages.properties` (append)
```properties
crm.customer.accountId.caption=Account ID
crm.customer.name.caption=Name
crm.customer.type.caption=Type
```

### `ValidationMessages.properties` (append)
```properties
crm.customer.accountId.notBlank=Account ID is required
crm.customer.accountId.size=Account ID must be at most 20 characters
crm.customer.name.notBlank=Name is required
crm.customer.type.notNull=Type is required
```

---

## Type and import reference

| Type / Annotation | Package | Purpose |
|---|---|---|
| `@Caption(value, messageCode)` | `com.holonplatform.core.i18n` | UI label + I18N key |
| `@Ignore` | `com.holonplatform.core.beans` | Exclude field from BeanPropertySet |
| `@NotBlank(message)` | `jakarta.validation.constraints` | Non-null, non-empty String |
| `@NotNull(message)` | `jakarta.validation.constraints` | Non-null value |
| `@Size(max, message)` | `jakarta.validation.constraints` | Max string length |
| `BeanPropertySet<T>` | `com.holonplatform.core.beans` | PropertySet from bean introspection |
| `PathProperty<V>` | `com.holonplatform.core.property` | Typed property reference |
| `PropertySet` (raw) | `com.holonplatform.core.property` | For LISTING / FORM sub-sets |
| `DataTarget` | `com.holonplatform.core.datastore` | Datastore target (table name) |
| `BeanDatastoreHelper<T>` | `com.holonplatform.core.datastore.beans` | Holon write delegate |
| `BeanProjection` | `com.holonplatform.core.query` | Maps Datastore rows to bean instances |
| `QueryFilter` | `com.holonplatform.core.query` | Typed filter for streaming queries |
| `QuerySort` | `com.holonplatform.core.query` | Typed sort for streaming queries |
