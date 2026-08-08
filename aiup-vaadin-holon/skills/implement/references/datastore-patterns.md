# Datastore Patterns Reference

All persistence in the Holon Platform stack goes through **`BeanDatastoreHelper<T>`** —
a typed helper over `BeanDatastore` that covers the overwhelming majority of query, save,
and delete needs without writing raw query chains.

**Rule:** always check `BeanDatastoreHelper` first. Only fall back to a raw
`BeanDatastore` / `Datastore` query chain when `BeanDatastoreHelper` has no method for
the required operation (e.g. multi-table joins, aggregates, recursive CTEs).

Never use `PropertyBox`, Spring Data, or raw JPA.

---

## Two required artefacts per feature

Every feature needs a **Model interface** and a **service** that holds a
`BeanDatastoreHelper<T>` instance.

### 1 · Model interface

Declare all typed `PathProperty` constants here. These are the only thing you
pass to `BeanDatastoreHelper` as filters and sorts — never use raw string property
names outside the model.

```java
package com.example.ap.bill;

import com.example.ap.bill.Bill;
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.property.NumericProperty;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertySet;
import com.holonplatform.core.property.StringProperty;
import com.holonplatform.core.property.TemporalProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("rawtypes")
public interface BillModel {

    DataTarget<String> TARGET = DataTarget.named("bill");   // matches @DataPath on the bean class

    BeanPropertySet<Bill> PROPERTY_SET = BeanPropertySet.create(Bill.class);

    // Use typed sub-types — they expose the richer query expression API for their type:
    //   NumericProperty  → .count(), .sum(), .avg(), .min(), .max()
    //   StringProperty   → .contains(), .startsWith(), .endsWith(), case-insensitive variants
    //   TemporalProperty → .year(), .month(), .day(), .hour()
    //   BooleanProperty  → .isTrue(), .isFalse()
    NumericProperty<Long>        ID           = PROPERTY_SET.propertyNumeric("id");
    StringProperty               VENDOR_NAME  = PROPERTY_SET.propertyString("vendorName");
    StringProperty               INVOICE_NO   = PROPERTY_SET.propertyString("invoiceNumber");
    TemporalProperty<LocalDate>  INVOICE_DATE = PROPERTY_SET.propertyTemporal("invoiceDate");
    PathProperty<BigDecimal>     TOTAL_AMOUNT = PROPERTY_SET.property("totalAmount", BigDecimal.class);
    StringProperty               STATUS       = PROPERTY_SET.propertyString("status");

    // Subset property sets for UI listing / form views.
    // RULE: any PropertySet used with the Datastore (queries, writes) MUST declare .withIdentifier().
    // Use .withIdentifier(ID) when ID is in the set.
    PropertySet LISTING = PropertySet.builderOf(ID, VENDOR_NAME, INVOICE_NO, INVOICE_DATE, TOTAL_AMOUNT, STATUS)
            .withIdentifier(ID).build();

    // FORM is UI-only — populated via form.setBean(bean), never used directly as a Datastore projection.
    // It intentionally omits ID; no .withIdentifier() is needed for UI-only subsets.
    PropertySet FORM = PropertySet.builderOf(VENDOR_NAME, INVOICE_NO, INVOICE_DATE, TOTAL_AMOUNT, STATUS).build();
}
```

### 2 · Service class

```java
package com.example.ap.bill;   // feature package — same package as Bill.java

import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastoreHelper;

/**
 * Datastore-backed service for Bill operations.
 * Declare as a Spring @Bean and inject via constructor injection.
 */
public class BillService {

    private final BeanDatastoreHelper<Bill> helper;

    public BillService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
    }
}
```

---

## Query patterns (use `BeanDatastoreHelper` first)

### Find all — no filter

```java
// All records — returns Stream<T>, collect if needed
Stream<Bill> all = helper.findAll();
List<Bill> list  = helper.findAll().toList();
```

### Find all — with filter

