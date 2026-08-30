# Bean Model Reference

Domain objects in the Holon Platform + Vaadin Flow stack are **plain JavaBeans** annotated
with Holon bean annotations. **Never use `PropertyBox`** — the stack uses `Bean` +
`BeanPropertySet<T>` exclusively.

---

## JavaBean conventions

### `@DataPath` — table / column mapping

```java
import com.holonplatform.core.beans.DataPath;

@DataPath("order")                  // maps to the "order" table in the database
public class Order {

    @DataPath("id")                 // optional — field name used by default
    private Long id;

    @DataPath("customer_name")      // explicit snake_case override
    private String customerName;

    @DataPath("total_amount")
    private BigDecimal totalAmount;

    @DataPath("status")
    private String status;          // "PENDING", "APPROVED", "REJECTED"

    @DataPath("created_at")
    private LocalDateTime createdAt;

    // standard getters / setters (Lombok @Data is fine)
}
```

**Rules:**
- Class-level `@DataPath` maps to the database table name (snake_case).
- Field-level `@DataPath` maps to the column name. If omitted, the field name is used as-is.
  Always add `@DataPath` when the column name differs from the field name (i.e., camelCase → snake_case).
- Column names in Flyway migrations MUST match the `@DataPath` values on the fields.

### `@Identifier` — primary key

```java
import com.holonplatform.core.beans.Identifier;

@Identifier
private Long id;
```

- Exactly one field per bean should be `@Identifier`.
- The field type is `Long` for generated sequence PKs.
- Composite keys: annotate each part-field with `@Identifier`.

### `@Caption` — I18N label for every property ⭐ required

Every user-visible bean field MUST be annotated with `@Caption` so that Holon components
(`ListingBundle`, `EntityPanelForm`) automatically pick up translated labels and column
headers from the `BeanPropertySet` — no `.columnHeader(...)` or `.propertyCaption(...)`
calls needed in view code.

```java
import com.holonplatform.core.i18n.Caption;

@Caption(value = "Invoice Number", messageCode = "bill.invoiceNumber")
private String invoiceNumber;

@Caption(value = "Total Amount (USD)", messageCode = "bill.totalAmount")
private BigDecimal totalAmount;

@Caption(value = "Status", messageCode = "bill.status")
private String status;
```

**`@Caption` fields:**

| Field | Meaning |
|-------|---------|
| `message` | Fallback text shown when no translation bundle is loaded (always English) |
| `messageCode` | Holon i18n message key resolved from the active `LocalizationContext` at runtime |

**Key naming convention:** `<domain>.<fieldName>` (e.g. `bill.invoiceNumber`,
`bill.totalAmount`). Keep keys stable and domain-scoped.

> `BeanPropertySet.create(BeanClass.class)` scans all `@Caption` annotations at startup.
> Every downstream Holon component built from the same `BeanPropertySet` automatically
> uses these captions. This is the single source of truth for field labels in the UI.

### `@NotNull` — required-field marker

Mark mandatory fields with `@NotNull` from `jakarta.validation.constraints`.
`EntityPanelForm` reads this annotation and sets `aria-required="true"` automatically.

```java
import jakarta.validation.constraints.NotNull;

@NotNull
@Caption(value = "Vendor Name", messageCode = "bill.vendorName")
private String vendorName;
```

### `@Sequence` — property ordering

Use `@Sequence` to control the order in which properties appear in `BeanPropertySet` iteration,
which determines the default column / field order in `ListingBundle` and `EntityPanelForm`.

```java
import com.holonplatform.core.beans.Sequence;

@Sequence(1)
private Long id;

@Sequence(2)
private String vendorName;

@Sequence(3)
private LocalDate invoiceDate;
```

Lower values appear first. Properties without `@Sequence` appear after all sequenced ones.

### Foreign key fields

```java
@DataPath("customer_id")
private Long customerId;        // store the FK value as a Long

// For convenience, you may also add a transient reference:
@Transient
private Customer customer;      // loaded separately, not persisted via BeanPropertySet
```

---

### Audit & Version fields

Every domain JavaBean **must** include the five audit/version fields below. They map
directly to the mandatory audit columns added to every entity table by the
`flyway-migration` skill.

