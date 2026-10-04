---
name: jpa-to-holon-domain
description: >
  Convert a JPA entity into a full Holon Platform domain layer in one step: annotates the
  entity with Holon meta-annotations and Jakarta Validation constraints, creates the
  BeanPropertySet companion model interface, generates I18N resource bundles, a Holon
  BeanDatastoreHelper service, and a mapping test. Follows the project's own conventions
  (package by feature, audit base class, no repository by default). Use when asked to "add
  the domain layer", "create a service for an entity", "bridge JPA with Holon", "add
  property set", or "prepare an entity for Holon Datastore / EntityFormPanel / ListingBundle".
argument-hint: "[EntityName or package path]"
---

# JPA → Holon Platform Domain Layer

Convert the JPA entity (or entities) identified by $ARGUMENTS into a Holon Platform domain
layer. Each entity produces **four artefacts and one test** in a single pass:

1. **The entity** — annotated with `@Caption` (I18N) and Jakarta Validation constraints,
   extending the project's audit base class.
2. **`<Entity>Model`** — a companion interface **in the same package** holding the
   `BeanPropertySet<T>`, typed property constants, and named sub-sets for listing and form.
3. **Resource bundles** — keys appended to `messages.properties` (captions) and
   `ValidationMessages.properties` (validation texts) under `src/main/resources/`.
4. **`<Entity>Service`** — a Spring `@Service` that reads and writes only through
   `BeanDatastoreHelper<T>`.
5. **`<Entity>MappingTest`** — runs holon-saas's `HolonEntityMappingValidator` on the entity.

There is **no repository** by default (Step 4).

## The project decides, not this skill

Read these before generating; where they differ from this file, they win:

| Read | Decides |
|---|---|
| `docs/entity_model.md` | the attributes, types, lengths, which entities are tenant or platform (`**Schema:**`) |
| `docs/architecture/development.md` | package layout, naming, conventions, persistence notes |
| `rules/holon-stack.md` (this plugin) | allowed and banned imports, idioms, the holon-saas carve-out |
| `src/main/resources/db/**` | the real table and column names, sequence `INCREMENT BY` |

The entity is **generated from the entity model and the migrations**, not the other way
round. A column the entity maps must exist in the migration, with the same name and type.

## Background: how Holon bridges JPA

`holon-jpa-bean-processors` registers `BeanPropertyPostProcessor` implementations that read
JPA annotations when a bean is introspected:

| JPA annotation | Holon effect |
|---|---|
| `@Id` | property marked as **identifier** |
| `@Column(nullable = false)` | property marked **required** |
| `@Column(updatable = false)` | property marked **read-only** |
| `@Column(name = "...")` | column name registered as the property path |
| `@Transient` | **no effect on a scalar getter** — do not rely on it (holon-saas checklist section 2) |
| `@OneToMany`, `@ManyToMany` | collection — add `@Ignore`; **never** `@ElementCollection` |
| `@ManyToOne`, `@OneToOne` | FK reference — add `@Ignore`, `fetch = LAZY`; expose the FK id as a `Long` field |
| `@Enumerated` | **forbidden** — replace with a `Long <field>Id` FK to a lookup table |

## Package layout (package by feature)

```
com.<org>.<app>.<feature>/            e.g. customer/, inventory/, or shared/ for cross-feature entities
  <Entity>.java                       the JPA entity
  <Entity>Model.java                  Holon property model (Step 2)
  <Entity>Service.java                BeanDatastoreHelper service (Step 5)
com.<org>.<app>.shared/
  AuditedEntity.java                  audit base class, written once (Step 1)
src/main/resources/
  messages.properties                 captions (Step 3)
  ValidationMessages.properties       validation texts (Step 3)
src/test/java/<same package>/
  <Entity>MappingTest.java            mapping test (Step 6)
```

There are **no** `model/`, `domain/` or other layer sub-packages. An entity used by two or
more features and owned by neither goes in `shared`; keep `shared` small.

## Step 0 — JPA anti-pattern guardrail (blocking)

**Read [`../../rules/jpa-anti-patterns.md`](../../rules/jpa-anti-patterns.md) before writing
anything.**

- **Pass 1** — if an entity already exists, scan it against the Detection Table and report a
  findings list. Fix findings this skill rewrites anyway (enums → lookup FK, missing
  `@Version`, `LocalDateTime` → `Instant`); ask before touching anything else.
- **Pass 2** — before writing, scan what you are about to emit. Zero 🛑 findings; ⚠️ findings
  carry `// JPA-WAIVER(<id>): <justification>`.

Print the guardrail report in the final summary, or `✅ JPA anti-pattern guardrail: clean`.

## Step 1 — The entity and the audit base class

### 1a. The audit base class (once per project)

