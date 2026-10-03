---
name: holon-vaadin-test
description: >
  Creates server-side Vaadin Browserless unit tests using the browserless-test-spring
  module (SpringBrowserlessTest + @SpringBootTest) for Holon Vaadin Flow views
  (PropertyForm, PropertyListing). Seeds via Flyway test migrations. Also tests
  role-gated buttons appear/hide correctly using Holon Auth (Spring Security supported
  as a documented fallback). Use when the user asks to "write Vaadin unit tests",
  "test the Holon Vaadin view", "write browserless tests", "test the PropertyForm",
  "test the PropertyListing", "verify role-based visibility", or mentions Vaadin
  Browserless, SpringBrowserlessTest, browserless-test-spring, server-side UI tests
  for UC-XXX, or testing Holon Vaadin components without a browser.
---

# Holon Vaadin Test (Browserless)

## Prerequisites

Before writing any tests, verify that the view implementation for the use case exists:

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-XXX-*.md` (the use case being tested) | `/use-case-spec` |
| Vaadin view class for the use case (e.g. `src/main/java/**/*View.java`) | `/implement` |
| `src/main/resources/db/migration/V*.sql` (Flyway migration scripts) | `/flyway-migration` |

If any artifact is missing, **stop** and tell the user which skill to run first:
> "No Vaadin view implementation found — run `/implement UC-XXX` first, then re-run `/holon-vaadin-test`."  
> "No Flyway migration scripts found — run `/flyway-migration` first." (if no SQL migrations exist)

Do not attempt to generate tests against unimplemented views.

## Instructions

Create server-side Vaadin Browserless unit tests for the view(s) in use case `$ARGUMENTS`
using `com.vaadin:browserless-test-spring`. Test classes extend `SpringBrowserlessTest`
and are annotated with `@SpringBootTest`, so the full Spring context (Datastore, services,
security) is available. Tests run in-memory without a browser, exercising the real Holon
Vaadin Flow component tree.

Seed test data via Flyway test migrations in `src/test/resources/db/migration/`.

Also test **role-gated visibility**: verify that buttons and sections requiring specific
Holon Auth permissions appear or hide correctly for different roles. When the target app
secures views with Spring Security instead of Holon Auth, use the documented Spring
Security fallback below.

## Traceability naming contract

If `/traceability-sensors` is installed, the test class name and the `@UseCase`
annotation are **not** cosmetic — the sensors parse them and fail the build when
they disagree with `docs/use_cases/`.

| Rule | Form |
|---|---|
| Class name | `UC<NNN><Name>Test` — e.g. `UC004BillFormViewTest` |
| Phase | Surefire (`*Test`), so these run in `mvn test` |
| Every test method | `@UseCase(id = "UC-NNN", scenario = "<exact flow heading>")` |
| A method covering a business rule | add `businessRules = "BR-NNN"` |

`scenario` must match the heading in the specification **character for
character** — `"Main"` for the main success scenario, otherwise the alternative
or exception flow heading as written. `UseCaseTraceabilityTest` reports the
exact mismatch, so when it fails, copy the heading from the document rather
than guessing.

A use case whose `**Status:**` is `Done` or `Tested` must have a test for every
flow it declares. That is why the status is set **after** the tests exist, not
before — see `/coverage-check`.

## Quality Gate Guardrail

When testing starts, this skill also enforces the SonarQube-equivalent **quality
gate** defined in [`../../rules/quality-gate.md`](../../rules/quality-gate.md).
Before emitting the test summary:

1. Verify the target project's `pom.xml` has the `quality` profile and the `config/`
   rulesets. If missing, scaffold them from the reference implementation in
   [`demo/crm-minimal`](../../../demo/crm-minimal).
2. Run the tests **and** the gate together: `mvn -Pquality verify`.
3. Report bug/smell/duplication counts, coverage %, and CVE findings alongside the
   test results (reports under `target/`).

The gate ships in **report mode** (non-failing) — see the guardrail doc for the
toggles that turn it into a hard gate once the baseline is triaged.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.**

- **No browser required** — `browserless-test-spring` (`SpringBrowserlessTest`) runs server-side
- **No Mockito for Datastore** — use a real Testcontainers DB (or in-memory H2 if simpler)
- **No `@Autowired` in production code** — inject via constructors; in tests, resolve beans from the Spring context or `@Autowired` fields are acceptable in the test class only
- Seed test data with Flyway migrations in `src/test/resources/db/migration/`
- Auth context for role-gated tests: use Holon `AuthContext` to bind the current user's permissions (**primary**). If the app uses Spring Security, seed the `SecurityContextHolder` instead (**fallback**, see below). In browserless there is no request-bound `AuthContext`, so **build an `AuthContext` and store it on the `VaadinSession` under `AuthContext.CONTEXT_KEY`** (Holon's `VaadinSessionScope` reads it from the session) — see the example below
- Query the component tree with **`find(Class)`** → `ComponentQuery` terminals `.first()`, `.single()`, `.all()`; simulate user actions with `test(component)`. (The older `$`/`$view` API belongs to browserless 1.0.x and does **not** work with `browserless-test-spring` 1.1.x on Vaadin 25.2 — always use `find(...)`.)
- Every `@Route` view under a parent `@Route(...) layout` must not be **more permissive** than that layout, or Vaadin's `AnnotatedViewAccessChecker` denies it ("broader access than the layout") and `navigate(...)` reroutes to an error page. Annotate the layout at least as permissively as its most-permissive child (e.g. a `@PermitAll` view needs a `@PermitAll` layout)

## Pre-Emit Checklist

- [ ] Test class extends `com.vaadin.browserless.SpringBrowserlessTest` and is annotated `@SpringBootTest`
- [ ] `@ViewPackages(packages = "...")` present if the `@Route` views are not in the test's own package
- [ ] Flyway test migrations in `src/test/resources/db/migration/`
- [ ] No mocked Datastore (use in-memory H2 or Testcontainers)
- [ ] Role-gated visibility tests bind auth with Holon `AuthContext` stored on the `VaadinSession` under `AuthContext.CONTEXT_KEY` (or seed `SecurityContextHolder` for Spring Security apps) before `navigate(...)`
- [ ] Component queries use `find(Class)` (not `$`/`$view`)
- [ ] The parent `layout` of each tested view is at least as permissive as the view's own access annotation
- [ ] Tests cover: view renders correctly, form submit, listing shows data, role-restricted buttons visible/hidden

## Test structure

```java
package com.example.ap.ui;

