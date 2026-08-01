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

@Caption(message = "Invoice Number", messageCode = "bill.invoiceNumber")
private String invoiceNumber;

@Caption(message = "Total Amount (USD)", messageCode = "bill.totalAmount")
private BigDecimal totalAmount;

@Caption(message = "Status", messageCode = "bill.status")
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

Mark mandatory fields with `@NotNull` from `com.holonplatform.core.beans.NotNull`.
`EntityPanelForm` reads this annotation and sets `aria-required="true"` automatically.

```java
import com.holonplatform.core.beans.NotNull;

@NotNull
@Caption(message = "Vendor Name", messageCode = "bill.vendorName")
private String vendorName;
```

### Foreign key fields

```java
@DataPath("customer_id")
private Long customerId;        // store the FK value as a Long

// For convenience, you may also add a transient reference:
@Transient
private Customer customer;      // loaded separately, not persisted via BeanPropertySet
```

---

## BeanPropertySet

`BeanPropertySet<T>` is the property descriptor for a JavaBean. Declare it as a
`public static final` constant on the bean class (or a companion class):

```java
import com.holonplatform.core.beans.BeanPropertySet;

@DataPath("order")
public class Order {

    public static final BeanPropertySet<Order> PROPERTIES =
        BeanPropertySet.create(Order.class);

    @Identifier
    private Long id;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    // getters / setters
}
```

### Using BeanPropertySet for queries

```java
// Get the DataPath (table name) from the BeanPropertySet
DataTarget<?> target = Order.PROPERTIES.getDataPath();   // → "order"

// Reference a property by name (type-safe)
PathProperty<String> statusProp = (PathProperty<String>)
    Order.PROPERTIES.property("status");

// Filter expression
QueryFilter filter = statusProp.eq("PENDING");
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

Always derive column names from the field `@DataPath` values when writing Flyway migrations.

---

## Full example

```java
package com.example.ap.domain;

import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.beans.DataPath;
import com.holonplatform.core.beans.Identifier;
import com.holonplatform.core.beans.NotNull;
import com.holonplatform.core.i18n.Caption;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@DataPath("bill")
public class Bill {

    public static final BeanPropertySet<Bill> PROPERTIES =
        BeanPropertySet.create(Bill.class);

    @Identifier
    @DataPath("id")
    @Caption(message = "ID", messageCode = "bill.id")
    private Long id;

    @NotNull
    @DataPath("vendor_name")
    @Caption(message = "Vendor Name", messageCode = "bill.vendorName")
    private String vendorName;

    @NotNull
    @DataPath("invoice_number")
    @Caption(message = "Invoice Number", messageCode = "bill.invoiceNumber")
    private String invoiceNumber;

    @NotNull
    @DataPath("invoice_date")
    @Caption(message = "Invoice Date", messageCode = "bill.invoiceDate")
    private LocalDateTime invoiceDate;

    @NotNull
    @DataPath("total_amount")
    @Caption(message = "Total Amount (USD)", messageCode = "bill.totalAmount")
    private BigDecimal totalAmount;

    @NotNull
    @DataPath("status")
    @Caption(message = "Status", messageCode = "bill.status")
    private String status;          // "PENDING_REVIEW", "APPROVED", "REJECTED"

    @DataPath("purchase_order_id")
    @Caption(message = "Purchase Order", messageCode = "bill.purchaseOrderId")
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