Every entity table carries `created_by`, `created_date`, `last_modified_by`,
`last_modified_date` and `version`. Put them in one `@MappedSuperclass`:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditedEntity implements Serializable {
    @CreatedBy      @Column(name = "created_by", length = 100, nullable = false, updatable = false) private String createdBy;
    @CreatedDate    @Column(name = "created_date", nullable = false, updatable = false)             private Instant createdDate;
    @LastModifiedBy @Column(name = "last_modified_by", length = 100)                                private String lastModifiedBy;
    @LastModifiedDate @Column(name = "last_modified_date")                                          private Instant lastModifiedDate;
    @Version        @Column(name = "version", nullable = false)                                     private Long version;
    // public getter and setter for every field
}
```

The auditing annotations come from `org.springframework.data.annotation` and
`AuditingEntityListener` from `org.springframework.data.jpa.domain.support`; the holon-saas
carve-out in `holon-stack.md` allows them in this class only. holon-saas fills them through its
`securityContextActorResolver`. Date-times are `Instant` and the columns are
`TIMESTAMP WITH TIME ZONE` (`JPA-008`).

### 1b. The entity

Rules, from the holon-saas checklist (section 2): a **public no-arg constructor**, a public
getter **and** setter for every persisted field, no `@ElementCollection`, no Lombok `@Data`
(`JPA-020`), `equals` on a business key and a constant `hashCode` (`JPA-021`), `toString()`
on scalar fields only.

```java
@Entity
@Table(name = "country")
public class Country extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "country_gen")
    @SequenceGenerator(name = "country_gen", sequenceName = "country_seq", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "{country.code.notBlank}")
    @Size(max = 30, message = "{country.code.size}")
    @Caption(value = "Code", messageCode = "country.code")
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;
    ...
    public Country() { super(); }
}
```

- **`allocationSize` equals the migration's `INCREMENT BY`** (`JPA-007`).
- **Annotation order on a field:** validation → `@Caption` → JPA.
- **Validation:** `@NotBlank` + `@Size` for a required String, `@Size` for an optional one,
  `@NotNull` for a required non-String. A primitive (`boolean`) needs no `@NotNull`.
- **Message codes follow `holon-stack.md`: `<domain>.<field>`** (`country.code`,
  `customer.legalName`), **not** `<module>.<entity>.<field>.caption`. Validation keys are
  `{<domain>.<field>.<notBlank|notNull|size>}`.
- **Categorical values** are a `Long <field>Id` FK to a lookup entity, never an enum.
- **Associations** use `@Ignore` and `fetch = FetchType.LAZY`; expose the FK id as a field.
- **Fields to skip:** `id` and the audit fields get no `@Caption` and no validation.
- **Never remove or change an existing JPA annotation.**

## Step 2 — The `*Model` interface

**Path:** the entity's own package.

```java
@SuppressWarnings("rawtypes")
public interface CountryModel {

    BeanPropertySet<Country> PROPERTY_SET = BeanPropertySet.create(Country.class);

    NumericProperty<Long> ID = PROPERTY_SET.propertyNumeric("id");
    StringProperty CODE = PROPERTY_SET.propertyString("code");
    NumericProperty<Integer> SORT_ORDER = PROPERTY_SET.propertyNumeric("sortOrder");
    BooleanProperty ACTIVE = PROPERTY_SET.propertyBoolean("active");

    /** Listing columns; carries the identifier, as the datastore requires. */
    PropertySet LISTING = PropertySet.builderOf(ID, CODE, SORT_ORDER, ACTIVE).withIdentifier(ID).build();
    /** Create and edit fields; no id and no audit columns. */
    PropertySet FORM = PropertySet.builderOf(CODE, SORT_ORDER, ACTIVE).build();
}
```

Use the typed constants (`NumericProperty`, `StringProperty`, `BooleanProperty`,
`TemporalProperty`). A `BooleanProperty` has `eq(true)`, **not** `isTrue()`. Constants are
`SCREAMING_SNAKE_CASE`; an embedded path is `"billingAddress.city"`. A `PropertySet` used with
the datastore declares `.withIdentifier(ID)`.

## Step 3 — Resource bundles

**Append** to `src/main/resources/messages.properties` and `ValidationMessages.properties`;
never overwrite another entity's keys.

```properties
# messages.properties
country.code=Code
country.sortOrder=Sort order
```
```properties
# ValidationMessages.properties
country.code.notBlank=Code is required
country.code.size=Code must be at most 30 characters
```

`spring.messages.basename=messages` belongs in `application.properties`; if that file does not
exist yet, say so in the report instead of creating a partial one.

## Step 4 — No repository

**Do not create a Spring Data repository.** All reads and writes go through
`BeanDatastoreHelper` (`holon-stack.md`: Spring Data is banned outside the holon-saas
carve-out). Create a plain `JpaRepository` only for an entity the Holon datastore cannot map
(holon-saas checklist section 6), with a `// FALLBACK: <reason>` comment, and never add a
method to it (`JPA-033`).

## Step 5 — The service

```java
@Service
public class CountryService {

    private final BeanDatastoreHelper<Country> helper;

    public CountryService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Country.class);
    }

    @Transactional(readOnly = true)
    public Stream<Country> findActive() {
        return helper.findAll(CountryModel.ACTIVE.eq(true), CountryModel.SORT_ORDER.asc());
    }

    @Transactional(readOnly = true)
    public Optional<Country> findByCode(String code) {
        return helper.findFirst(CountryModel.CODE.eq(code));
    }

    @Transactional
    public Country save(Country country) {
        return helper.save(country).getResult().orElse(country);
    }

    @Transactional
    public void delete(Country country) { helper.delete(country); }
}
```