import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.Authentication;
import com.holonplatform.auth.AuthenticationToken;
import com.holonplatform.auth.Authenticator;
import com.holonplatform.auth.Realm;
import com.holonplatform.auth.token.AccountCredentialsToken;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.ap.traceability.UseCase;   // see /traceability-sensors

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ViewPackages(packages = "com.example.ap.ui")   // scan package for @Route views
class UC004BillFormViewTest extends SpringBrowserlessTest {

    @AfterEach
    void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(AuthContext.CONTEXT_KEY, null);
        }
    }

    /**
     * Build an authenticated Holon AuthContext and bind it onto the VaadinSession.
     * In browserless there is no HTTP request, so Holon's VaadinSessionScope resolves
     * the AuthContext from this session attribute (AuthContext.CONTEXT_KEY). Do this
     * BEFORE navigate(...) so the Holon navigation guard sees an authenticated user.
     */
    private void loginAs(String user, String... permissions) {
        Authentication authentication =
                Authentication.builder(user).withPermission(String.join(",", permissions)).build();
        Realm realm = Realm.builder()
                .withDefaultAuthorizer()
                .withAuthenticator(Authenticator.create(AuthenticationToken.class, token -> authentication))
                .build();
        AuthContext authContext = AuthContext.create(realm);
        authContext.authenticate(AccountCredentialsToken.create(user, "n/a"));
        VaadinSession.getCurrent().setAttribute(AuthContext.CONTEXT_KEY, authContext);
    }

    @Test
    @DisplayName("New bill form renders and submits to the Datastore")
    @UseCase(id = "UC-004", scenario = "Main")
    void formRendersAndSubmits() {
        // Holon Auth: bind the current user BEFORE navigating (primary security model)
        loginAs("reviewer@example.com", "bills:view", "bills:submit");

        BillFormView view = navigate(BillFormView.class);

        // Query the tree with find(...), then drive components with test(...)
        TextField vendor = find(TextField.class).withId("vendor").first();
        test(vendor).setValue("Acme Ltd");

        Button save = find(Button.class).withText("Save").first();
        test(save).click();

        // assert persisted via the injected service / Datastore
    }

    @Nested
    @DisplayName("Role-gated visibility (Holon Auth)")
    class RoleGatedVisibility {

        @Test
        @DisplayName("Approve button is visible for the approver permission")
        void approveVisibleForApprover() {
            loginAs("director@example.com", "bills:approve");

            navigate(BillListView.class);

            assertFalse(find(Button.class).withText("Approve").all().isEmpty());
        }

        @Test
        @DisplayName("Approve button is hidden without the approver permission")
        void approveHiddenForReviewer() {
            loginAs("reviewer@example.com", "bills:submit");

            navigate(BillListView.class);

            assertTrue(find(Button.class).withText("Approve").all().isEmpty());
        }
    }
}
```

### Spring Security fallback (when the app secures views with Spring Security, not Holon Auth)

If the project uses Spring Security (e.g. `VaadinWebSecurity`, `@RolesAllowed`/`@PermitAll`
honoured by the navigation access control), authenticate by seeding the
`SecurityContextHolder` **before** `navigate(...)` instead of using `AuthContext`:

```java
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@BeforeEach
void login() {
    var auth = new UsernamePasswordAuthenticationToken(
        "reviewer@example.com", "n/a",
        List.of(new SimpleGrantedAuthority("ROLE_REVIEWER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
}

@AfterEach
void logout() {
    SecurityContextHolder.clearContext();
}
```

For multi-tenant apps (schema-per-tenant), also provision/bind the tenant schema before
navigating (e.g. `TenantContext.set(tenantId)` in `@BeforeEach`, `TenantContext.clear()` in
`@AfterEach`) so the view's Datastore resolves the correct schema.

## Flyway test seed migration

Place test seed data in `src/test/resources/db/migration/`:

```sql
-- src/test/resources/db/migration/V900__test_seed_bills.sql

INSERT INTO bill (id, vendor_name, invoice_number, invoice_date, total_amount, status)
VALUES
  (nextval('bill_seq'), 'Test Vendor A', 'TEST-001', '2024-01-10', 1000.00, 'PENDING_REVIEW'),
  (nextval('bill_seq'), 'Test Vendor B', 'TEST-002', '2024-01-11', 2500.00, 'APPROVED');
```

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Identify the `@Route` view class(es) to test
3. Read the Holon Auth role/permission model from `security-patterns.md`
4. Consult the browserless testing docs (`browserless-test-spring`, `SpringBrowserlessTest`) for the exact test APIs — see <https://vaadin.com/docs/latest/flow/testing/browserless/getting-started>
5. Write test seeds in `src/test/resources/db/migration/V9NN__test_seed_<entity>.sql`
6. Create test class:
    - View renders and lists data
    - Form submit saves data
    - Role-gated buttons visible/hidden per role
7. Validate tests pass with `./mvnw verify` or `./gradlew test`

## Maven dependency

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-spring</artifactId>
    <version>1.1.2</version>
    <scope>test</scope>
    <!--
      The Vaadin BOM (e.g. 25.2.1) does NOT manage browserless-test-spring, so pin a version.
      Use 1.1.x (or newer) on Vaadin 25.2 — the 1.0.x line was built against Vaadin 25.1-rc
      and references a removed com.vaadin.flow.component.slider.Slider, causing a
      NoClassDefFoundError. 1.1.x provides SpringBrowserlessTest + the find(...) query API.
    -->
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Resources

- Browserless testing guide: <https://vaadin.com/docs/latest/flow/testing/browserless/getting-started> (base class, `navigate`/`test`/query APIs, component testers)
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for browserless testing APIs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Auth APIs in tests
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
