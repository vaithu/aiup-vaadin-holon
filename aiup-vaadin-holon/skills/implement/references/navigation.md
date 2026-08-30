# Navigation Reference (Holon Navigator)

All programmatic navigation in the Holon Platform + Vaadin Flow stack uses
**`com.holonplatform.vaadin.flow.navigator.Navigator`**.
Never call `UI.getCurrent().navigate(...)` directly — `Navigator.get()` is the single
navigation entry point for all routing operations.

---

## Quick reference

| Goal | API |
|------|-----|
| Navigate to a view class | `Navigator.get().navigateTo(BillListView.class)` |
| Navigate to a route string | `Navigator.get().navigateTo("bills")` |
| Navigate back (browser history) | `Navigator.get().navigateBack()` |
| Navigate to previous Holon route | `Navigator.get().navigateToPrevious()` |
| Navigate to home / default route | `Navigator.get().navigateToDefault()` |
| Navigate with URL parameters | `Navigator.get().navigateTo(DetailView.class, Map.of("id", 42L))` |
| Build a URL (for links) | `Navigator.get().getUrl(BillListView.class)` |

---

## Basic navigation

```java
import com.holonplatform.vaadin.flow.navigator.Navigator;

// Navigate by view class (preferred — compile-time safe)
Navigator.get().navigateTo(BillListView.class);

// Navigate by route string (only when the class is not available at compile time)
Navigator.get().navigateTo("bills");

// Go back one step in Vaadin's navigation history
Navigator.get().navigateBack();

// Go back to the previous Holon-tracked route
Navigator.get().navigateToPrevious();

// Navigate to the application default / home route
Navigator.get().navigateToDefault();
```

---

## Navigating with URL parameters

Pass URL query parameters as a `Map<String, Object>`. Values are serialised to strings
and appended as `?key=value` pairs.

```java
// Navigate to /bills/detail?id=42
Navigator.get().navigateTo(BillDetailView.class, Map.of("id", 42L));

// Multiple parameters
Navigator.get().navigateTo(BillListView.class,
    Map.of("status", "PENDING_REVIEW", "page", 1));
```

---

## Receiving URL parameters in a view (`@QueryParameter`, `@OnShow`)

Use `@QueryParameter` on a field to bind a URL query parameter by name.
Use `@OnShow` on a no-arg method to be called **after** all parameters have been injected
(similar to `BeforeEnterEvent` but cleaner for parameter-driven init).

```java
package com.example.ap.bill;

import com.holonplatform.auth.annotations.Authenticate;
import com.holonplatform.vaadin.flow.navigator.annotations.OnShow;
import com.holonplatform.vaadin.flow.navigator.annotations.QueryParameter;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Authenticate
@RolesAllowed("bills:view")
@Route(value = "bills", layout = MainLayout.class)
public class BillListView extends Layout {

    @QueryParameter("status")          // bound from ?status=PENDING_REVIEW
    private String filterStatus;

    @QueryParameter("page")
    private Integer page;              // null if the parameter is absent

    private final BillService svc;
    private ListingBundle<Bill> bundle;

    public BillListView(BillService svc) {
        this.svc = svc;
        // Do NOT access @QueryParameter fields here — they are null at constructor time.
        // Build only the skeleton layout; data loading happens in @OnShow.
        setSizeFull();
        setPadding(false);
    }

    @OnShow
    void onShow() {
        // Called after @QueryParameter fields are injected.
        // Safe to use filterStatus, page, etc. here.
        removeAll();
        bundle = Components.<Bill>listing(Bill.class)
            .fetch(q -> svc.findSlice(q.getOffset(), q.getLength(),
                filterStatus != null
                    ? BillModel.STATUS.eq(filterStatus)
                    : null))
            .emptyState()
            .build();
        add(bundle);
    }
}
```

**Key rules:**
- Never read `@QueryParameter` fields inside the constructor — they are still `null`.
- Always move parameter-driven logic into an `@OnShow` method.
- `@OnShow` fires every time the user navigates **to** this view (including forward navigation after `navigateBack`).

---

## Navigation lifecycle (`BeforeEnterObserver`)

Use `BeforeEnterObserver` when you need to reroute or redirect before the view is rendered
(e.g. permission check that cannot be expressed with `@RolesAllowed`).

```java
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.holonplatform.auth.AuthContext;

@Authenticate
@Route(value = "bills/approve", layout = MainLayout.class)
public class BillApprovalView extends Layout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!AuthContext.require().isPermitted("bills:approve")) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }
}
```

---

## URL synchronisation with `MasterDetailLayout`

When using `MasterDetailLayout`, pass `.withUrlSync(...)` to keep the selected item's ID
in the URL so that the browser back-button and bookmarks work correctly.

```java
MasterDetailLayout<Bill> layout = Components.<Bill>masterDetail(Bill.class)
    .master(master -> master
        .listing(svc::findAll)
        .columns("vendorName", "invoiceDate", "status"))
    .detail(detail -> detail
        .content(bill -> buildDetailView(bill)))
    .withUrlSync(
        bill -> String.valueOf(bill.getId()),           // bean → URL segment
        id   -> svc.findById(Long.valueOf(id))          // URL segment → bean (Optional)
    )
    .build();
```

---

## Building typed links

Use `Navigator.get().getUrl(...)` to get the absolute or relative URL for a view class.
Useful for breadcrumbs, email links, and `RouterLink` labels.

```java
// URL for a plain view
String billsUrl = Navigator.get().getUrl(BillListView.class);

// URL with parameters
String detailUrl = Navigator.get().getUrl(BillDetailView.class, Map.of("id", 42L));
```

---

## Navigating from inside a service or utility class

`Navigator.get()` resolves the `Navigator` from the current Vaadin UI session scope.
It is safe to call from any Vaadin request thread (inside a `UI.access(...)` callback for
background-thread pushes).

```java
// Background thread / push
ui.access(() -> Navigator.get().navigateTo(BillListView.class));
```

---

## Pre-Emit Checklist (navigation)

- [ ] All programmatic navigation uses `Navigator.get().navigateTo(...)` — never `UI.getCurrent().navigate(...)`
- [ ] `@QueryParameter` fields are never read in the constructor — logic moved to `@OnShow`
- [ ] `@OnShow` rebuilds/refreshes data-driven content so re-navigation with new parameters re-loads correctly
- [ ] `BeforeEnterObserver.beforeEnter` used only for permission reroutes not expressible via `@RolesAllowed`
- [ ] `MasterDetailLayout` uses `.withUrlSync(...)` when the detail selection should survive page refresh / back navigation