```java
// Single filter
Stream<Bill> pending = helper.findAll(BillModel.STATUS.eq("PENDING_REVIEW"));

// Single filter + sort
Stream<Bill> sorted = helper.findAll(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.INVOICE_DATE.asc());

// Multiple filters (AND) — compose with QueryFilter
import com.holonplatform.core.query.QueryFilter;

Stream<Bill> filtered = helper.findAll(
    QueryFilter.allOf(
        BillModel.STATUS.eq("PENDING_REVIEW"),
        BillModel.VENDOR_NAME.contains("ACME")),
    BillModel.INVOICE_DATE.desc());
```

### Find single record

```java
// By exact filter (expects at most one row — throws if more than one)
Optional<Bill> one = helper.findOne(BillModel.ID.eq(id));

// First match for a filter (no uniqueness assumption)
Optional<Bill> first = helper.findFirst(BillModel.ID.eq(id));

// First match with sort (useful for "latest" queries)
Optional<Bill> latest = helper.findFirst(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.INVOICE_DATE.desc());
```

### Top N records

```java
// Top 10 most recent pending bills
Stream<Bill> top10 = helper.findTop(10,
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.INVOICE_DATE.desc());
```

### Pagination

`findPage` uses **1-based page numbers**; `findSlice` uses **0-based offset**.

```java
// Page 1, 20 records per page, filtered and sorted (1-based)
Stream<Bill> page1 = helper.findPage(1, 20,
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.INVOICE_DATE.desc());

// Offset-based slice: skip 40, take 20 (0-based offset)
Stream<Bill> slice = helper.findSlice(40, 20,
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.INVOICE_DATE.desc());
```

### Count and exists

```java
// Count all
long total = helper.count();

// Count matching a filter
long pending = helper.count(BillModel.STATUS.eq("PENDING_REVIEW"));

// Check existence
boolean exists = helper.exists(BillModel.ID.eq(id));
```

---

## Write patterns

### Save (upsert)

`save()` inserts when `@Identifier` is `null`, updates otherwise.
Auto-generated IDs (database sequences / auto-increment) are **automatically written back** into the
bean after `save()` or `insert()` — no manual refresh is needed.

```java
// Insert (id == null → INSERT; id is populated by the database after save)
Bill newBill = new Bill();
newBill.setVendorName("ACME Corp");
newBill.setInvoiceDate(LocalDate.now());
newBill.setTotalAmount(new BigDecimal("1500.00"));
newBill.setStatus("PENDING_REVIEW");
helper.save(newBill);
// newBill.getId() is now populated — DefaultWriteOption.BRING_BACK_GENERATED_IDS is active by default

// Update (id != null → UPDATE)
bill.setStatus("APPROVED");
helper.save(bill);
```

### Explicit insert / update

Use when you need to enforce intent (guard against accidental upsert direction):

```java
helper.insert(newBill);   // always INSERT — throws if id is already set
helper.update(bill);      // always UPDATE — requires non-null id
```

### Delete single record

```java
helper.delete(bill);   // deletes the row matching the @Identifier value
```

### Refresh

Re-loads the bean from the database, replacing in-memory values:

```java
Bill fresh = helper.refresh(bill);
```

---

## Bulk operations

Use bulk operations for batch writes. **Never loop `save()` over a collection.**

```java
// Bulk insert a collection
helper.bulkInsert(bills);

// Bulk insert varargs
helper.bulkInsert(bill1, bill2, bill3);

// Bulk save (upsert) a list
helper.bulkSave(bills);

// Bulk update a list (each bean must have its @Identifier set)
helper.bulkUpdate(bills);

// Bulk delete a list
helper.bulkDelete(bills);

// Bulk delete by filter (no bean instances needed)
helper.bulkDelete(BillModel.STATUS.eq("REJECTED"));

// Bulk update a single property on all matching rows
helper.bulkUpdateProperty(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    "status",
    "UNDER_REVIEW");

// Bulk update multiple properties on matching rows
helper.bulkUpdateProperties(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    Map.of("status", "UNDER_REVIEW", "reviewerNote", "Batch escalation"));
```

---

## Transactions

```java
helper.withTransaction(tx -> {
    helper.save(bill);
    helper.save(lineItem);   // lineItem uses its own BeanDatastoreHelper
    tx.commit();
    return null;
});

// Check if the current operation context is already transactional
if (helper.isTransactional()) { ... }
```

