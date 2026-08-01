# Security Patterns Reference (Holon Auth)

All authentication and authorization in the Holon Platform + Vaadin Flow stack uses
**Holon Auth** (`com.holon-platform.auth:holon-auth`). Never use Spring Security for
business-level auth. Spring Security may appear only in filter-chain wiring if Holon Auth
requires it, with a `// FALLBACK:` comment.

---

## Core Holon Auth concepts

| Concept | Holon class | Description |
|---------|-------------|-------------|
| Authentication | `com.holonplatform.auth.Authentication` | Represents the authenticated principal (username, roles, permissions) |
| Authenticator | `com.holonplatform.auth.Authenticator` | Verifies credentials and produces an `Authentication` |
| Realm | `com.holonplatform.auth.Realm` | Container for `Authenticator`s and `Authorizer`s |
| Authorizer | `com.holonplatform.auth.Authorizer` | Checks whether an `Authentication` has a role or permission |
| AuthContext | `com.holonplatform.auth.AuthContext` | Thread-bound holder of the current `Authentication`; access point for auth checks |
| Permission | `com.holonplatform.auth.Permission` | An authorization unit, typically expressed as `"resource:action"` |
| @Permitted | `com.holonplatform.auth.annotations.Permitted` | Annotation for route-level or method-level permission guards |

---

## Realm bootstrap

The `Realm` is the single auth configuration point. Define it as a `@Bean` in a
`@Configuration` class (or the main `@SpringBootApplication` class).

```java
import com.holonplatform.auth.Realm;
import com.holonplatform.auth.Authenticator;
import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.keys.AccountCredentialsAuthenticator;

@Bean
public Realm realm(AccountProvider accountProvider) {
    return Realm.builder()
        .withAuthenticator(
            AccountCredentialsAuthenticator.create(accountProvider))
        .withAuthorizer(Authorizer.create())
        .withDefaultAuthorization()
        .build();
}

// AccountProvider example (loaded from Holon Auth tables in the database)
@Bean
public AccountProvider accountProvider(Datastore ds) {
    // Use Holon Auth JDBC AccountProvider backed by holon_account + holon_role + holon_permission tables
    return DatastoreAccountProvider.create(ds);
}
```

---

## Role and permission model

Define roles and their permissions in the `Realm` bootstrap or via the database
(Holon Auth JDBC schema from `V0NN__auth_schema.sql`):

```
Role: AP_REVIEWER
  permissions: bills:view, bills:submit

Role: FINANCE_DIRECTOR
  permissions: bills:view, bills:approve, bills:reject

Role: RECEIVER
  permissions: bills:view, goods-receipts:confirm
```

Permissions follow the pattern `"resource:action"`. Roles group permissions for
assignment to users.

---

## Route-level guard (`@Permitted`)

```java
import com.holonplatform.auth.annotations.Permitted;
import com.vaadin.flow.router.Route;

@Route("bills")
@Permitted("bills:view")            // user must have "bills:view" permission to access this route
public class BillListView extends VerticalLayout { ... }

@Route("bills/approve")
@Permitted({"bills:view", "bills:approve"})   // requires BOTH permissions
public class BillApprovalView extends VerticalLayout { ... }
```

Holon's Vaadin Flow integration enforces `@Permitted` before the view is rendered.
Unauthorized access redirects to a login / access-denied view.

---

## Programmatic permission checks (`AuthContext`)

```java
import com.holonplatform.auth.AuthContext;

// Check a single permission
if (AuthContext.require().isPermitted("bills:approve")) {
    approveButton.setVisible(true);
}

// Check a role
if (AuthContext.require().isInRole("FINANCE_DIRECTOR")) {
    financeOnlyPanel.setVisible(true);
}

// Require a permission — throws AuthenticationException if not satisfied
AuthContext.require().requirePermission("bills:approve");

// Get the current authenticated principal name
Optional<String> username = AuthContext.require()
    .getAuthentication()
    .map(Authentication::getName);

// Get all roles of current user
Set<String> roles = AuthContext.require()
    .getAuthentication()
    .map(auth -> auth.getParameters().stream()
        .filter(p -> "role".equals(p.getName()))
        .map(p -> p.getValue().toString())
        .collect(Collectors.toSet()))
    .orElse(Collections.emptySet());
```

---

## JWT (stateless API, if needed)

```java
// FALLBACK: only when a stateless REST API is required alongside the Vaadin UI
// FALLBACK: no Holon equivalent for JWT generation without holon-auth-jwt
import com.holonplatform.auth.jwt.JwtConfiguration;
import com.holonplatform.auth.jwt.JwtAuthenticator;

JwtConfiguration jwtConfig = JwtConfiguration.build()
    .issuer("my-app")
    .sharedKeyAlgorithm("HmacSHA256", secretKeyBytes)
    .build();

Realm realmWithJwt = Realm.builder()
    .withAuthenticator(JwtAuthenticator.builder().configuration(jwtConfig).build())
    .withAuthenticator(AccountCredentialsAuthenticator.create(accountProvider))
    .withAuthorizer(Authorizer.create())
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

- [ ] Every `@Route` view that requires authentication has `@Permitted` annotation
- [ ] Action buttons that require specific permissions check `AuthContext.require().isPermitted(...)` and set `setVisible(false)` if not permitted
- [ ] No `org.springframework.security.*` imports except in filter-chain wiring + `// FALLBACK:` comment
- [ ] `Realm` `@Bean` is present and configures all required roles / permissions
- [ ] Role/permission names in `@Permitted` match the names in the `Realm` bootstrap exactly
