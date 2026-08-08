---
name: jpa-to-holon-bean-properties
description: Convert Spring JPA entities into Holon Platform BeanPropertySet models with typed property constants. Use when asked to bridge JPA entities with Holon's property model, create property sets, or prepare entities for use with Holon Datastore / PropertyInputForm / PropertyListing.
argument-hint: "[EntityName or package path]"
---

# JPA → Holon Platform Bean Properties

Convert the JPA entity (or entities) identified by $ARGUMENTS into Holon Platform property models.
Each entity produces **three artefacts**:

1. **The entity itself** — minimally annotated with Holon meta-annotations (`@Caption` with I18N
   `messageCode`, `@Ignore`) and Jakarta Validation constraints (`@NotBlank`, `@NotNull`, `@Size`).
2. **A companion `*Model` interface** — lives in the `model` sub-package of the entity's package.
   Holds the `BeanPropertySet<T>`, one `PathProperty<V>` constant per mapped field, and named
   sub-sets for listing and form usage.
3. **Resource bundles** — `messages.properties` for caption labels and
   `ValidationMessages.properties` for validation error texts, both under
   `src/main/resources/`.

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
| `@OneToMany`, `@ManyToMany` | collection — should add `@Ignore` explicitly |
| `@ManyToOne`, `@OneToOne` | FK reference — add `@Ignore`; expose FK id separately |
| `@Embedded` / `@Embeddable` | nested bean — Holon flattens to `<field>.<nestedField>` paths |
| `@EnumType.STRING` | enum value serialised/deserialised as its name string |

---

## Step-by-step process

Work entity by entity. For each entity do **all six steps in order**.

---

### Step 1 — Read and catalogue the entity

Read the entity source file. Build a mental table:

| Field | JPA type | Holon mapping | Action needed |
|---|---|---|---|
| `id` (from `BaseEntity`) | `@Id Long` | identifier, read-only | none |
| `createdAt`, `updatedAt` | `@Column(updatable=false)` | read-only | none |
| `version` | `@Version` | read-only | none |
| scalar String | `@Column(length=N, nullable=false)` | mapped | `@Caption` + `@NotBlank` + `@Size(max=N)` |
| scalar String | `@Column(length=N)` | mapped | `@Caption` + `@Size(max=N)` |
| non-String scalar | `@Column(nullable=false)` | mapped | `@Caption` + `@NotNull` |
| enum | `@Enumerated(STRING)` | enum property | `@Caption` + `@NotNull` if nullable=false |
| `@Embedded` value object | `@Embedded` | nested bean paths | `@Caption` to nested fields inside `@Embeddable` |
| `@ManyToOne` / `@OneToOne` | FK reference | **exclude** | `@Ignore` on the object field |
| `@OneToMany` / `@ManyToMany` | collection | **exclude** | `@Ignore` on the collection field |

---

### Step 2 — Annotate the entity

#### 2a. Add `@Caption` with I18N `messageCode`

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

**Important:** For `@Embedded` sub-objects, annotate the **nested fields** inside the
`@Embeddable` class with their own `messageCode`, e.g. `common.address.street.caption`.
Do not add `@Caption` to the embedding field in the parent entity.

#### 2b. Add Jakarta Validation annotations

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// String, nullable=false → @NotBlank + @Size if length is declared
@NotBlank(message = "{crm.customer.accountId.notBlank}")
@Size(max = 20, message = "{crm.customer.accountId.size}")
@Caption(value = "Account ID", messageCode = "crm.customer.accountId.caption")
@Column(name = "account_id", length = 20, nullable = false, ...)
private String accountId;

// String, nullable=true + length declared → @Size only
@Size(max = 30, message = "{crm.customer.taxId.size}")
@Caption(value = "Tax ID", messageCode = "crm.customer.taxId.caption")
@Column(name = "tax_id", length = 30)
private String taxId;

// Non-String, nullable=false → @NotNull
@NotNull(message = "{crm.customer.type.notNull}")
@Caption(value = "Type", messageCode = "crm.customer.type.caption")
@Enumerated(EnumType.STRING)
@Column(name = "type", nullable = false)
private CustomerType type;
```

**Validation message key format:** `{<module>.<entity>.<fieldName>.<constraintType>}`
- The `{...}` braces are required — Jakarta Validation looks them up in `ValidationMessages.properties`
- `constraintType` = `notBlank` | `notNull` | `size`

#### 2c. Add `@Ignore` to excluded fields

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

### Step 3 — Create the `*Model` interface in the `model` sub-package

**Path:** `<entity-package>/model/<EntityName>Model.java`

Example: `Customer` is in `com.iyensoft.crm` → model goes to
`com.iyensoft.crm.model.CustomerModel`.

```java
package <entity.package>.model;

import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertySet;
// + type imports (BigDecimal, LocalDate, Instant, enums...)

@SuppressWarnings("rawtypes")
public interface <EntityName>Model {

    /** Datastore target — @Table(name="...") value */
    DataTarget<String> TARGET = DataTarget.named("<table_name>");

    /** Full BeanPropertySet introspected from the annotated entity bean. */
    BeanPropertySet<<EntityName>> PROPERTY_SET = BeanPropertySet.create(<EntityName>.class);

    // ── BaseEntity ────────────────────────────────────────────────────────────
    PathProperty<Long>    ID         = PROPERTY_SET.property("id",        Long.class);
    PathProperty<Instant> CREATED_AT = PROPERTY_SET.property("createdAt", Instant.class);
    PathProperty<Instant> UPDATED_AT = PROPERTY_SET.property("updatedAt", Instant.class);