For multi-service transactions that span more than one `BeanDatastoreHelper` instance,
use Spring `@Transactional` on the service method (the class may then be a `@Service`
with constructor injection):

```java
@Service
public class BillService {

    private final BeanDatastoreHelper<Bill> billHelper;
    private final BeanDatastoreHelper<BillLineItem> lineItemHelper;

    public BillService(Datastore datastore) {
        this.billHelper     = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
        this.lineItemHelper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), BillLineItem.class);
    }

    @Transactional
    public void submitWithLineItems(Bill bill, List<BillLineItem> items) {
        billHelper.save(bill);
        items.forEach(item -> {
            item.setBillId(bill.getId());
            lineItemHelper.save(item);
        });
    }
}
```

---

## Raw Datastore fallback (last resort only)

Use the raw `BeanDatastore` or `Datastore` API **only** when `BeanDatastoreHelper` has
no matching method. Add a justification comment:

```java
// FALLBACK: BeanDatastoreHelper has no equivalent for multi-table aggregate projection
helper.getDatastore()
    .query(BillModel.TARGET)
    ...
```

The `BeanDatastore` is always accessible via `helper.getDatastore()` — never construct a
separate `Datastore` reference for the same feature.

### Bean projection in raw queries (`BeanProjection`)

When using raw `Datastore` queries and you need the result as bean instances instead of `PropertyBox` rows:

```java
import com.holonplatform.core.datastore.beans.BeanProjection;

// Project directly to Bean instances
Stream<Bill> bills = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.STATUS.eq("APPROVED"))
    .stream(BeanProjection.of(Bill.class));

Optional<Bill> one = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.ID.eq(id))
    .findOne(BeanProjection.of(Bill.class));

// Restrict to specific paths only (partial load):
helper.getDatastore()
    .query(BillModel.TARGET)
    .stream(BeanProjection.of(Bill.class, BillModel.PROPERTY_SET.property("status")));
```

---

## Filter expressions (`QueryFilter` / `QueryExpression`)

Every `PathProperty<T>` from the Model interface implements `QueryExpression<T>`, which
provides all filter methods directly on the property constant. Use these instead of
`QueryFilter` static methods wherever possible — they read more naturally.

### Comparison

```java
BillModel.STATUS.eq("PENDING_REVIEW")           // =
BillModel.STATUS.neq("APPROVED")                // !=
BillModel.TOTAL_AMOUNT.gt(new BigDecimal("0"))  // >
BillModel.TOTAL_AMOUNT.goe(new BigDecimal("100")) // >=
BillModel.TOTAL_AMOUNT.lt(new BigDecimal("1000")) // <
BillModel.TOTAL_AMOUNT.loe(new BigDecimal("999")) // <=
BillModel.TOTAL_AMOUNT.between(new BigDecimal("100"), new BigDecimal("999")) // BETWEEN
BillModel.ID.isNull()                           // IS NULL
BillModel.ID.isNotNull()                        // IS NOT NULL
```

### IN / NOT IN

```java
BillModel.STATUS.in("PENDING_REVIEW", "UNDER_REVIEW")
BillModel.STATUS.in(List.of("PENDING_REVIEW", "APPROVED"))
BillModel.STATUS.nin("REJECTED", "ARCHIVED")
BillModel.ID.in(subQuery)    // IN (SELECT ...)
```

### String-specific (`PathProperty<String>` only)

```java
BillModel.VENDOR_NAME.contains("ACME")              // LIKE '%ACME%'
BillModel.VENDOR_NAME.containsIgnoreCase("acme")    // LIKE '%acme%' (case-insensitive)
BillModel.VENDOR_NAME.startsWith("ACME")            // LIKE 'ACME%'
BillModel.VENDOR_NAME.startsWithIgnoreCase("acme")
BillModel.VENDOR_NAME.endsWith("Corp")              // LIKE '%Corp'
BillModel.VENDOR_NAME.endsWithIgnoreCase("corp")
```

### Combining filters (AND / OR / NOT)

