# Security Patterns Reference (Holon Auth)

All authentication and authorization in the Holon Platform + Vaadin Flow stack uses
**Holon Auth** (`com.holon-platform.core:holon-auth`). Never use Spring Security for
business-level auth. Spring Security may appear only in filter-chain wiring if Holon Auth
requires it, with a `// FALLBACK:` comment.

---

## Core Holon Auth concepts

| Concept | Holon class | Description |
|---------|-------------|-------------|
| Authentication | `com.holonplatform.auth.Authentication` | Represents the authenticated principal (username, permissions) |
| Authenticator | `com.holonplatform.auth.Authenticator` | Verifies credentials and produces an `Authentication` |
| Realm | `com.holonplatform.auth.Realm` | Container for `Authenticator`s and `Authorizer`s |
| Authorizer | `com.holonplatform.auth.Authorizer` | Checks whether an `Authentication` has a permission |
| AuthContext | `com.holonplatform.auth.AuthContext` | Context-bound holder of the current `Authentication`; main access point for auth checks |
| Permission | `com.holonplatform.auth.Permission` | An authorization unit; string value obtainable via `permission.getPermission()` |
| Account | `com.holonplatform.auth.Account` | Built-in user model with credentials and permissions; also factory for `Authenticator` |
| AccountProvider | `com.holonplatform.auth.AccountProvider` | Functional interface: `accountId -> Optional<Account>` |
| @Authenticate | `com.holonplatform.auth.annotations.Authenticate` | Annotation for route-level authentication guard |

---

## Realm bootstrap

The `Realm` is the single auth configuration point. Define it as a `@Bean` in a
`@Configuration` class (or the main `@SpringBootApplication` class).

```java
import com.holonplatform.auth.Realm;
import com.holonplatform.auth.Account;
import com.holonplatform.auth.AccountProvider;
import com.holonplatform.auth.Credentials;
import com.holonplatform.auth.AuthContext;

@Bean
public Realm realm(AccountProvider accountProvider) {
    return Realm.builder()
        .withAuthenticator(Account.authenticator(accountProvider))  // correct factory method
        .withDefaultAuthorizer()
        .build();
}

// AccountProvider is a functional interface: accountId -> Optional<Account>
// Implement it manually — there is no DatastoreAccountProvider in Holon Auth
@Bean
public AccountProvider accountProvider(Datastore datastore) {
    return accountId -> {
        // Query the account from the datastore
        return datastore.query()
            .target(DataTarget.named("app_account"))
            .filter(DataPath.create("username").eq(accountId))
            .findOne(BeanProjection.of(AppAccount.class))
            .map(row -> Account.builder(accountId)
                .enabled(true)
                .credentials(Credentials.builder()
                    .secret(row.getPasswordHash())
                    .hashAlgorithm(Credentials.Encoder.BCRYPT.name())
                    .build())
                .withPermission("bills:view")        // assign permissions from the database row
                .build());
    };
}

// Make AuthContext available as a Vaadin session-scoped context resource
@Bean
@VaadinSessionScope
public AuthContext authContext(Realm realm) {
    return AuthContext.create(realm);
}
```

---

## Permission model

Permissions are plain `String` values granted to each `Account` via the `AccountProvider`.
Use the pattern `"resource:action"` (or a simple role name if preferred):

```
Account: ap-reviewer
  permissions: bills:view, bills:submit

Account: finance-director
  permissions: bills:view, bills:approve, bills:reject

Account: receiver
  permissions: bills:view, goods-receipts:confirm
```

The default `Authorizer` matches permissions by exact string equality.

---

## Route-level authentication guard (`@Authenticate`)

Use `@Authenticate` to require that the user is authenticated before accessing a route.
Requires `holon-vaadin-flow-navigator` on the classpath and an `AuthContext` available as
a context resource.

```java
import com.holonplatform.auth.annotations.Authenticate;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.router.Route;

@Authenticate                                   // any authenticated user may access this route
@Route("bills")
public class BillListView extends Layout { ... }

@Authenticate(redirectURI = "login")            // redirect to "login" when not authenticated
@Route("bills/approve")
public class BillApprovalView extends Layout { ... }
```

