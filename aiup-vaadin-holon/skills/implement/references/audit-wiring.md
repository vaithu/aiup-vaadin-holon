# Audit Wiring Reference

This document explains how to populate the five mandatory audit fields
(`createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate`, `version`)
before calling `BeanDatastoreHelper.save(...)` when using the **Holon Datastore**
(no Spring JPA / Hibernate auditing listener active).

---

## The problem

Spring Data JPA's `@EnableJpaAuditing` + `AuditorAware<String>` mechanism is only active
when Hibernate is on the classpath and Spring JPA auditing is enabled. When using only the
Holon Datastore (no Spring JPA), the `@CreatedBy` / `@LastModifiedBy` / `@CreatedDate` /
`@LastModifiedDate` / `@Version` annotations on the bean are **not** processed automatically.
You must set them in the service layer before calling `save()`.

---

## Obtaining the current username from `AuthContext`

Use `AuthContext.require().requireAuthentication().getName()` to get the principal name
of the currently authenticated user. This resolves from the Vaadin session scope.

```java
import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.Authentication;

// Get the current username — throws AuthenticationException if no user is authenticated
String currentUser = AuthContext.require()
    .requireAuthentication()
    .getName();

// Or safely, returning a fallback for background / system operations
String currentUser = AuthContext.require()
    .getAuthentication()
    .map(Authentication::getName)
    .orElse("system");
```

---

## Audit helper utility

Declare a static helper in `shared` to centralise audit-field population:

```java
package com.example.ap.shared;

import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.Authentication;
import java.time.Instant;

/**
 * Utility for populating audit fields on domain JavaBeans before save.
 * All beans must expose the five audit fields via getters/setters:
 *   createdBy, createdDate, lastModifiedBy, lastModifiedDate, version.
 */
public final class AuditUtil {

    private AuditUtil() {}

    /** Populate audit fields for INSERT (new entity, id == null). */
    public static void stampCreate(AuditedBean bean) {
        String user = resolveCurrentUser();
        Instant now = Instant.now();
        bean.setCreatedBy(user);
        bean.setCreatedDate(now);
        bean.setLastModifiedBy(user);
        bean.setLastModifiedDate(now);
        // version starts at 0 — the Datastore increments to 1 on first save
        if (bean.getVersion() == null) {
            bean.setVersion(0L);
        }
    }

    /** Populate audit fields for UPDATE (existing entity, id != null). */
    public static void stampUpdate(AuditedBean bean) {
        String user = resolveCurrentUser();
        bean.setLastModifiedBy(user);
        bean.setLastModifiedDate(Instant.now());
        // Do NOT reset createdBy / createdDate on updates
    }

    private static String resolveCurrentUser() {
        return AuthContext.require()
            .getAuthentication()
            .map(Authentication::getName)
            .orElse("system");   // fallback for batch / scheduled operations
    }
}
```

Declare an `AuditedBean` interface so `AuditUtil` can work with any domain bean:

```java
package com.example.ap.shared;

import java.time.Instant;

/** Marker interface for beans that carry the five standard audit fields. */
public interface AuditedBean {
    String  getCreatedBy();       void setCreatedBy(String v);
    Instant getCreatedDate();     void setCreatedDate(Instant v);
    String  getLastModifiedBy();  void setLastModifiedBy(String v);
    Instant getLastModifiedDate();void setLastModifiedDate(Instant v);
    Long    getVersion();         void setVersion(Long v);
}
```

Every domain bean implements `AuditedBean`:

```java
@DataPath("bill")
public class Bill implements AuditedBean {

    // ... business fields ...

    // ── Audit & version ──
    @CreatedBy  @DataPath("created_by")         private String  createdBy;
    @CreatedDate @DataPath("created_date")       private Instant createdDate;
    @LastModifiedBy @DataPath("last_modified_by")private String  lastModifiedBy;
    @LastModifiedDate @DataPath("last_modified_date") private Instant lastModifiedDate;
    @Version @DataPath("version")                private Long    version;

    // getters / setters ...
}
```

---

## Service save pattern

```java
package com.example.ap.bill;

import com.example.ap.shared.AuditUtil;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastoreHelper;

public class BillService {

    private final BeanDatastoreHelper<Bill> helper;

    public BillService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(BeanDatastore.of(datastore), Bill.class);
    }

    /** Insert or update a bill with automatic audit-field population. */
    public void save(Bill bill) {
        if (bill.getId() == null) {
            AuditUtil.stampCreate(bill);
            helper.save(bill);   // INSERT — id is populated back after save
        } else {
            AuditUtil.stampUpdate(bill);
            helper.save(bill);   // UPDATE — version checked by Datastore
        }
    }

    public Optional<Bill> findById(Long id) {
        return helper.findOne(BillModel.ID.eq(id));
    }

    public Stream<Bill> findSlice(int offset, int length, QueryFilter filter) {
        if (filter != null) {
            return helper.findSlice(offset, length, filter, BillModel.INVOICE_DATE.desc());
        }
        return helper.findSlice(offset, length, BillModel.INVOICE_DATE.desc());
    }

    public void delete(Bill bill) {
        helper.delete(bill);
    }
}
```

---

## Registering the service as a Spring `@Bean`

```java
// In @SpringBootApplication or a @Configuration class
@Bean
public BillService billService(Datastore datastore) {
    return new BillService(datastore);
}
```

---

## Background / scheduled operations

When audit fields must be set outside a Vaadin request thread (batch jobs, `@Scheduled`
tasks), there is no active `AuthContext`. Use the `"system"` fallback — `AuditUtil`
already returns `"system"` when no `Authentication` is present.

```java
@Scheduled(cron = "0 0 2 * * *")   // nightly batch
public void archiveOldBills() {
    Stream<Bill> old = billService.findOlderThan(90);
    old.forEach(bill -> {
        bill.setStatus("ARCHIVED");
        AuditUtil.stampUpdate(bill);   // sets lastModifiedBy = "system"
        helper.save(bill);
    });
}
```

---

## Pre-Emit Checklist (audit wiring)

- [ ] Every domain bean implements `AuditedBean` (or exposes the five audit-field getters/setters)
- [ ] `AuditUtil.stampCreate(bean)` is called for every INSERT path (id == null)
- [ ] `AuditUtil.stampUpdate(bean)` is called for every UPDATE path (id != null)
- [ ] `createdBy` / `createdDate` are NOT overwritten on updates
- [ ] `version` is initialised to `0L` for new beans (the Datastore increments it to 1 on first save)
- [ ] Background / scheduled operations tolerate missing `AuthContext` via the `"system"` fallback
- [ ] `AuditUtil` lives in `shared` — not in a feature package