```java
// AND — chain with .and(...)
QueryFilter combined = BillModel.STATUS.eq("PENDING_REVIEW")
    .and(BillModel.TOTAL_AMOUNT.gt(BigDecimal.ZERO));

// OR — chain with .or(...)
QueryFilter either = BillModel.STATUS.eq("PENDING_REVIEW")
    .or(BillModel.STATUS.eq("UNDER_REVIEW"));

// NOT
QueryFilter notRejected = BillModel.STATUS.eq("REJECTED").not();

// allOf (varargs AND — returns Optional, use .orElse(null) or .get())
QueryFilter all = QueryFilter.allOf(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.VENDOR_NAME.containsIgnoreCase("acme"),
    BillModel.TOTAL_AMOUNT.gt(BigDecimal.ZERO)).orElseThrow();

// anyOf (varargs OR)
QueryFilter any = QueryFilter.anyOf(
    BillModel.STATUS.eq("PENDING_REVIEW"),
    BillModel.STATUS.eq("UNDER_REVIEW")).orElseThrow();
```

### Sort

```java
BillModel.INVOICE_DATE.asc()    // ORDER BY invoice_date ASC
BillModel.INVOICE_DATE.desc()   // ORDER BY invoice_date DESC

// Compound sort — chain with .and(...)
QuerySort sort = BillModel.STATUS.asc().and(BillModel.INVOICE_DATE.desc());

// Or use QuerySort.of(...)
QuerySort sort2 = QuerySort.of(BillModel.STATUS.asc(), BillModel.INVOICE_DATE.desc());
```

---

## Aggregate functions (`QueryFunction`)

Use aggregate functions in projections via the raw `BeanDatastore` query (no
`BeanDatastoreHelper` equivalent — this is the expected fallback):

```java
import com.holonplatform.core.query.QueryFunction;

// COUNT(*)
long total = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.STATUS.eq("PENDING_REVIEW"))
    .stream(QueryFunction.count(BillModel.ID))
    .findFirst().orElse(0L);

// SUM
BigDecimal totalAmount = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.STATUS.eq("APPROVED"))
    .stream(QueryFunction.sum(BillModel.TOTAL_AMOUNT))
    .findFirst().orElse(BigDecimal.ZERO);

// MIN / MAX
Optional<LocalDate> earliest = helper.getDatastore()
    .query(BillModel.TARGET)
    .stream(QueryFunction.min(BillModel.INVOICE_DATE))
    .findFirst();

// AVG
Double avg = helper.getDatastore()
    .query(BillModel.TARGET)
    .stream(QueryFunction.avg(BillModel.TOTAL_AMOUNT))
    .findFirst().orElse(0.0);
```

### GROUP BY (QueryAggregation)

```java
import com.holonplatform.core.query.QueryAggregation;

// GROUP BY status + HAVING count > 5
helper.getDatastore()
    .query(BillModel.TARGET)
    .aggregate(QueryAggregation.builder()
        .path(BillModel.STATUS)
        .filter(QueryFunction.count(BillModel.ID).gt(5L))
        .build())
    .stream(BillModel.STATUS, QueryFunction.count(BillModel.ID));
```

---

## Joins (`RelationalTarget`)

Joins require the raw `BeanDatastore.query()` — `BeanDatastoreHelper` does not expose join configuration. Use `RelationalTarget` to add join clauses, then project with a `PropertySet`.

```java
import com.holonplatform.core.datastore.relational.RelationalTarget;

// INNER JOIN: bills b INNER JOIN customers c ON b.customer_id = c.id
RelationalTarget<?> target = RelationalTarget.of(BillModel.TARGET)
    .innerJoin(CustomerModel.TARGET)
        .on(BillModel.CUSTOMER_ID.eq(CustomerModel.ID))
        .add();

// LEFT JOIN
RelationalTarget<?> targetLeft = RelationalTarget.of(BillModel.TARGET)
    .leftJoin(CustomerModel.TARGET)
        .on(BillModel.CUSTOMER_ID.eq(CustomerModel.ID))
        .add();

// RIGHT JOIN
RelationalTarget<?> targetRight = RelationalTarget.of(BillModel.TARGET)
    .rightJoin(CustomerModel.TARGET)
        .on(BillModel.CUSTOMER_ID.eq(CustomerModel.ID))
        .add();

// Query with join — project only the columns you need
// (use a PropertySet from both models, or use raw Property varargs)
List<PropertyBox> rows = helper.getDatastore()
    .query(target)
    .filter(BillModel.STATUS.eq("APPROVED"))
    .sort(BillModel.INVOICE_DATE.desc())
    .list(BillModel.VENDOR_NAME, BillModel.TOTAL_AMOUNT,
          CustomerModel.NAME, CustomerModel.BILLING_EMAIL);
```

