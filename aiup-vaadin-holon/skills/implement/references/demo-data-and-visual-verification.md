# Demo data seeding + mandatory visual verification

Every HTML mockup in this project is rendered **populated** — a customer list with 342 rows,
a detail pane with a real hero strip, contacts, activity, invoices. A freshly implemented view
backed by an empty dev database will **always** look broken next to it (empty-state icon, "0"
KPIs, blank detail pane) even when every component is mapped correctly. That gap — not a
component-mapping defect — is the most common reason an implementation looks like it barely
resembles its mockup.

This reference closes that gap with two mandatory, non-optional steps that run at the **end**
of every `/implement`, `/implement-master-detail`, and `/implement-from-html` invocation:

1. **Seed realistic demo data** so the view is never empty when it's verified.
2. **Take a screenshot of the running view and compare it region-by-region against the
   mockup**, using the Visual Region Inventory / Fidelity note already produced during
   implementation. Do not declare the skill "done" until this comparison has been performed.

## What "100% match" actually means here

Read this before promising — or expecting — pixel-identical output:

| Category | Expectation |
|---|---|
| **Structural / functional regions** (appbar, nav, master list populated with rows, detail hero with real field values, forms, tabs, KPI strip with real numbers) | **Must match**: same region present, same layout position, populated with real data — not the empty state, not "0" everywhere. This is the bar a screenshot must clear. |
| **Regions marked `Full` in the Visual Region Inventory** | Must be visually present and populated. A `Full` region rendering empty/blank is a **bug** — fix the seed data, the fetch binding, or the query filter before finishing. |
| **Regions marked `Partial`** | Structural container must match; decorative CSS (gradients, shadows, custom fonts) may differ — this is expected and already called out with a `styles.css` comment. |
| **Regions marked `Manual` or referencing an entity outside the current entity model** (e.g. a Deals/Invoices/Forecast panel when only Customer/Contact exist) | **Explicitly out of scope.** Do not fabricate a fake entity or hard-coded numbers just to look identical — this was already raised to the developer per the Visual Region Inventory. Pixel similarity here is **not** part of the completion bar. |
| **Decorative-only detail** (exact gradient angle, font-rendering, animation timing, browser chrome) | **Never** part of the completion bar. Do not chase pixel-diff-zero against the static mockup screenshot for these. |

If a stakeholder expects literal pixel-for-pixel equality including out-of-scope entities and
decorative flourishes, say so explicitly in the final report instead of silently attempting it —
that is a scope conversation, not an implementation defect.

## Step A — Seed demo data (mandatory)

Do this **before** taking any verification screenshot. Never verify against an empty database.

1. Add a `demo` Spring profile-guarded Flyway location so seed data never leaks into a real
   deployment:

   ```properties
   # src/main/resources/application.properties
   spring.flyway.locations=classpath:db/migration
   ```

   ```properties
   # src/main/resources/application-demo.properties
   spring.flyway.locations=classpath:db/migration,classpath:db/demo-seed
   ```

2. Write one `V9NN__demo_seed_<entity>.sql` per entity under
   `src/main/resources/db/demo-seed/`, seeding **5–10 rows** that mirror the mockup's own
   sample data in naming style (reuse the mockup's actual sample names/numbers where visible —
   e.g. `Helix Robotics SE`, `C-2026-0023` — so the screenshot reads the same way the mockup
   does), not generic `Test 1` / `Row A` placeholders:

   ```sql
   -- src/main/resources/db/demo-seed/V901__demo_seed_customer.sql
   INSERT INTO customer (id, name, customer_number, billing_address, default_currency, payment_terms_days, version)
   VALUES
     (nextval('customer_seq'), 'Helix Robotics SE', 'C-2026-0023', 'Munich, DE', 'EUR', 30, 0),
     (nextval('customer_seq'), 'PrahaTech s.r.o.', 'C-2026-0019', 'Prague, CZ', 'EUR', 14, 0),
     (nextval('customer_seq'), 'Lumen Health AG', 'C-2026-0011', 'Vienna, AT', 'EUR', 45, 0);
   ```

3. Seed at least one row per **lookup** entity referenced by a combobox (tax codes, statuses,
   tiers) — an empty lookup table breaks the "creatable combobox" pattern just as visibly as
   an empty listing.
4. Run the app with the profile active: `mvn spring-boot:run -Dspring-boot.run.profiles=demo`
   (or `./gradlew bootRun --args='--spring.profiles.active=demo'`).

## Step B — Mandatory visual verification (blocking)

Perform this immediately after Step A, before declaring the skill's work complete.

1. **Start the app** with the `demo` profile (Step A.4) and log in as a seeded user.
2. **Navigate to the implemented view(s)** and wait for data to finish loading (grid rows
   visible, hero strip populated, KPI numbers non-zero).
3. **Take a full-page screenshot** of each implemented view. Use whichever tool is available
   in your environment — a Playwright script (see `../../playwright-test/SKILL.md`), the
   `open_browser_page` / `screenshot_page` browser tools if available, or the project's own
   dev server plus a manual screenshot tool.
4. **Open the source mockup HTML file directly** (`file:///.../mockups/.../<view>.html`) and
   take a screenshot of it too, for side-by-side reference.
5. **Compare region-by-region against the Visual Region Inventory** produced during
   implementation (see `implement-from-html/SKILL.md` Step 4b or the equivalent inventory in
   `implement-master-detail/SKILL.md`):
   - Every `Full` region: confirm it is visible **and populated** in the live screenshot. If it
     renders empty (e.g. "No items", "0", blank card), this is a **regression** — go back and
     fix the Datastore query, the seed data, or the binding; do not report completion.
   - Every `Partial` region: confirm the structural container is present; note (do not "fix")
     any purely decorative divergence already covered by a `styles.css` comment.
   - Every `Manual` / out-of-scope region: confirm it was raised to the developer per the
     Visual Region Inventory and is not silently missing without explanation.
6. **Attach both screenshots and the region-by-region comparison table** to the final report
   delivered to the developer. If any `Full` region failed, iterate steps A–B until it passes
   before considering the skill done.
7. **Persist the live-view screenshot as a Playwright baseline** (optional but recommended) so
   future runs catch regressions automatically — see the "Visual regression" section of
   `../../playwright-test/SKILL.md` (`assertThat(page).hasScreenshot(...)`,
   `setMaxDiffPixelRatio(0.01)`, masked dynamic regions).

## Common root causes of an "empty-looking" implementation (checklist)

When a screenshot doesn't match, check these **before** touching component code — they are far
more common than a wrong Holon component choice:

- [ ] No demo data seeded at all (most common cause — see Step A)
- [ ] `ListingBundle` fetch callback filters on a query parameter (e.g. `q`, a chip filter) that
      defaults to a value matching nothing in the seed data
- [ ] Lookup comboboxes (tax code, status, tier) have zero rows, so `EntityFormPanel` renders
      blank selects
- [ ] The seeded rows don't satisfy a `NOT NULL` / FK constraint the entity model requires,
      so the insert silently failed (check Flyway migration logs)
- [ ] The view under test requires an authenticated session/tenant that wasn't provisioned
      before navigating (see the multi-tenant note in `context-wiring.md` if applicable)
