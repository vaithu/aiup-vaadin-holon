# Architecture: Package Structure

The Holon Platform + Vaadin Flow application follows a **package-by-feature** layout.
Each feature is a top-level package under the application root.
Inside every feature package all classes are **co-located in one flat package** — no `ui/`
or `domain/` sub-packages are created.

Cross-cutting concerns that are not part of a single feature (application shell, auth
configuration, shared helpers such as `AuditUtil`, `AuditedBean`) live in a dedicated
`shared` package.

---

## Layering inside a feature

The call flow inside a feature is:

```
View  →  Service  →  BeanDatastoreHelper<T>  →  Datastore
```

with one important rule: **the service layer only exists when it carries real business
logic.** If the use case is pure data access — read rows and render them; insert one row
and navigate back — the view calls `BeanDatastoreHelper<T>` through a minimal service
wrapper. An empty service that only forwards `findAll()` adds indirection without value;
omit it and have the view inject the helper directly only if no business logic is needed.

Add a dedicated service the moment the feature needs any of the following:

- Coordinating writes across more than one `DataTarget` inside a `@Transactional` block
- Validation or invariants that span multiple entities (e.g. "a member may not have more
  than five open loans")
- Side effects beyond the database (sending mail, publishing events, calling another system)
- Logic that must be reused from more than one view

For a book-library application that means:

- **Catalog search** is a read-only query → `CatalogView` calls a thin `BookService` that
  delegates directly to `BeanDatastoreHelper<Book>`; no orchestration.
- **Borrow / return** must check copy availability and mutate both `loan` and `book_copy`
  rows inside a transaction → `BorrowDialog` calls `LoanService`, which is a
  `@Service`-annotated class that orchestrates `BeanDatastoreHelper<Loan>` and
  `BeanDatastoreHelper<BookCopy>`.

### Why co-location?

- **Locality.** Everything needed to understand or change a feature lives in one place.
  There is no jumping between `controllers/`, `services/`, `repositories/`, and `views/`
  packages to follow a single flow.
- **Replaceability.** A feature can be deleted, rewritten, or extracted with no ripple
  effects, because nothing outside its package depends on its internals.
- **Pairs naturally with AIUP.** Each use case spec maps to one feature package. The
  `/implement` skill drops the bean, model interface, service, and view into
  `<feature>` with no further routing decisions.

---

## Package tree

```
com.example.<app>
├── shared                         # cross-cutting concerns
│   ├── MainLayout.java            # AppShell / RouterLayout, navigation sidebar
│   ├── LoginView.java             # @AnonymousAllowed login screen
│   ├── SecurityConfig.java        # Realm @Bean, AccountProvider @Bean
│   ├── AppAccount.java            # JavaBean mapped to app_account table
│   ├── AuditUtil.java             # stampCreate / stampUpdate helpers
│   ├── AuditedBean.java           # interface every domain bean implements
│   ├── NotFoundView.java          # 404 error view
│   ├── InternalErrorView.java     # 500 error view
│   └── AccessDeniedView.java      # 403 error view
│
├── catalog                        # feature: browse / search books
│   ├── Book.java                  # JavaBean  (@DataPath, @Identifier, @Caption, AuditedBean)
│   ├── BookModel.java             # Model interface (BeanPropertySet, PathProperty constants)
│   ├── BookService.java           # BeanDatastoreHelper<Book> wrapper
│   └── CatalogView.java           # @Route view — listing, search filter
│
├── loan                           # feature: borrow / return books
│   ├── Loan.java
│   ├── LoanModel.java
│   ├── LoanService.java           # @Service — orchestrates loan + book_copy tables
│   ├── ActiveLoansView.java       # librarian view: all active loans
│   ├── MyLoansView.java           # member view: own loans
│   ├── BorrowDialog.java          # dialog: check availability, insert loan
│   └── ReturnDialog.java          # dialog: mark loan returned
│
└── member                         # feature: patron profiles
    ├── Member.java
    ├── MemberModel.java
    ├── MemberService.java         # @Service — inserts app_account + member in one transaction
    └── MemberView.java
```

Feature names are nouns from the entity model and use case diagram, not verbs from the
use cases. _Borrowing_ and _returning_ both live in `loan` because they operate on the
same aggregate.

---

## Co-location rules

1. **No feature-to-feature imports from one view into another feature's service.**
   If `CatalogView` needs loan data (e.g. "is this book currently borrowed?"), it calls a
   method on `LoanService` — not directly into any class inside `loan` that is not the
   service entry point.

2. **Views may depend on services within the same feature; services must not depend on
   views.** This keeps the data layer headless and independently testable.

3. **Skip the service when there is no business logic.** A view may hold a
   `BeanDatastoreHelper<T>` directly if the only operations are `findAll` / `findById` /
   `save` / `delete` with no invariants to enforce. Introduce a service the moment any of
   the criteria in the layering section apply.

4. **Once a service exists, the view must go through it.** Mixed access — some calls
   through the service, others bypassing it — defeats the invariant guarantees.

5. **No `controller`, `service`, `repository`, `ui`, or `domain` top-level packages.**
   The split is by feature first; the role of a class is expressed by its name and its
   position inside the feature package.

6. **`shared` is for things used by two or more features.** A class used only by one
   feature belongs in that feature, not in `shared`.

7. **Beans and model interfaces live with the code that owns them.** A `Book` bean and
   `BookModel` interface returned by the catalog service live in `catalog`. If the loan
   feature needs the book's copy count, it imports from `catalog` — it does not duplicate
   the bean.

---

## Identity and domain separation

Authentication and authorisation are part of the architecture, not a separate feature.

- **Identity is separate from the domain.** The `app_account` table holds authentication
  data only: `id`, `username`, `password_hash`, `role` (`MEMBER` or `LIBRARIAN`),
  `created_at`. No name, no email, no profile data — those belong to a domain entity.

- **`member` is a domain entity, not the identity.** The `member` table carries the
  patron's profile (name, email, …) and a `user_id` foreign key into `app_account`.
  A librarian is an `app_account` with role `LIBRARIAN` and **no** `member` row —
  librarians do not borrow books, so they do not need a patron profile.

- **Loans reference `member.id`, not `app_account.id`.** The path from "currently
  logged-in user" to "their loans" goes through `member.user_id`.

- **No self-service sign-up.** A librarian creates member accounts (a domain action that
  inserts an `app_account` row and a `member` row in the same `@Transactional` method).
  The first librarian's `app_account` row is seeded on startup so the app is usable on
  first boot.

---

## Security: Holon Auth

Authentication uses **Holon Auth** — never Spring Security for business-level checks.

```java
// shared/SecurityConfig.java
@Bean
public Realm realm(AccountProvider accountProvider) {
    return Realm.builder()
        .withAuthenticator(Account.authenticator(accountProvider))
        .withDefaultAuthorizer()
        .build();
}

@Bean
public AccountProvider accountProvider(Datastore datastore) {
    BeanDatastoreHelper<AppAccount> helper =
        BeanDatastoreHelper.of(AppAccount.class, datastore);
    return accountId -> helper
        .findOne(AppAccountModel.USERNAME.eq(accountId))
        .map(row -> Account.builder(accountId)
            .enabled(true)
            .credentials(Credentials.builder()
                .secret(row.getPasswordHash())
                .hashAlgorithm(Credentials.Encoder.BCRYPT.name())
                .build())
            .withPermission(row.getRole().toLowerCase() + ":access")
            .build());
}
```

### Permissions

Use the pattern `"resource:action"` (or a simple role slug if preferred):

| Role         | Permissions                                               |
| ------------ | --------------------------------------------------------- |
| `MEMBER`     | `catalog:view`, `loan:borrow`, `loan:return`, `loan:view` |
| `LIBRARIAN`  | all of the above + `member:manage`, `loan:manage`         |

### Route-level guards

```java
@Authenticate                              // requires a logged-in user
@RolesAllowed("catalog:view")              // user must have this permission
@Route(value = "catalog", layout = MainLayout.class)
public class CatalogView extends Layout { ... }

@Authenticate
@RolesAllowed("loan:manage")              // librarian-only
@Route(value = "loans/active", layout = MainLayout.class)
public class ActiveLoansView extends Layout { ... }

@AnonymousAllowed
@Route("login")
public class LoginView extends Layout { ... }
```

**Rules:**

- `@Authenticate` + `@RolesAllowed("...")` on every secured route — never `@PermitAll`
  (it hides the role intent and grants access to any authenticated user).
- `@AnonymousAllowed` only on the login view.
- `@RolesAllowed` on `MainLayout` is **not** used — apply it per view so that the
  navigator can hide menu items based on per-view permissions.
- Role-aware scoping in the domain (e.g. "a member sees only their own loans") lives in
  the service, not the view, reading the current principal from `AuthContext.require()`.

```java
// In LoanService — scoping by role without branching in the view
public List<Loan> findLoansForCurrentUser() {
    AuthContext ctx = AuthContext.require();
    if (ctx.isPermitted("loan:manage")) {
        return helper.findAll();                         // librarian: all loans
    }
    String username = ctx.requireAuthentication().getName();
    Long memberId = resolveMemberId(username);
    return helper.findAll(LoanModel.MEMBER_ID.eq(memberId));  // member: own loans only
}
```

---

## Persistence: Holon Datastore + BeanDatastoreHelper

All database access goes through `BeanDatastoreHelper<T>` — never jOOQ, Spring Data, or
raw JPA.

Every feature needs two artefacts:

### 1 · JavaBean

```java
@DataPath("book")
public class Book implements AuditedBean {

    @Identifier
    @DataPath("id")
    private Long id;

    @Caption(value = "Title", messageCode = "book.title")
    @NotBlank
    @DataPath("title")
    private String title;

    @Caption(value = "Author", messageCode = "book.author")
    @NotBlank
    @DataPath("author")
    private String author;

    // audit fields (mandatory on every domain bean — see bean-model.md)
    private String  createdBy;
    private Instant createdDate;
    private String  lastModifiedBy;
    private Instant lastModifiedDate;
    @Version private Long version;

    // standard getters / setters
}
```

### 2 · Model interface

```java
public interface BookModel {
    DataTarget<String>       TARGET       = DataTarget.named("book");
    BeanPropertySet<Book>    PROPERTY_SET = BeanPropertySet.create(Book.class);
    NumericProperty<Long>    ID           = PROPERTY_SET.propertyNumeric("id");
    StringProperty           TITLE        = PROPERTY_SET.propertyString("title");
    StringProperty           AUTHOR       = PROPERTY_SET.propertyString("author");

    PropertySet LISTING = PropertySet.builderOf(ID, TITLE, AUTHOR).withIdentifier(ID).build();
    PropertySet FORM    = PropertySet.builderOf(TITLE, AUTHOR).build();
}
```

### 3 · Service

```java
@Service
@Transactional
public class BookService {

    private final BeanDatastoreHelper<Book> helper;

    public BookService(Datastore datastore) {
        this.helper = BeanDatastoreHelper.of(Book.class, datastore);
    }

    public List<Book> findAll() { return helper.findAll(); }

    public Optional<Book> findById(Long id) {
        return helper.findOne(BookModel.ID.eq(id));
    }

    public List<Book> findSlice(int offset, int length, QueryFilter filter) {
        return helper.findSlice(offset, length, filter);
    }

    public void save(Book book) {
        if (book.getId() == null) {
            AuditUtil.stampCreate(book);
            helper.insert(book);
        } else {
            AuditUtil.stampUpdate(book);
            helper.update(book);
        }
    }

    public void delete(Long id) { helper.delete(BookModel.ID.eq(id)); }
}
```

Use a raw `BeanDatastore` / `Datastore` query chain only when `BeanDatastoreHelper` has
no equivalent method (multi-table joins, window functions, CTEs), with a
`// FALLBACK: BeanDatastoreHelper has no equivalent for <thing>` comment.

---

## UI: Holon Vaadin Flow components

All UI components come from `com.holon-platform.vaadin` — never raw Vaadin core.

| Need | Holon API |
| ---- | --------- |
| Data grid | `Components.listing(T.class)` → `ListingBundleBuilder<T>` |
| Entity form | `EntityFormPanel.bean(T.class)` |
| Button | `ButtonBuilder.create().primary()` / `.secondary()` / `.error()` |
| Notification | `NotificationUtil.notificationSuccess(...)` / `NotificationUtil.notificationError(...)` |
| Navigation | `Navigator.get().navigateTo(MyView.class)` |

```java
@Authenticate
@RolesAllowed("catalog:view")
@Route(value = "catalog", layout = MainLayout.class)
public class CatalogView extends Layout {

    private final BookService svc;

    public CatalogView(BookService svc) {
        this.svc = svc;
    }

    @OnShow
    void onShow() {
        removeAll();
        ListingBundle<Book> bundle = Components.<Book>listing(Book.class)
            .fetch(q -> svc.findSlice(q.getOffset(), q.getLength(), null))
            .emptyState()
            .build();
        add(bundle);
    }
}
```

---

## ArchUnit enforcement

Add ArchUnit tests (`src/test/java/.../architecture/`) to enforce co-location rules:

- No class outside a feature package imports from another feature's non-service class.
- `@Route` views must not call `Datastore` / `BeanDatastoreHelper` directly when a
  service already exists for that feature (mix-prevention rule).
- `shared` classes must not import from any feature package.

These tests run as part of `./mvnw test` so a violation breaks the build instead of
relying on code review discipline.
