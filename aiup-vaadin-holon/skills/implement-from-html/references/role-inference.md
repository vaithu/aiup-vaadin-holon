# Role Inference Reference

Infer roles and permissions from HTML mockup elements. Use this reference during
Step 3 of the `/implement-from-html` pipeline.

---

## Signal sources (in priority order)

1. **User chip / avatar area** — text labels beside or below a user avatar in the appbar or profile menu.
2. **Role badge / tag** — `<span class="role-badge">`, `<span class="badge">`, or similar near the user chip.
3. **Action buttons** — button labels and their disabled/hidden states reveal which roles can act.
4. **Navigation items** — breadcrumb segments, sidenav links, and tab labels suggest accessible areas per role.
5. **Conditional UI regions** — sections with `data-role="..."`, `v-if="role === '...'"`, or CSS classes like `.admin-only`, `.finance-only`.

---

## Role name normalisation

Convert display names to code names used in `@Permitted` and `Realm` configuration:

| HTML display name | Normalised code name | Convention |
|------------------|---------------------|------------|
| "AP Reviewer" | `AP_REVIEWER` | UPPER_SNAKE_CASE |
| "Finance Director" | `FINANCE_DIRECTOR` | — |
| "Receiver" | `RECEIVER` | — |
| "Admin" / "Administrator" | `ADMIN` | — |
| "Super Admin" | `SUPER_ADMIN` | — |
| "Guest" / "Visitor" | `GUEST` | — |

---

## Action → permission mapping

| Button label / icon tooltip | Inferred permission |
|-----------------------------|---------------------|
| "View" / "Details" / eye icon | `<resource>:view` |
| "Create" / "New" / "Add" / plus icon | `<resource>:create` |
| "Edit" / "Modify" / pencil icon | `<resource>:edit` |
| "Delete" / "Remove" / trash icon | `<resource>:delete` |
| "Submit" / "Submit for Approval" | `<resource>:submit` |
| "Approve" / check-circle icon | `<resource>:approve` |
| "Reject" / "Decline" / x-circle icon | `<resource>:reject` |
| "Export" / download icon | `<resource>:export` |
| "Confirm Receipt" / "Receive" | `<resource>:confirm` |
| "Manage" (admin context) | `<resource>:manage` |

Replace `<resource>` with the snake_case entity name (e.g. `bill`, `purchase_order`, `goods_receipt`).

---

## Disabled button heuristic

If a button is rendered disabled in a mockup annotated for a specific role:
- The button's action permission is **not granted** to that role.
- If the same button is enabled in another role's mockup, that role has the permission.

**Example:**

| Mockup context | Button | Interpretation |
|----------------|--------|---------------|
| "AP Reviewer" screen | "Approve" is disabled | `AP_REVIEWER` does NOT have `bills:approve` |
| "Finance Director" screen | "Approve" is enabled | `FINANCE_DIRECTOR` HAS `bills:approve` |

---

## Navigation → view permissions

| Navigation element | Inferred view permission |
|-------------------|--------------------------|
| Sidenav link "Bills" | `bills:view` |
| Sidenav link "Purchase Orders" | `purchase-orders:view` |
| Tab "Pending Review" | `bills:view` (filter, not separate permission) |
| Tab "Admin Settings" | `settings:manage` |

---

## Role → permission matrix template

Fill this in during Step 3 before writing any security code:

```
Role             | Permissions
─────────────────┼─────────────────────────────────────────────────
AP_REVIEWER      | bills:view, bills:submit
FINANCE_DIRECTOR | bills:view, bills:approve, bills:reject
RECEIVER         | bills:view, goods-receipts:confirm
```

Translate to Holon Auth `Realm` bootstrap:

```java
@Bean
public Realm realm(AccountProvider accountProvider) {
    return Realm.builder()
        .withAuthenticator(AccountCredentialsAuthenticator.create(accountProvider))
        .withAuthorizer(Authorizer.create())
        .withDefaultAuthorization()
        .build();
}
```

Populate roles and permissions in the Flyway `V0NN__auth_schema.sql` seed data or
via an `ApplicationReadyEvent` bootstrapper.

---

## Ambiguity handling

- If only one role is visible in the mockup, default to a single `USER` role with all
  non-destructive permissions, and warn: `WARNING: Only one role detected — defaulting to USER. Confirm or provide role names.`
- If button disabled/enabled state is not annotated per role, assume the button is
  accessible to all authenticated users and note the assumption in the summary.
- If a section is marked `.admin-only` with no further context, infer an `ADMIN` role
  with `*:manage` permissions and note it.