```java
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import jakarta.persistence.Version;   // @Version — standard JPA / Spring Data optimistic-lock marker
import java.time.Instant;

// ── Audit & version ──────────────────────────────────────────────────────────
// IMPORTANT: use Instant (not LocalDateTime) for all timestamp fields so values
// are stored and retrieved in UTC and can be converted to any user timezone at
// display time.  See holon-vaadin-ui.md §"Timezone-aware display".

@CreatedBy
@DataPath("created_by")
@Caption(value = "Created By", messageCode = "audit.createdBy")
private String createdBy;

@CreatedDate
@DataPath("created_date")
@Caption(value = "Created Date", messageCode = "audit.createdDate")
private Instant createdDate;          // UTC — convert to user zone in the view

@LastModifiedBy
@DataPath("last_modified_by")
@Caption(value = "Last Modified By", messageCode = "audit.lastModifiedBy")
private String lastModifiedBy;

@LastModifiedDate
@DataPath("last_modified_date")
@Caption(value = "Last Modified Date", messageCode = "audit.lastModifiedDate")
private Instant lastModifiedDate;     // UTC — convert to user zone in the view

@Version
@DataPath("version")
@Caption(value = "Version", messageCode = "audit.version")
private Long version;
```

**Notes:**

- `@Version` enables **optimistic locking**: the Holon Datastore checks that the
  `version` value has not changed since the entity was loaded before issuing an `UPDATE`.
  Increment is handled automatically.
- `@CreatedBy` / `@LastModifiedBy` are populated automatically when Spring JPA
  auditing is enabled (`@EnableJpaAuditing` on a `@Configuration` class, plus an
  `AuditorAware<String>` bean that returns the current username). When using only the
  Holon Datastore (no JPA), set `createdBy` / `lastModifiedBy` in the service layer before
  calling `Datastore.save(...)`.
- Audit fields are **not** typically shown in grids or forms; exclude them from the
  `LISTING_SUBSET` and `FORM_SUBSET` defined in the companion `<Entity>Model`.

---

## BeanPropertySet (declare in `<Entity>Model`, not on the bean)

`BeanPropertySet<T>` is the property descriptor for a JavaBean. Declare it in the companion
`<Entity>Model` interface — **not** on the bean class itself — alongside all typed property
constants and `PropertySet` subsets. See `datastore-patterns.md` for the full Model interface pattern.

The bean class stays clean: no `PROPERTIES` static field, no Holon imports (except annotations).

### Using BeanPropertySet for typed property constants

```java
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.property.NumericProperty;
import com.holonplatform.core.property.StringProperty;
import com.holonplatform.core.property.TemporalProperty;

BeanPropertySet<Order> PROPERTY_SET = BeanPropertySet.create(Order.class);

// Preferred: use typed sub-types — they expose richer query expression APIs
NumericProperty<Long>       ID         = PROPERTY_SET.propertyNumeric("id");
StringProperty              STATUS     = PROPERTY_SET.propertyString("status");
TemporalProperty<LocalDateTime> CREATED_AT = PROPERTY_SET.propertyTemporal("createdAt");
BooleanProperty             ACTIVE     = PROPERTY_SET.propertyBoolean("active");

// Fallback when no typed sub-type exists (e.g. BigDecimal):
PathProperty<BigDecimal>    TOTAL      = PROPERTY_SET.property("total", BigDecimal.class);
```

---

## Naming conventions summary

| Java field | `@DataPath` value | Database column |
|-----------|------------------|-----------------|
| `id` | `"id"` (or omit) | `id` |
| `customerName` | `"customer_name"` | `customer_name` |
| `totalAmount` | `"total_amount"` | `total_amount` |
| `createdAt` | `"created_at"` | `created_at` |
| `customerId` (FK) | `"customer_id"` | `customer_id` |
| `createdBy` | `"created_by"` | `created_by` |
| `createdDate` | `"created_date"` | `created_date` |
| `lastModifiedBy` | `"last_modified_by"` | `last_modified_by` |
| `lastModifiedDate` | `"last_modified_date"` | `last_modified_date` |
| `version` | `"version"` | `version` |