    // ── <section> ─────────────────────────────────────────────────────────────
    PathProperty<<Type>> <CONSTANT> = PROPERTY_SET.property("<fieldName>", <Type>.class);
    // ... one per mapped field

    // ── Sub-sets ──────────────────────────────────────────────────────────────

    /** Grid columns (≤ 6–8 for readability). */
    PropertySet LISTING = PropertySet.builderOf(ID, <F1>, <F2>, ...).build();

    /** Create / edit form fields — excludes read-only audit columns. */
    PropertySet FORM = PropertySet.builderOf(<F1>, <F2>, ...).build();
}
```

**Constant naming:** SCREAMING_SNAKE_CASE matching the field name (`accountId` → `ACCOUNT_ID`).
**Embedded paths:** use dot notation: `PROPERTY_SET.property("billingAddress.city", String.class)`.
**Sub-sets:** use raw `PropertySet` (no type parameter) — `PropertySet<T>` requires `T extends Property<?>`.

---

### Step 4 — Create / update the resource bundles

Both files live at the **classpath root**: `src/main/resources/`.

#### `messages.properties` — caption labels

```properties
# ── Customer ──────────────────────────────────────────────────────────────────
crm.customer.accountId.caption=Account ID
crm.customer.name.caption=Name
crm.customer.legalName.caption=Legal name
crm.customer.type.caption=Type
# ... one key per @Caption messageCode
```

Append new entity keys to the existing file (do **not** overwrite existing entries).

#### `ValidationMessages.properties` — validation errors

```properties
# ── Customer validation ───────────────────────────────────────────────────────
crm.customer.accountId.notBlank=Account ID is required
crm.customer.accountId.size=Account ID must be at most 20 characters
crm.customer.name.notBlank=Name is required
crm.customer.name.size=Name must be at most 200 characters
crm.customer.type.notNull=Type is required
# ... one key per @NotBlank / @NotNull / @Size message
```

Note: Jakarta Validation looks up `ValidationMessages.properties` on the classpath root
automatically — no extra configuration needed.

For `messages.properties`, add the following to `application.properties` once (idempotent):
```properties
spring.messages.basename=messages
```
This registers the bundle with Spring's `MessageSource`, which Holon's localization bridge
picks up automatically.

---

### Step 5 — Verify and iterate

Run:
```
./mvnw compile
```
Fix **every** compile error before moving to the next entity. Treat it as a gate — do not
declare the entity done until the build is clean.

---

### Step 6 — Repeat for `@Embeddable` classes

For embedded value objects (like `Address`, `Money`):
- Add `@Caption(value="...", messageCode="<module>.<embeddable>.<field>.caption")` to each field
- Add `@Size` / `@NotBlank` as appropriate
- Add the new keys to `messages.properties` and `ValidationMessages.properties`
- The embedding entity needs **no changes** — Holon flattens the nested bean automatically

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
| `PathProperty<V>` | `com.holonplatform.core.property` | Typed property ref — return type of `PROPERTY_SET.property()` |
| `PropertySet` (raw) | `com.holonplatform.core.property` | For LISTING / FORM sub-sets |
| `DataTarget` | `com.holonplatform.core.datastore` | Datastore target (table name) |

---

## Rules and constraints

- **Never remove or change JPA annotations** — the JPA persistence layer must keep working.
- **One `*Model` interface per entity**, placed in `<entity-package>.model`.
- **Validation placement order** on a field: `@NotBlank` / `@NotNull` → `@Size` → `@Caption` → JPA annotations.
- **Primitives** (e.g. `boolean active`) cannot be null — no `@NotNull` needed; skip validation.
- **`BaseEntity` fields** (`id`, `createdAt`, `updatedAt`, `version`) — no `@Caption` or validation; they are infrastructure fields.
- **Collections** (`@OneToMany`, `@ManyToMany`) — `@Ignore` only; no `@Caption` or validation.
- **FK-object references** (`@ManyToOne`) — `@Ignore` only; expose the FK `Long` id via a separate `PathProperty<Long>` if Datastore filtering by FK is needed.
- **Append** to resource bundles — never overwrite entries from other entities.
- **Compile gate** — the build must be clean after every entity conversion.
- **Never name a business field `version`** — `BaseEntity` declares `@Version Long version` and Lombok will generate a conflicting `getVersion(): Long`. Use descriptive names like `revisionLabel`, `amendmentLabel`, `schemaVersion`, etc. for business-version strings.

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

### Companion interface path:
`src/main/java/com/iyensoft/crm/model/CustomerModel.java`

### Resource bundle entry (`messages.properties`):
```properties
crm.customer.accountId.caption=Account ID
```

### Resource bundle entry (`ValidationMessages.properties`):
```properties
crm.customer.accountId.notBlank=Account ID is required
crm.customer.accountId.size=Account ID must be at most 20 characters
```

### Usage in a Vaadin view:
```java
// Datastore query using typed constants
datastore.query(CustomerModel.TARGET)
    .filter(CustomerModel.NAME.contains("Corp"))
    .sort(CustomerModel.NAME.asc())
    .stream(CustomerModel.PROPERTY_SET)
    .collect(Collectors.toList());

// Build a grid — column headers come from @Caption messageCode via LocalizationContext
Components.listing.properties(CustomerModel.LISTING)
    .dataSource(datastore, CustomerModel.TARGET)
    .build();

// Build a form — labels come from @Caption messageCode
Components.input.form(CustomerModel.FORM).build();
```