Imports: `com.holonplatform.core.datastore.Datastore`,
`com.holonplatform.core.datastore.beans.BeanDatastore`,
`com.holonplatform.core.datastore.beans.BeanDatastoreHelper` (**not**
`com.holonplatform.core.beans.…`).

Rules:

- **Constructor injection only.** The `Datastore` is a Spring bean; never `Context.get()`.
- **Transaction boundary is the service method.** Reads `@Transactional(readOnly = true)`,
  writes `@Transactional`; no `@Transactional` on a view (`JPA-038`).
- **`BeanDatastoreHelper` has no sort-only `findAll`.** To sort without filtering, pass a
  filter every row passes: `findAll(Model.ID.isNotNull(), Model.NAME.asc())`.
- **A `Stream` is lazy**: the caller must be inside a transaction and close it with
  try-with-resources (`JPA-035`, `JPA-036`). Never `.toList()` in a UI fetch callback.
- **Never return `PropertyBox`.** Log every write at `INFO` with a readable field.
- **Writes go through `helper.save/insert/update/delete`**, never `repository.save()` (`JPA-040`).
- Hand-written queries use the `*Model` constants, not strings.

## Step 6 — The mapping test

```java
class CountryMappingTest {
    @Test
    void countryIsSafeForTheHolonDatastore() {
        HolonEntityMappingValidator.assertValid(Country.class);   // com.holonplatform.multitenant.testing
    }
    // plus: the property set contains every mapped field; LISTING has the identifier; FORM does not.
}
```

This is the build-time version of the holon-saas checklist: it catches a missing setter, a
non-public constructor or an `@ElementCollection` that would otherwise fail, or silently drop
rows, at runtime.

## Step 7 — Compile and test gate

```
mvn -q compile        # or ./mvnw compile when the project has a wrapper
mvn -q test
```

Fix every error before the next entity. Common ones:

| Error | Fix |
|---|---|
| `cannot find symbol: BeanDatastoreHelper` | import `com.holonplatform.core.datastore.beans.BeanDatastoreHelper` |
| `findAll(QuerySort)` not applicable | no sort-only overload; see Step 5 |
| `isTrue()` not found on `BooleanProperty` | use `eq(true)` |
| tests fail at start with `IllegalAccessError … KotlinReflectionUtils` | mixed JUnit versions; import `org.junit:junit-bom` (see `holon-stack.md`, Parent POM) |
| `No qualifying bean of type 'Datastore'` | the holon-saas / Holon JPA starter is missing from the classpath |

A clean compile does not prove the SQL: also load the migrations (see the
`flyway-migration` skill), and say in the report what was **not** run (a real database,
PostgreSQL, the service itself).

## Step 8 — Repeat for `@Embeddable` classes

Add `@Caption` and validation to each field inside the `@Embeddable`, and its keys to both
bundles. The embedding entity needs no change; Holon flattens the nested bean.

## Constraints

- **Guardrail gate:** zero 🛑 findings, every ⚠️ waived inline, the report printed; a clean
  compile alone is not enough.
- **Every association explicitly `FetchType.LAZY`** (`JPA-001`); no `CascadeType.ALL`/`REMOVE`
  on the many side (`JPA-002`).
- **No `@Enumerated`, no Java enum for a domain value** — a lookup-table FK (`JPA-005`).
- **Every entity has `@Version`** (inherited from the audit base class) and uses `Instant`
  (`JPA-012`, `JPA-008`).
- **No Lombok `@Data` / `@EqualsAndHashCode` / `@ToString` on an entity** (`JPA-020`).
- **No repository** unless the datastore cannot map the entity, and then with a `FALLBACK` comment.
- **All writes through `BeanDatastoreHelper`.**
- **One `*Model` and one `*Service` per entity, in the entity's own feature package.**
- **Append** to resource bundles; never overwrite.
- **Do not name a business field `version`** — the audit base class owns it.
- **Never remove or change an existing JPA annotation.**

## Type and import reference

| Type / annotation | Package |
|---|---|
| `@Caption(value, messageCode)` | `com.holonplatform.core.i18n` |
| `@Ignore` | `com.holonplatform.core.beans` |
| `@NotBlank`, `@NotNull`, `@Size` | `jakarta.validation.constraints` |
| `BeanPropertySet<T>` | `com.holonplatform.core.beans` |
| `NumericProperty`, `StringProperty`, `BooleanProperty`, `PropertySet` | `com.holonplatform.core.property` |
| `Datastore` | `com.holonplatform.core.datastore` |
| `BeanDatastore`, `BeanDatastoreHelper` | `com.holonplatform.core.datastore.beans` |
| `QueryFilter`, `QuerySort` | `com.holonplatform.core.query` |
| `HolonEntityMappingValidator` | `com.holonplatform.multitenant.testing` (holon-saas `tenant-core`) |
| `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate` | `org.springframework.data.annotation` (audit base class only) |
| `AuditingEntityListener` | `org.springframework.data.jpa.domain.support` (audit base class only) |
