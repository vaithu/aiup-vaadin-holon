# Datastore Patterns Reference

All persistence in the Holon Platform stack goes through the **Holon `Datastore` API**
backed by `BeanPropertySet`. Never use `PropertyBox`, Spring Data, or raw JPA.

---

## Obtaining the Datastore

Always retrieve the `Datastore` from the Holon `Context` — **never** inject it with `@Autowired`.

```java
import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;

Datastore ds = Context.get()
    .resource(Datastore.CONTEXT_KEY, Datastore.class)
    .orElseThrow(() -> new IllegalStateException("Datastore not available in Holon Context"));
```

The `holon-spring-boot-jdbc-datastore` (or `holon-spring-boot-jpa-datastore`) starter
registers the `Datastore` bean automatically. Holon's Spring Boot auto-config publishes it
into the `Context` — no manual registration needed.

---

## Query patterns

### List all / filtered

```java
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.query.QueryFilter;

// Retrieve the BeanPropertySet constant from the domain class
BeanPropertySet<Order> props = Order.PROPERTIES;

// List all orders
List<Order> all = ds.query(props.getDataPath()).list(props);

// List filtered
List<Order> pending = ds.query(props.getDataPath())
    .filter(props.property("status").eq("PENDING"))
    .list(props);

// Multiple filters (AND)
List<Order> result = ds.query(props.getDataPath())
    .filter(QueryFilter.allOf(
        props.property("status").eq("PENDING"),
        props.property("customerId").eq(customerId)
    ))
    .sort(props.property("createdAt").desc())
    .limit(50)
    .list(props);
```

### Single record

```java
Optional<Order> order = ds.query(Order.PROPERTIES.getDataPath())
    .filter(Order.PROPERTIES.property("id").eq(id))
    .findOne(Order.PROPERTIES);
```

### Count

```java
long count = ds.query(Order.PROPERTIES.getDataPath())
    .filter(Order.PROPERTIES.property("status").eq("PENDING"))
    .count();
```

---

## Insert / Update (save)

`ds.save()` performs an upsert: insert when the `@Identifier` field is `null`,
update otherwise.

```java
// Insert (id is null → INSERT)
Order newOrder = new Order();
newOrder.setCustomerName("ACME Corp");
newOrder.setTotalAmount(new BigDecimal("1500.00"));
newOrder.setStatus("PENDING");
newOrder.setCreatedAt(LocalDateTime.now());

ds.save(Order.PROPERTIES.getDataPath(), newOrder);
// After save, newOrder.getId() is populated with the generated PK

// Update (id is non-null → UPDATE)
order.setStatus("APPROVED");
ds.save(Order.PROPERTIES.getDataPath(), order);
```

---

## Delete

```java
// Delete by PK filter
ds.delete(Order.PROPERTIES.getDataPath(),
    Order.PROPERTIES.property("id").eq(orderId));

// Delete by arbitrary filter
ds.delete(Order.PROPERTIES.getDataPath(),
    Order.PROPERTIES.property("status").eq("REJECTED"));
```

---

## Transactional operations

Wrap multi-step operations in a `Datastore` transaction:

```java
ds.withTransaction(tx -> {
    ds.save(Order.PROPERTIES.getDataPath(), order);
    ds.save(OrderLineItem.PROPERTIES.getDataPath(), lineItem);
    tx.commit();
});
```

---

## Using JPA Datastore (exception case only)

Use the JPA Datastore (`holon-datastore-jpa`) **only** when the JDBC Datastore cannot
express a required query (e.g. complex aggregate or recursive CTE). Add a justification
comment:

```java
// FALLBACK: JDBC Datastore lacks native recursive CTE support for this hierarchy query
Datastore jpaDs = Context.get()
    .resource(Datastore.CONTEXT_KEY, Datastore.class).orElseThrow();
```

The patterns above are identical for both JDBC and JPA Datastores — only the starter
dependency differs.

---

## Service class pattern

```java
package com.example.ap.service;

import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;
import com.example.ap.domain.Bill;

/**
 * Service for Bill operations. Retrieve from Holon Context via CONTEXT_KEY.
 * Do NOT annotate with @Service or inject with @Autowired.
 */
public class BillService {

    public static final String CONTEXT_KEY = BillService.class.getName();

    private final Datastore datastore;

    public BillService() {
        this.datastore = Context.get()
            .resource(Datastore.CONTEXT_KEY, Datastore.class)
            .orElseThrow();
    }

    public List<Bill> findPending() {
        return datastore.query(Bill.PROPERTIES.getDataPath())
            .filter(Bill.PROPERTIES.property("status").eq("PENDING_REVIEW"))
            .sort(Bill.PROPERTIES.property("invoiceDate").asc())
            .list(Bill.PROPERTIES);
    }

    public Optional<Bill> findById(Long id) {
        return datastore.query(Bill.PROPERTIES.getDataPath())
            .filter(Bill.PROPERTIES.property("id").eq(id))
            .findOne(Bill.PROPERTIES);
    }

    public void approve(Long id) {
        findById(id).ifPresent(bill -> {
            bill.setStatus("APPROVED");
            datastore.save(Bill.PROPERTIES.getDataPath(), bill);
        });
    }

    public void reject(Long id) {
        findById(id).ifPresent(bill -> {
            bill.setStatus("REJECTED");
            datastore.save(Bill.PROPERTIES.getDataPath(), bill);
        });
    }
}
```

Register the service in the Holon Context (in a `@Configuration` or `@SpringBootApplication` class):

```java
@Bean
public BillService billService() {
    BillService svc = new BillService();
    Context.get().scope(ContextScope.APPLICATION)
        .registerResource(BillService.CONTEXT_KEY, svc);
    return svc;
}
```
