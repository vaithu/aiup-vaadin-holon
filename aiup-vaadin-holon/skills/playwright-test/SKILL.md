---
name: playwright-test
description: >
  Creates Playwright browser-based end-to-end tests for Holon Vaadin Flow views.
  Tests run against a running application at http://localhost:8080. Covers the
  happy path, at least one denied-permission path, and visual regression
  screenshots (assertThat(page).hasScreenshot) for key stable view states.
  Use when the user asks to "write Playwright tests", "create e2e tests",
  "write integration tests", "test in the browser", "write E2E tests",
  "test the full stack in a browser", "add screenshot tests", "add visual
  regression", or mentions end-to-end testing, browser tests, Playwright for
  Vaadin, toHaveScreenshot, or E2E tests for UC-XXX.
---

# Playwright E2E Tests

Create Playwright end-to-end tests for the use case `$ARGUMENTS`.
Tests run against a live application at `http://localhost:8080`.

Cover:
1. **Happy path** — the main success scenario from the use case spec.
2. **Denied-permission path** — a user without the required permission is
   redirected or sees a restricted view.
3. **Visual regression** — `assertThat(page).hasScreenshot(...)` snapshots for
   each stable, fully-loaded view state to catch unintended UI regressions.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.**

- **No `Thread.sleep()`** — use Playwright auto-waiting and `waitFor*` methods
- **No raw locators** (`page.locator("vaadin-text-field")`) — use accessible locators
  (`getByLabel`, `getByRole`, `getByText`) which pierce Vaadin's shadow DOM correctly
- **No direct service/datastore access** — this is a blackbox test; only the browser
  and the running app are in scope
- **Delete only data created during the test** in `@AfterEach` / `afterEach`
- **Do not assume all grid rows are rendered** — Vaadin virtualizes the grid viewport
- **Screenshot baselines are committed** — store generated baseline PNGs in
  `src/test/resources/e2e-snapshots/` and commit them; the test fails if a baseline is
  missing (run with `-Dplaywright.snapshotsUpdate=true` once to create them)
- **Mask dynamic regions** — use `Page.HasScreenshotOptions().setMask(...)` to exclude
  timestamps, user avatars, and other content that changes between runs
- **Set a diff tolerance** — use `setMaxDiffPixelRatio(0.01)` (1 %) to tolerate
  minor sub-pixel rendering differences across platforms

## Pre-Emit Checklist

- [ ] No `Thread.sleep()` or `page.waitForTimeout()`
- [ ] No raw CSS/shadow DOM locators — use `getByLabel`, `getByRole`, `getByText`
- [ ] Happy path test covers the full main success scenario
- [ ] At least one denied-permission test (login as a role without the required permission)
- [ ] `afterEach` cleans up only data created during the test
- [ ] Tests run against `http://localhost:8080` (configurable via env var)
- [ ] One `assertThat(page).hasScreenshot(...)` per stable view state (list view loaded, detail pane open)
- [ ] Dynamic regions (timestamps, avatars) masked with `setMask(...)`
- [ ] `setMaxDiffPixelRatio(0.01)` applied to every snapshot assertion
- [ ] Baseline PNGs committed to `src/test/resources/e2e-snapshots/`

## Test structure (Java)

```java
package com.example.ap.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillApprovalE2ETest {

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    static final String BASE_URL = System.getenv().getOrDefault("APP_URL", "http://localhost:8080");

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void newContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Finance Director can approve a pending bill")
    void financeDirectorApprovesBill() {
        // 1. Navigate and log in as Finance Director
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("director@example.com");
        page.getByLabel("Password").fill("test-password");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();

        // 2. Navigate to bills
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Bills")).click();
        page.waitForURL("**bills**");

        // 3. Find a pending bill row and open it
        Locator firstPendingRow = page.getByRole(AriaRole.ROW)
            .filter(new Locator.FilterOptions().setHasText("PENDING_REVIEW"))
            .first();
        firstPendingRow.click();

        // 4. Wait for detail pane to open
        page.waitForSelector("[aria-label='Approve']");

        // 5. Click Approve
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Approve")).click();

        // 6. Verify success notification
        page.waitForSelector(".v-notification, vaadin-notification-container");
        assertTrue(page.getByText("approved").isVisible());
    }

    // ── Denied-permission path ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("AP Reviewer cannot see the Approve button")
    void apReviewerCannotApprove() {
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("reviewer@example.com");
        page.getByLabel("Password").fill("test-password");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Bills")).click();
        page.waitForURL("**bills**");

        Locator firstRow = page.getByRole(AriaRole.ROW).nth(1);
        firstRow.click();

        // Approve button should not be visible for AP_REVIEWER
        assertFalse(page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Approve")).isVisible());
    }
}
```

## Visual regression (screenshot comparison)

Use `assertThat(page).hasScreenshot(...)` from `com.microsoft.playwright.assertions.PlaywrightAssertions`
to catch unintended visual regressions. Take a snapshot after every stable, fully-loaded view state.

### Rules

- **Snapshot after data is loaded** — wait for the `PropertyListing` or `PropertyForm` to be
  visible before asserting, so the grid rows are fully rendered.
- **Mask dynamic regions** — any content that changes between runs (timestamps, relative dates,
  user avatars, notification badges) must be masked; otherwise snapshots will produce false
  failures on every run.
- **1 % pixel-ratio tolerance** — set `setMaxDiffPixelRatio(0.01)` to absorb sub-pixel
  rendering differences across OS / GPU combinations.
