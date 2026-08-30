---
name: holon-vaadin-test
description: >
  Creates server-side Vaadin Browserless unit tests using vaadin-testbench-unit-junit5
  for Holon Vaadin Flow views (PropertyForm, PropertyListing). Seeds via Flyway test
  migrations. Also tests role-gated buttons appear/hide correctly using Holon Auth.
  Use when the user asks to "write Vaadin unit tests", "test the Holon Vaadin view",
  "write browserless tests", "test the PropertyForm", "test the PropertyListing",
  "verify role-based visibility", or mentions Vaadin Browserless, vaadin-testbench-unit,
  server-side UI tests for UC-XXX, or testing Holon Vaadin components without a browser.
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
using `com.vaadin:vaadin-testbench-unit-junit5`. Tests run in-memory without a browser,
exercising the real Holon Vaadin Flow component tree.

Seed test data via Flyway test migrations in `src/test/resources/db/migration/`.

Also test **role-gated visibility**: verify that buttons and sections requiring specific
Holon Auth permissions appear or hide correctly for different roles.

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

- **No browser required** — `vaadin-testbench-unit-junit5` runs server-side
- **No Mockito for Datastore** — use a real Testcontainers DB (or in-memory H2 if simpler)
- **No `@Autowired`** — inject dependencies via constructors; pass `Datastore` to service in test setup
- Seed test data with Flyway migrations in `src/test/resources/db/migration/`
- Auth context for role-gated tests: use Holon `AuthContext` API to mock the current user's role

## Pre-Emit Checklist

- [ ] Test class extends the Vaadin Browserless test base (check vaadin-testbench-unit-junit5 docs via MCP)
- [ ] Flyway test migrations in `src/test/resources/db/migration/`
- [ ] No mocked Datastore (use in-memory H2 or Testcontainers)
- [ ] Role-gated button visibility tests use Holon `AuthContext` to set current user permissions
- [ ] Tests cover: view renders correctly, form submit, listing shows data, role-restricted buttons visible/hidden

## Test structure

```java
package com.example.ap.ui;

import com.holonplatform.auth.AuthContext;
import com.holonplatform.auth.Authentication;
import com.vaadin.testbench.unit.UIUnitTest;
import com.vaadin.testbench.unit.ViewPackages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@ViewPackages(packages = "com.example.ap.ui")   // scan package for @Route views
class BillListViewTest extends UIUnitTest {

    @BeforeEach
    void setUp() {
        // Navigate to the view under test
        navigate(BillListView.class);
    }

    @Test
    @DisplayName("Bill list renders with data from Datastore")
    void listRendersWithData() {
        // Verify the PropertyListing is present
        // (Use vaadin-testbench-unit APIs — consult Vaadin MCP for exact method names)
        assertNotNull($(BillListView.class).first());
    }

    @Nested
    @DisplayName("Role-gated visibility")
    class RoleGatedVisibility {

        @Test
        @DisplayName("Approve button is visible for FINANCE_DIRECTOR")
        void approveButtonVisibleForFinanceDirector() {
            // Set the Holon AuthContext for this test
            AuthContext.require().setAuthentication(
                Authentication.build("director@example.com")
                    .withPermission("bills:approve")
                    .build());

            navigate(BillListView.class);

            // Verify approve button is visible
            // (Use vaadin-testbench-unit button query APIs)
            // assertFalse(approveButton.isHidden());
        }

        @Test
        @DisplayName("Approve button is hidden for AP_REVIEWER")
        void approveButtonHiddenForApReviewer() {
            AuthContext.require().setAuthentication(
                Authentication.build("reviewer@example.com")
                    .withPermission("bills:view")
                    .withPermission("bills:submit")
                    .build());

            navigate(BillListView.class);

            // Verify approve button is NOT visible
            // assertFalse(approveButton.isVisible());
        }
    }
}
```

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
4. Consult vaadin-testbench-unit-junit5 docs via Vaadin MCP server for exact test APIs
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
    <artifactId>vaadin-testbench-unit-junit5</artifactId>
    <scope>test</scope>
    <!-- version managed by Vaadin BOM -->
</dependency>
```

## Resources

- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for vaadin-testbench-unit-junit5 APIs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Auth APIs in tests
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