`@Authenticate` can also be placed on a `RouterLayout` class to protect all child routes:

```java
@Authenticate
public class MainLayout extends Div implements RouterLayout { }

@Route(value = "bills", layout = MainLayout.class)
public class BillListView extends Layout { ... }   // inherits @Authenticate
```

---

## Route-level authorization guard (`@RolesAllowed`)

Use `jakarta.annotation.security.RolesAllowed` to require that the authenticated user has
**at least one** of the listed permission strings.

```java
import jakarta.annotation.security.RolesAllowed;
import com.holonplatform.auth.annotations.Authenticate;
import com.holonplatform.vaadin.flow.vaadinplus.components.Layout;
import com.vaadin.flow.router.Route;

@Authenticate
@RolesAllowed("bills:view")                     // user must have "bills:view" permission
@Route("bills")
public class BillListView extends Layout { ... }

@Authenticate
@RolesAllowed({"bills:view", "bills:approve"})  // user must have "bills:view" OR "bills:approve"
@Route("bills/approve")
public class BillApprovalView extends Layout { ... }
```

Unauthorized access fires a `ForbiddenNavigationException`. The Holon Vaadin Flow
navigator checks permissions against the current `AuthContext`.

---

## Programmatic permission checks (`AuthContext`)

```java
import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.Authentication;
import com.holonplatform.auth.Permission;

// Obtain the current AuthContext from context (resolves from Vaadin session scope)
AuthContext authContext = AuthContext.require();

// Check a single permission
if (authContext.isPermitted("bills:approve")) {
    approveButton.setVisible(true);
}

// Check multiple permissions (ALL must be granted)
if (authContext.isPermitted("bills:view", "bills:approve")) {
    financePanel.setVisible(true);
}

// Get the current authenticated principal name
Optional<String> username = authContext.getAuthentication()
    .map(Authentication::getName);

// Get all permissions of the current user
Set<String> perms = authContext.requireAuthentication()
    .getPermissions().stream()
    .map(p -> p.getPermission().orElse(""))
    .filter(s -> !s.isEmpty())
    .collect(Collectors.toSet());
```

---

## JWT (stateless API, if needed)

```java
// FALLBACK: only when a stateless REST API is required alongside the Vaadin UI
import com.holonplatform.auth.jwt.JwtConfiguration;
import com.holonplatform.auth.jwt.JwtAuthenticator;
import com.holonplatform.auth.jwt.JwtSignatureAlgorithm;

JwtConfiguration jwtConfig = JwtConfiguration.builder()
    .issuer("my-app")
    .signatureAlgorithm(JwtSignatureAlgorithm.HS256)
    .sharedKey(secretKeyBytes)
    .build();

Realm realmWithJwt = Realm.builder()
    .withAuthenticator(JwtAuthenticator.builder().configuration(jwtConfig).build())
    .withAuthenticator(Account.authenticator(accountProvider))
    .withDefaultAuthorizer()
    .build();
```

---

## Spring Security filter chain (only when Holon Auth integration requires it)

```java
// FALLBACK: Holon Auth requires a Spring Security filter chain for HTTP session management
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // Delegate all business auth to Holon Auth — Spring Security only manages HTTP session
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
}
```

---

## Pre-Emit Checklist (security)

- [ ] Every `@Route` view that requires authentication has `@Authenticate` annotation
- [ ] Every `@Route` view that requires specific permissions has `@RolesAllowed` annotation
- [ ] Action buttons that require specific permissions check `authContext.isPermitted(...)` and call `setVisible(false)` if not permitted
- [ ] No `org.springframework.security.*` imports except in filter-chain wiring + `// FALLBACK:` comment
- [ ] `Realm` `@Bean` uses `Account.authenticator(accountProvider)` (not `AccountCredentialsAuthenticator`)
- [ ] `AccountProvider` is a hand-written lambda — no `DatastoreAccountProvider` exists in Holon Auth
- [ ] `AuthContext` is available as a context resource (e.g. `@VaadinSessionScope` bean)
- [ ] Permission strings in `@RolesAllowed` match those granted in `AccountProvider` exactly
