---
name: datastore-test
description: >
  Creates JUnit 5 integration tests for Holon Datastore-backed services using
  Testcontainers PostgreSQL and Flyway migrations. Use when the user asks to
  "write datastore tests", "test the Holon Datastore", "create integration tests
  for the service layer", "write JUnit tests with Testcontainers", or mentions
  Holon Datastore testing, JUnit 5 integration tests, Testcontainers, or
  database integration tests for UC-XXX.
---

# Datastore Test

## Prerequisites

Before writing any tests, verify that the implementation for the use case exists:

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-XXX-*.md` (the use case being tested) | `/use-case-spec` |
| Service class for the use case (e.g. `src/main/java/**/*Service.java`) | `/implement` |
| `src/main/resources/db/migration/V*.sql` (Flyway migration scripts) | `/flyway-migration` |

If any artifact is missing, **stop** and tell the user which skill to run first:
> "No service implementation found — run `/implement UC-XXX` first, then re-run `/datastore-test`."  
> "No Flyway migration scripts found — run `/flyway-migration` first." (if no SQL migrations exist)

Do not attempt to generate tests against unimplemented services.

## Instructions

Create JUnit 5 integration tests for the use case `$ARGUMENTS` using
Testcontainers PostgreSQL + Flyway-migrated schema + Holon Datastore from Context.

**Never mock the Datastore.** Every test exercises the real database via the real
Holon `Datastore` backed by a Testcontainers PostgreSQL container.

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

- **No mocking of Datastore** — use Testcontainers + real Flyway-migrated DB
- **No `@Autowired`** — inject `Datastore` into the service via constructor; pass it directly in `@BeforeAll`
- **No Spring Security in test layer** — auth is tested via Holon Auth APIs only
- Java 25 / JUnit 5 / Testcontainers / Flyway 10.x / PostgreSQL 16+

## Pre-Emit Checklist

- [ ] No mocked Datastore
- [ ] Testcontainers `@Container` with `PostgreSQLContainer`
- [ ] Flyway migrations applied before tests (`Flyway.configure().load().migrate()`)
- [ ] `Datastore` constructed and passed directly to service constructor — no `@Autowired`, no `Context.get()`
- [ ] Test data created in `@BeforeEach`, cleaned in `@AfterEach`
- [ ] No `Thread.sleep()` — use proper JUnit 5 assertions
- [ ] Tests cover: list/filter, single-record lookup, save (insert + update), delete

## Test structure

```java
package com.example.ap.datastore;

import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.jpa.spring.boot.JpaDatastoreAutoConfiguration;
import com.example.ap.domain.Bill;
import com.example.ap.service.BillService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ap_test")
            .withUsername("test")
            .withPassword("test");

    static Datastore datastore;
    static BillService billService;

    @BeforeAll
    static void setUpDatastore() {
        // Apply Flyway migrations to the test container
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();

        // Configure and register Holon Datastore in Holon Context
        // (Use Holon Datastore builder — exact API from JavaDocs MCP or holon-datastore-jpa docs)
        datastore = /* JpaDatastore.builder()
            .entityManagerFactory(emf)
            .build() */ null; // replace with actual Holon JPA Datastore bootstrap

        billService = new BillService(datastore);
    }

    private Long createdId;

    @Test
    @Order(1)
    void testInsert() {
        Bill bill = new Bill();
        bill.setVendorName("ACME Corp");
        bill.setInvoiceNumber("INV-2024-001");
        bill.setInvoiceDate(LocalDate.of(2024, 1, 15));
        bill.setTotalAmount(new BigDecimal("1500.00"));
        bill.setStatus("PENDING_REVIEW");

        datastore.save(Bill.PROPERTIES.getDataPath(), bill);
        assertNotNull(bill.getId(), "ID should be populated after insert");
        createdId = bill.getId();
    }

    @Test
    @Order(2)
    void testFindById() {
        Optional<Bill> found = billService.findById(createdId);
        assertTrue(found.isPresent());
        assertEquals("ACME Corp", found.get().getVendorName());
    }

    @Test
    @Order(3)
    void testFindPending() {
        List<Bill> pending = billService.findPending();
        assertFalse(pending.isEmpty());
        assertTrue(pending.stream().allMatch(b -> "PENDING_REVIEW".equals(b.getStatus())));
    }

    @Test
    @Order(4)
    void testApprove() {
        billService.approve(createdId);
        Optional<Bill> approved = billService.findById(createdId);
        assertTrue(approved.isPresent());
        assertEquals("APPROVED", approved.get().getStatus());
    }

    @Test
    @Order(5)
    void testDelete() {
        datastore.delete(Bill.PROPERTIES.getDataPath(),
            Bill.PROPERTIES.property("id").eq(createdId));
        Optional<Bill> deleted = billService.findById(createdId);
        assertFalse(deleted.isPresent());
    }
}
```

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Identify the service class and `BeanPropertySet` to test
3. Set up Testcontainers PostgreSQL container + Flyway migration in `@BeforeAll`
4. Bootstrap Holon Datastore from the container URL (consult JavaDocs MCP for exact API)
5. Register Datastore in Holon Context
6. For each operation in the service (list, findById, save, delete, domain actions):
    - Write a `@Test` method
    - Create test data in setup, clean up in teardown
7. Validate:
    - Tests cover all service methods from the use case
    - No mocked Datastore
    - `@AfterEach` deletes only data created during the test
    - Tests pass with `./mvnw verify` or `./gradlew test`

## Resources

- If configured, use the JavaDocs MCP server for Holon Datastore builder API: `https://www.javadocs.dev/mcp`
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