> **Rule:** joins return `PropertyBox` rows — not beans. Use `box.getValue(BillModel.VENDOR_NAME)`
> to extract individual values. Do not attempt to map join results back to a single bean.

---

## Sub-queries and EXISTS / NOT EXISTS

```java
import com.holonplatform.core.datastore.relational.SubQuery;

// EXISTS (SELECT 1 FROM contact WHERE contact.customer_id = customer.id)
QueryFilter hasContacts = SubQuery.create()
    .target(ContactModel.TARGET)
    .filter(ContactModel.CUSTOMER_ID.eq(CustomerModel.ID))
    .exists();

Stream<Customer> customersWithContacts = helper.getDatastore()
    .query(CustomerModel.TARGET)
    .filter(hasContacts)
    .stream(CustomerModel.PROPERTY_SET);

// NOT EXISTS
QueryFilter hasNoContacts = SubQuery.create()
    .target(ContactModel.TARGET)
    .filter(ContactModel.CUSTOMER_ID.eq(CustomerModel.ID))
    .notExists();

// IN (subquery) — select bills where customer is active
QueryFilter activeBills = BillModel.CUSTOMER_ID.in(
    SubQuery.create(Long.class)
        .target(CustomerModel.TARGET)
        .filter(CustomerModel.STATUS.eq("ACTIVE"))
        .select(CustomerModel.ID));

Stream<Bill> active = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(activeBills)
    .stream(BillModel.PROPERTY_SET);
```

---

## Projection — scalar values and multiple columns

When you need only specific columns (not full beans):

```java
// Single column — stream of values
Stream<String> statuses = helper.getDatastore()
    .query(BillModel.TARGET)
    .stream(BillModel.STATUS);

// Multiple columns — stream of PropertyBox
Stream<PropertyBox> rows = helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.STATUS.eq("APPROVED"))
    .stream(BillModel.VENDOR_NAME, BillModel.TOTAL_AMOUNT, BillModel.INVOICE_DATE);

// Read values from PropertyBox
rows.forEach(box -> {
    String vendor = box.getValue(BillModel.VENDOR_NAME);
    BigDecimal amount = box.getValue(BillModel.TOTAL_AMOUNT);
});
```

---

## Distinct

```java
helper.getDatastore()
    .query(BillModel.TARGET)
    .distinct()
    .stream(BillModel.STATUS);
```

---

## Temporal functions

```java
import com.holonplatform.core.query.QueryFunction;
import com.holonplatform.core.query.TemporalFunction;

// Filter by year/month/day extracted from a date column
helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(QueryFunction.year(BillModel.INVOICE_DATE).eq(2025))
    .filter(QueryFunction.month(BillModel.INVOICE_DATE).eq(12))
    .stream(BillModel.PROPERTY_SET);

// Use current date/time in a filter
helper.getDatastore()
    .query(BillModel.TARGET)
    .filter(BillModel.INVOICE_DATE.loe(QueryFunction.currentLocalDate()))
    .stream(BillModel.PROPERTY_SET);
```

---

## Bean annotations (`BeanPropertySet` mapping rules)

`BeanPropertySet.create(MyBean.class)` introspects the bean class using these annotations.
Always annotate your domain beans appropriately so the property set reflects the right
column names, captions, validators, and converters.

