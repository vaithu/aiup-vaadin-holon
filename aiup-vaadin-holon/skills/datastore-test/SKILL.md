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

## Instructions

Create JUnit 5 integration tests for the use case `$ARGUMENTS` using
Testcontainers PostgreSQL + Flyway-migrated schema + Holon Datastore from Context.

**Never mock the Datastore.** Every test exercises the real database via the real
Holon `Datastore` backed by a Testcontainers PostgreSQL container.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.**

- **No mocking of Datastore** — use Testcontainers + real Flyway-migrated DB
- **No `@Autowired`** — retrieve Datastore from `Context.get()`
- **No Spring Security in test layer** — auth is tested via Holon Auth APIs only
- Java 25 / JUnit 5 / Testcontainers / Flyway 10.x / PostgreSQL 16+

## Pre-Emit Checklist

- [ ] No mocked Datastore
- [ ] Testcontainers `@Container` with `PostgreSQLContainer`
- [ ] Flyway migrations applied before tests (`Flyway.configure().load().migrate()`)
- [ ] Datastore retrieved via `Context.get()`, not `@Autowired`
- [ ] Test data created in `@BeforeEach`, cleaned in `@AfterEach`
- [ ] No `Thread.sleep()` — use proper JUnit 5 assertions
- [ ] Tests cover: list/filter, single-record lookup, save (insert + update), delete

## Test structure

```java
package com.example.ap.datastore;

import com.holonplatform.core.Context;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.jdbc.spring.boot.JdbcDatastoreAutoConfiguration;
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

        // Configure and register JDBC Datastore in Holon Context
        // (Use Holon JDBC Datastore builder — exact API from JavaDocs MCP or holon-datastore-jdbc docs)
        datastore = /* JdbcDatastore.builder()
            .dataSource(ds)
            .build() */ null; // replace with actual Holon JDBC Datastore bootstrap

        Context.get().scope(com.holonplatform.core.ContextScope.APPLICATION)
            .registerResource(Datastore.CONTEXT_KEY, datastore, Datastore.class);

        billService = new BillService();
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
4. Bootstrap Holon JDBC Datastore from the container's JDBC URL (consult JavaDocs MCP for exact API)
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

- If configured, use the JavaDocs MCP server for Holon JDBC Datastore builder API: `https://www.javadocs.dev/mcp`
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