Always derive column names from the field `@DataPath` values when writing Flyway migrations.

### UTC timestamp rule

All timestamp fields in every domain bean **must** use `java.time.Instant` (not
`LocalDateTime`). `Instant` is always UTC, so the value stored in the database is
unambiguous regardless of server location.

- Flyway columns must be `TIMESTAMPTZ` (Postgres) or equivalent `TIMESTAMP WITH TIME ZONE`.
- JVM startup: always pass `-Duser.timezone=UTC` (or set `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` when Spring JPA is in use) to prevent driver-level timezone shifts.
- **Display**: convert `Instant → ZonedDateTime` using the user's browser timezone captured via `ExtendedClientDetails` (see `holon-vaadin-ui.md` §"Timezone-aware display").
- **Input**: when reading a `LocalDateTime` from a date-time picker, re-attach the user's `ZoneId` before saving: `picked.atZone(userZone).toInstant()`.
- Never use `LocalDateTime.now()` for timestamps — use `Instant.now()` instead.

---

## Full example

```java
package com.example.ap.bill;   // feature package — bean, service, and view all live here

import com.holonplatform.core.beans.DataPath;
import com.holonplatform.core.beans.Identifier;
import com.holonplatform.core.beans.Sequence;
import com.holonplatform.core.i18n.Caption;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@DataPath("bill")
public class Bill {

    // No PROPERTIES constant here — it belongs in BillModel (the companion Model interface)

    @Identifier
    @DataPath("id")
    @Sequence(1)
    @Caption(value = "ID", messageCode = "bill.id")
    private Long id;

    @NotNull
    @DataPath("vendor_name")
    @Sequence(2)
    @Caption(value = "Vendor Name", messageCode = "bill.vendorName")
    private String vendorName;

    @NotNull
    @DataPath("invoice_number")
    @Sequence(3)
    @Caption(value = "Invoice Number", messageCode = "bill.invoiceNumber")
    private String invoiceNumber;

    @NotNull
    @DataPath("invoice_date")
    @Sequence(4)
    @Caption(value = "Invoice Date", messageCode = "bill.invoiceDate")
    private Instant invoiceDate;  // stored as UTC; convert to user timezone in the view

    @NotNull
    @DataPath("total_amount")
    @Sequence(5)
    @Caption(value = "Total Amount (USD)", messageCode = "bill.totalAmount")
    private BigDecimal totalAmount;

    @NotNull
    @DataPath("status")
    @Sequence(6)
    @Caption(value = "Status", messageCode = "bill.status")
    private String status;          // "PENDING_REVIEW", "APPROVED", "REJECTED"

    @DataPath("purchase_order_id")
    @Sequence(7)
    @Caption(value = "Purchase Order", messageCode = "bill.purchaseOrderId")
    private Long purchaseOrderId;

    // getters / setters omitted for brevity
}
```

> With `@Caption` on every field, `ListingBundle` and `EntityPanelForm` automatically
> resolve column headers and field labels from the `BeanPropertySet` — no `.columnHeader()`
> or `.propertyCaption()` calls are needed in the view.
> 
> The message keys (`bill.vendorName`, etc.) must be registered in the application's
> Holon message source (e.g. `messages.properties` / `messages_<locale>.properties`).

---

## I18N message file convention

Every bean generates a set of message keys that MUST be present in the bundle:

```properties
# src/main/resources/messages.properties  (English fallback)
bill.id                 = ID
bill.vendorName         = Vendor Name
bill.invoiceNumber      = Invoice Number
bill.invoiceDate        = Invoice Date
bill.totalAmount        = Total Amount (USD)
bill.status             = Status
bill.purchaseOrderId    = Purchase Order
bill.list.empty         = No bills found
```

```properties
# src/main/resources/messages_es.properties  (Spanish)
bill.vendorName         = Nombre del Proveedor
bill.invoiceNumber      = Número de Factura
bill.invoiceDate        = Fecha de Factura
bill.totalAmount        = Importe Total (USD)
bill.status             = Estado
bill.list.empty         = No se encontraron facturas
```