| Annotation | Purpose |
|---|---|
| `@DataPath("col_name")` | Override property→column mapping (field) or bean→table mapping (class) |
| `@Caption("Label")` | Display caption for the property in UI; `@Caption(value="Name", messageCode="i18n.code")` for i18n |
| `@Sequence(10)` | Declare display order in PropertySet iteration |
| `@NotNull` | Add `Validator.notNull()` to the property |
| `@NotEmpty` | Add `Validator.notEmpty()` |
| `@NotBlank` | Add `Validator.notBlank()` |
| `@Min(1)` / `@Max(100)` | Add numeric range validators |
| `@Size(min=1, max=255)` | Add length validators |
| `@Email` | Add email-format validator |
| `@Validator(MyValidator.class)` | Attach a custom `Validator<T>` class (repeatable) |
| `@Converter(MyConverter.class)` | Custom `PropertyValueConverter` (e.g. enum ↔ String) |
| `@Converter(builtin = BUILTIN.NUMERIC_BOOLEAN)` | Builtin converters: `NUMERIC_BOOLEAN`, `LOCAL_DATE`, `LOCAL_DATETIME`, `ENUM_BY_ORDINAL`, `ENUM_BY_NAME` |
| `@Config(key="k", value="v")` | Add arbitrary configuration attributes to the property (repeatable) |
| `@Ignore` | Exclude the field from the BeanPropertySet entirely |

```java
@DataPath("bill")   // table name — matches DataTarget.named("bill") in the Model interface
public class Bill {

    @Identifier          // marks this field as the entity identifier for Datastore operations
    private Long id;

    @DataPath("vendor_nm")   // explicit column name override
    @Caption(value = "Vendor", messageCode = "bill.vendor")
    @NotBlank
    private String vendorName;

    @Caption("Invoice Date")
    @NotNull
    private LocalDate invoiceDate;

    @Caption("Total")
    @NotNull
    @Min(0)
    private BigDecimal totalAmount;

    @Caption("Status")
    @NotNull
    @Sequence(10)
    private String status;

    // getters / setters ...
}
```

> **`@DataPath` on class is mandatory** when the table name differs from the class name
> (snake_case vs. CamelCase). It must match `DataTarget.named("...")` in the Model interface.

---

## Property value converters

Use converters when the Java type differs from the database storage type.
Declare them on the bean field via `@Converter` or on a standalone `PathProperty` via `.converter(...)`.

```java
// Builtin converters (use via @Converter annotation or via PathProperty builder)
import com.holonplatform.core.property.PropertyValueConverter;

// Boolean stored as 0/1 integer in the database:
PathProperty<Boolean> ACTIVE = PathProperty.create("active", Boolean.class)
    .converter(PropertyValueConverter.numericBoolean(Integer.class));

// java.util.Date stored without time component:
PathProperty<java.util.Date> CREATED = PathProperty.create("created_date", java.util.Date.class)
    .temporalType(TemporalType.DATE);     // must set TemporalType for Date/Calendar

// Enum stored as its .name() string:
PathProperty<MyStatus> STATUS = PathProperty.create("status", MyStatus.class)
    .converter(PropertyValueConverter.enumByName());

// Enum stored as ordinal integer:
PathProperty<MyStatus> STATUS = PathProperty.create("status", MyStatus.class)
    .converter(PropertyValueConverter.enumByOrdinal());

// Custom inline converter:
PathProperty<Integer> PRIORITY = PathProperty.create("priority", Integer.class)
    .converter(String.class,
        v -> (v != null) ? Integer.parseInt(v) : null,   // fromModel (DB string → Java int)
        v -> (v != null) ? String.valueOf(v) : null);    // toModel (Java int → DB string)
```

> **`TemporalType` is required** for `java.util.Date` and `Calendar` properties.
> For Java 8 date/time types (`LocalDate`, `LocalDateTime`, `Instant`) no `TemporalType` is needed.

---

## Using JPA Datastore (exception case only)

Use the JPA Datastore (`holon-datastore-jpa`) **only** when the JDBC Datastore cannot
express a required query (e.g. recursive CTEs). Add a justification comment:

```java
// FALLBACK: JDBC Datastore lacks native recursive CTE support for this hierarchy query
```

All `BeanDatastoreHelper` patterns above apply identically to both JDBC and JPA Datastores.