- **Commit baselines** — on the first run (or when the UI intentionally changes) generate
  baselines with `-Dplaywright.snapshotsUpdate=true`, then commit the PNG files from
  `src/test/resources/e2e-snapshots/` so that CI can compare against them.

### Code example

```java
package com.example.ap.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BillListViewVisualTest {

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    static final String BASE_URL = System.getenv().getOrDefault("APP_URL", "http://localhost:8080");

    // Snapshot baseline directory — committed to version control
    static final String SNAPSHOT_DIR = "src/test/resources/e2e-snapshots";

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void newContext() {
        // Fixed viewport so snapshots are deterministic across machines
        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(1280, 900));
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    @Order(1)
    @DisplayName("Bill list view matches visual baseline")
    void billListViewMatchesBaseline() {
        // 1. Log in as Finance Director
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("director@example.com");
        page.getByLabel("Password").fill("test-password");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();

        // 2. Navigate to Bills list
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Bills")).click();
        page.waitForURL("**bills**");

        // 3. Wait for the PropertyListing to be fully rendered
        page.waitForSelector("vaadin-grid[aria-rowcount]");

        // 4. Mask dynamic regions (timestamps, user avatar)
        List<Locator> dynamicRegions = List.of(
            page.getByTestId("user-avatar"),          // user avatar / chip
            page.locator("[data-testid='timestamp']")  // any rendered timestamps
        );

        // 5. Assert visual snapshot — 1 % pixel-ratio tolerance
        assertThat(page).hasScreenshot(
            Paths.get(SNAPSHOT_DIR, "bill-list-view.png"),
            new Page.HasScreenshotOptions()
                .setMaxDiffPixelRatio(0.01)
                .setMask(dynamicRegions));
    }

    @Test
    @Order(2)
    @DisplayName("Bill detail pane matches visual baseline")
    void billDetailPaneMatchesBaseline() {
        // 1. Log in and navigate
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("director@example.com");
        page.getByLabel("Password").fill("test-password");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Bills")).click();
        page.waitForURL("**bills**");

        // 2. Open the first row to load the detail PropertyForm
        page.getByRole(AriaRole.ROW)
            .filter(new Locator.FilterOptions().setHasText("PENDING_REVIEW"))
            .first().click();
        page.waitForSelector("[aria-label='Approve']");

        // 3. Mask the invoice-date field (relative date display can vary)
        List<Locator> dynamicRegions = List.of(
            page.getByLabel("Invoice Date")
        );

        // 4. Assert visual snapshot of the detail pane only
        assertThat(page.locator(".detail-pane")).hasScreenshot(
            Paths.get(SNAPSHOT_DIR, "bill-detail-pane.png"),
            new Locator.HasScreenshotOptions()
                .setMaxDiffPixelRatio(0.01)
                .setMask(dynamicRegions));
    }
}
```

### Generating / updating baselines

```bash
# First run — create baseline PNGs
./mvnw verify -Pit -Dplaywright.snapshotsUpdate=true

# Commit the generated baselines
git add src/test/resources/e2e-snapshots/
git commit -m "chore: add Playwright visual regression baselines"

# Normal CI run — compare against committed baselines
./mvnw verify -Pit
```

> **Tip:** Re-run with `-Dplaywright.snapshotsUpdate=true` and commit updated PNGs whenever the
> UI is intentionally changed (e.g. a redesign or a new field added from the HTML mockup).
> CI will fail on unintentional visual diffs and show a diff PNG in the Surefire report.

## Locator rules (Vaadin shadow DOM)

Vaadin renders components in shadow DOM. These locator strategies work:

| Need | Locator |
|------|---------|
| Input field by visible label | `page.getByLabel("Invoice Number")` |
| Button by text | `page.getByRole(AriaRole.BUTTON, opts.setName("Approve"))` |
| Grid row by text | `page.getByRole(AriaRole.ROW).filter(hasText("ACME"))` |
| Navigation link | `page.getByRole(AriaRole.LINK, opts.setName("Bills"))` |
| Notification | `page.waitForSelector("vaadin-notification-container")` |

**Never use:**
- `page.locator("vaadin-text-field")` — shadow DOM piercing is unreliable
- `page.locator("//input")` — XPath does not pierce shadow DOM
- `page.waitForTimeout(3000)` — use `waitForURL`, `waitForSelector`, or assertion auto-wait

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md`
2. Identify happy path steps and at least one denied-permission scenario
3. Create test class with `@BeforeAll`/`@AfterAll` Playwright lifecycle management
4. For each test scenario:
    - Log in as the appropriate role
    - Navigate to the correct view
    - Perform interactions using accessible locators
    - Assert the outcome
    - Clean up any created data
5. Add a visual regression test class (see **Visual regression** section above):
    - One snapshot per stable, fully-loaded view state (list view, detail pane)
    - Fixed viewport (`1280 × 900`) in `NewContextOptions`
    - Mask all dynamic regions (timestamps, avatars)
    - `setMaxDiffPixelRatio(0.01)` on every snapshot assertion
6. Generate baselines: `./mvnw verify -Pit -Dplaywright.snapshotsUpdate=true`
7. Commit baseline PNGs from `src/test/resources/e2e-snapshots/`
8. Run `./mvnw verify -Pit` (or `./gradlew integrationTest`) against a running app
9. On failure: check that the app is running, verify test data exists via Flyway migrations,
   use `page.screenshot()` to capture failure state; for visual failures inspect the diff PNG
   in the Surefire report

## Maven dependency

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <scope>test</scope>
</dependency>
```

Install Playwright browsers once: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"`

## Resources

- If configured, use the Playwright MCP server (stdio) for browser automation assistance
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
