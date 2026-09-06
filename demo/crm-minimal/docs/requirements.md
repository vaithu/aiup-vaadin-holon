# Requirements: Acme CRM/ERP

> AI Unified Process (AIUP) requirements catalog. Source of truth: `docs/vision.md`.
> Three tables: **Functional Requirements** (FR, user stories), **Non-Functional
> Requirements** (NFR, measurable quality attributes), and **Constraints** (CON,
> rules that must always be enforced). Every requirement has a stable ID,
> a priority, and a status. Use case specifications (UC-*) trace back to FR
> IDs; entity attributes trace back to CON IDs.

---

## How to read this

- **FR-*** — what the system must do. Written as user stories
  `As a [role], I want [goal] so that [benefit]`.
- **NFR-*** — how well the system must do it. Measurable. Each has a metric
  and a target.
- **CON-*** — rules the system must always enforce. Mostly invariant-level
  (uniqueness, referential integrity, business rules that cannot be expressed
  as a story).

**Priority levels**: `MUST` (P0, blocks release), `SHOULD` (P1, expected next), `COULD` (P2, nice to have).

**Status values**: `Approved` (signed off), `Draft` (in review), `Superseded` (replaced), `Deferred` (moved out).

**Traceability**:
- Every UC-NNN in `docs/use_cases/` must reference ≥1 FR.
- Every entity in `docs/entity_model.md` must reference ≥1 CON.
- Every test in `src/test/...` must reference 1 FR or UC.

---

## Functional Requirements

### Self-Service Configuration (FR-T)

> All items in this section are performed by the tenant's own staff (the SaaS
> customer), with no engineering or support involvement required.

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-T-001 | MUST   | Approved | As a **tenant administrator**, I want to configure my tenant's brand name, logo, primary color, and font so that the application looks like our company, not a generic product. |
| FR-T-002 | MUST   | Approved | As a **tenant administrator**, I want to configure my tenant's default language, timezone, date format, and number format so that the UI matches the conventions of our country. |
| FR-T-003 | MUST   | Approved | As a **tenant administrator**, I want to define per-tenant numbering patterns for the nine document types (SO, INV, QU, PO, BIL, PAY, REC, JE, RMA) with year-reset and padding so that document IDs are consistent and human-readable. |
| FR-T-004 | MUST   | Approved | As a **tenant administrator**, I want to define the active tax codes and the default tax for my tenant so that sales and purchase documents are taxed correctly under our regime. |
| FR-T-005 | MUST   | Approved | As a **tenant administrator**, I want to configure the fiscal year start, period length (monthly), and the closure schedule so that month-end close is a predictable, scheduled event. |
| FR-T-006 | MUST   | Approved | As a **tenant administrator**, I want to onboard a new tenant through a four-step wizard (identity → branding → regional → admin) so that subsidiaries can be provisioned without engineering involvement. |
| FR-T-007 | MUST   | Approved | As a **tenant administrator**, I want to switch the active tenant from a top-bar menu so that holding-company staff can work across multiple tenants in one session. |
| FR-T-008 | SHOULD | Approved | As a **tenant administrator**, I want to invite users to the tenant via SAML SSO (Okta, Azure AD) with optional 2FA enforcement so that onboarding is secure and self-service. |
| FR-T-009 | SHOULD | Approved | As a **tenant administrator**, I want to see the audit log of configuration changes so that I can prove who changed what and when. |
| FR-T-010 | SHOULD | Draft    | As a **tenant administrator**, I want to clone an existing tenant's settings to a new tenant as a starting point so that the wizard takes minutes, not an hour. |
| FR-T-011 | MUST   | Approved | As a **tenant administrator**, I want the system to store tenant data in the tenant's own region (EU → EU-Frankfurt, US → US-East, UK → EU-London) so that we comply with data-residency regulations. |

### Sales Cycle (FR-S)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-S-001 | MUST   | Approved | As a **sales representative**, I want to search the customer catalog by name, region, MSA status, or 3-yr spend so that I can find the right account in under 5 seconds. |
| FR-S-002 | MUST   | Approved | As a **sales representative**, I want to create a new customer with name, billing address, tax ID, default currency, default payment terms, and default price list so that downstream documents can reference them. |
| FR-S-003 | MUST   | Approved | As a **sales representative**, I want to view a customer's 360° profile (open orders, unpaid invoices, recent activity, MSA status, 3-yr spend, key contacts) so that I have full context before the next call. |
| FR-S-004 | MUST   | Approved | As a **sales representative**, I want to create a quote from a price list or a previous order, with per-line discount, tax, and validity date, so that I can respond to an RFQ in minutes. |
| FR-S-005 | MUST   | Approved | As a **sales representative**, I want to convert an accepted quote into a sales order with one action so that the customer does not have to re-key anything. |
| FR-S-006 | MUST   | Approved | As a **sales representative**, I want to issue an invoice from a sales order (full or partial fulfilment) so that we can bill the customer without leaving the order view. |
| FR-S-007 | MUST   | Approved | As a **sales representative**, I want to record a payment received (bank transfer, SEPA direct debit, card, cheque) against an invoice or a customer so that AR aging is up to date. |
| FR-S-008 | MUST   | Approved | As a **sales representative**, I want to see real-time AR aging by customer (current, 1–30, 31–60, 61–90, 90+) so that I can chase the right invoices. |
| FR-S-009 | MUST   | Approved | As a **sales representative**, I want to record activities (call, email, meeting, note) tied to a customer and visible on the activity stream so that the team has a shared history. |
| FR-S-010 | SHOULD | Approved | As a **sales representative**, I want to set up a recurring subscription (monthly / quarterly / annual) for a customer with auto-invoicing so that MRR is automatic. |
| FR-S-011 | SHOULD | Approved | As a **sales representative**, I want the system to suggest the next action on a customer (overdue invoice, expiring MSA, quote follow-up due) so that nothing falls through the cracks. |

### Purchasing Cycle (FR-P)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-P-001 | MUST   | Approved | As a **purchaser**, I want to maintain a vendor catalog with name, address, tax ID, bank details, default payment terms, default currency, and MSA status so that POs and bills are consistent. |
| FR-P-002 | MUST   | Approved | As a **purchaser**, I want to issue a Request-for-Quotation (RFQ) to one or more vendors with line items, quantities, and required-by date so that I can collect comparable quotes. |
| FR-P-003 | MUST   | Approved | As a **purchaser**, I want to compare vendor quotes side by side on price, lead time, and terms so that I can pick the winner quickly. |
| FR-P-004 | MUST   | Approved | As a **purchaser**, I want to place a purchase order from a selected quote (or from scratch) with line items, expected receipt date, and ship-to warehouse so that the vendor gets a binding commitment. |
| FR-P-005 | MUST   | Approved | As a **purchaser**, I want to confirm a goods receipt (received / back-ordered / damaged) against a PO so that the inventory is updated and the bill can be 3-way-matched. |
| FR-P-006 | MUST   | Approved | As a **purchaser**, I want to receive a vendor bill and have the system 3-way-match it against the PO and the goods receipt at the line level, blocking payment if the match fails, so that we never pay for goods we didn't order or receive. |
| FR-P-007 | MUST   | Approved | As a **purchaser**, I want to schedule outgoing payments in a SEPA batch (or single wire) with optional early-pay discount capture so that I can clear the AP queue efficiently. |
| FR-P-008 | MUST   | Approved | As a **purchaser**, I want to see real-time AP aging by vendor so that I can prioritise payments. |
| FR-P-009 | MUST   | Approved | As a **purchaser**, I want to issue a vendor return (RMA) for defective or wrong goods, with reverse-flow tracking (QC → RMA → credit note → ship → receive → refund), so that the vendor credits us correctly. |
| FR-P-010 | SHOULD | Approved | As a **purchaser**, I want to set up a vendor price list or rate contract with effective dates so that PO pricing is automatic. |

### Inventory (FR-I)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-I-001 | MUST   | Approved | As a **warehouse operator**, I want to view real-time stock by SKU and bin (warehouse + zone + bin) so that I can pick and pack accurately. |
| FR-I-002 | MUST   | Approved | As a **warehouse operator**, I want to receive incoming goods (carton-by-carton, with bin assignment) so that inventory is updated in real time. |
| FR-I-003 | MUST   | Approved | As a **warehouse operator**, I want to pick, pack, and ship outgoing goods against a sales order with a generated packing list and carrier handover so that the customer gets the right items. |
| FR-I-004 | MUST   | Approved | As a **warehouse operator**, I want to perform a stock adjustment (cycle count, write-off, write-on, damage) with a reason code so that the bin balance stays accurate. |
| FR-I-005 | MUST   | Approved | As a **warehouse operator**, I want to transfer stock between bins or between warehouses (in-transit tracking) so that stock can be repositioned without manual re-counting. |
| FR-I-006 | MUST   | Approved | As a **warehouse operator**, I want to build a composite item (kit / BOM) from its components, decrementing component stock and incrementing finished stock, so that bundled offers can be fulfilled. |
| FR-I-007 | MUST   | Approved | As a **warehouse operator**, I want to see the buildable quantity of a composite item (max from component stock) so that I know how many I can promise. |
| FR-I-008 | MUST   | Approved | As a **warehouse operator**, I want to manage item groups (product families) with variant axes (size × color × material) and auto-generated SKUs so that SKU proliferation is controlled. |
| FR-I-009 | MUST   | Approved | As a **warehouse operator**, I want a stock valuation report (FIFO / weighted average) per warehouse so that finance can close the period. |
| FR-I-010 | MUST   | Approved | As a **warehouse operator**, I want a stock-on-hand report by SKU, by warehouse, by bin, with filters for batch / lot / status so that physical counts reconcile. |
| FR-I-011 | SHOULD | Approved | As a **warehouse operator**, I want barcode-driven receive and ship so that data entry is hands-free. |
| FR-I-012 | SHOULD | Approved | As a **warehouse operator**, I want a "pick-list" view optimised for the warehouse floor with bin order and pack hints so that I can pick a shipment in one pass without backtracking. |

### Quality (FR-Q)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-Q-001 | MUST   | Approved | As a **quality inspector**, I want to perform an AQL-based inspection on an incoming goods receipt, sampling N units and recording pass / fail per unit, so that defective stock is held before it enters inventory. |
| FR-Q-002 | MUST   | Approved | As a **quality inspector**, I want to attach an inspection result to a goods receipt and trigger a hold or release on the affected stock, so that downstream picking is blocked. |
| FR-Q-003 | MUST   | Approved | As a **quality inspector**, I want to open a vendor RMA from a failed inspection with the failed quantity, defect type, and photos, so that the vendor gets a clean credit claim. |
| FR-Q-004 | MUST   | Approved | As a **quality inspector**, I want to open a customer RMA (sales return) from a customer-reported defect so that the customer gets a clean return process. |
| FR-Q-005 | MUST   | Approved | As a **quality inspector**, I want to disposition returned stock (restock / return-to-vendor / scrap / credit-only) so that inventory and finance are both updated. |
| FR-Q-006 | SHOULD | Approved | As a **quality inspector**, I want a defect Pareto report (top 10 defects by SKU, by vendor, by month) so that we can drive supplier improvement. |

### Finance (FR-F)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-F-001 | MUST   | Approved | As a **finance controller**, I want every financially significant event (invoice post, payment, credit note, return, adjustment, composite build, FX revaluation) to auto-post a journal entry so that the books stay current without manual re-keying. |
| FR-F-002 | MUST   | Approved | As a **finance controller**, I want each journal entry to be hash-chained with SHA-256 (each entry's hash = SHA-256(previousHash + contentHash)) so that any post-close edit is detectable. |
| FR-F-003 | MUST   | Approved | As a **finance controller**, I want to see a real-time trial balance and a P&L by period so that I can review the books without waiting for month-end. |
| FR-F-004 | MUST   | Approved | As a **finance controller**, I want a balance sheet as of any date so that I can answer the CFO's "where do we stand right now" question. |
| FR-F-005 | MUST   | Approved | As a **finance controller**, I want to perform month-end close: hard-lock the period, require external-auditor sign-off, generate a close log so that the close is repeatable and auditable. |
| FR-F-006 | MUST   | Approved | As a **finance controller**, I want a SEPA payment batch workflow (select bills → review early-pay discount → generate pain.001.001.03 XML → approve at 4-eyes for > €50K → execute) so that AP clearing is fast and audited. |
| FR-F-007 | MUST   | Approved | As a **finance controller**, I want to run AR aging and AP aging reports as of any date so that I can prioritise collections and payments. |
| FR-F-008 | MUST   | Approved | As a **finance controller**, I want a cash-flow forecast (expected inflows from open invoices, expected outflows from open bills) so that treasury can plan. |
| FR-F-009 | SHOULD | Approved | As a **finance controller**, I want a multi-currency revaluation job at month-end so that unrealised FX gains / losses are posted automatically. |
| FR-F-010 | SHOULD | Approved | As a **finance controller**, I want a per-tenant chart of accounts (CoA) with mapping to the group's consolidated CoA so that consolidation is mechanical. |

### Reporting (FR-R)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-R-001 | MUST   | Approved | As a **finance controller**, I want ten financial reports (P&L, balance sheet, cash flow, AR aging, AP aging, trial balance, general ledger, journal, tax summary, fixed-asset register) accessible from the Reports menu so that I can answer any finance question without asking engineering for a custom query. |
| FR-R-002 | MUST   | Approved | As a **warehouse operator**, I want ten inventory reports (stock-on-hand, stock-valuation, slow movers, dead stock, ABC analysis, stock-aging, transfer-history, adjustment-history, RMA-history, composite-build history) accessible from the Reports menu so that I can answer stock questions without waiting on finance or IT. |
| FR-R-003 | MUST   | Approved | As a **tenant administrator**, I want every report to be run as-of any date in the past (point-in-time) or as a range, so that historical questions are answerable. |
| FR-R-004 | SHOULD | Approved | As a **tenant administrator**, I want to export every report to PDF and CSV so that I can share with auditors and the board. |
| FR-R-005 | COULD  | Draft    | As a **tenant administrator**, I want to schedule a report (daily / weekly / monthly) emailed to a recipient list so that I don't have to remember to run it. |

### Cross-cutting (FR-X)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-X-001 | MUST   | Approved | As a **user**, I want every master-detail view to start with a 5-cell summary strip (the five most important numbers) so that I have full context in 2 seconds. |
| FR-X-002 | MUST   | Approved | As a **user**, I want to use the same application from a desktop browser and a mobile browser (iPhone-class screen) with full feature parity for my role's workflow so that I am never blocked by which device I happen to have on hand. |
| FR-X-003 | MUST   | Approved | As a **user**, I want a consistent visual language across all 38 entities (the same color conventions, the same card patterns, the same 3-yr portfolio annotation) so that I never have to learn a new pattern. |
| FR-X-004 | MUST   | Approved | As a **user**, I want to search globally across customers, vendors, products, orders, and invoices from the top bar so that I can find anything in under 3 keystrokes. |
| FR-X-005 | SHOULD | Approved | As a **user**, I want every page to work without JavaScript (server-rendered HTML, progressive enhancement) so that the app is robust and accessible. |
| FR-X-006 | SHOULD | Approved | As a **user**, I want to see the audit history of any record (who changed what, when) so that I can answer "how did this number get here". |
| FR-X-007 | SHOULD | Approved | As a **user**, I want a dashboard on the home page with my open tasks (overdue invoices, pending approvals, low-stock alerts) so that I can plan my day. |
| FR-X-008 | COULD  | Draft    | As a **user**, I want keyboard shortcuts for the most common actions (n=new, s=save, /=search) so that I can work faster. |

### Demo & Onboarding (FR-D)

| ID     | Priority | Status   | User story |
|--------|----------|----------|------------|
| FR-D-001 | MUST   | Approved | As a **new user / evaluator**, I want a fully seeded demo tenant on first boot so that I can see the application working without any setup. |
| FR-D-002 | MUST   | Approved | As a **new user / evaluator**, I want to sign in with a documented demo account (`elena.lindqvist@iyensoft.com` / `demo`) so that I can explore the system immediately. |
| FR-D-003 | SHOULD | Approved | As a **new user / evaluator**, I want the demo data to tell a coherent story (one ongoing customer, one ongoing vendor, one cycle of each process) so that I can see how the pieces fit together. |

---

## Non-Functional Requirements

### Performance

| ID        | Priority | Status   | Metric | Target | Notes |
|-----------|----------|----------|--------|--------|-------|
| NFR-P-001 | MUST    | Approved | Detail view render time (server-side, including data fetch) | ≤500ms at p95 | For 95% of records, on a 4-core 8GB instance, under 1000 records per entity per tenant. |
| NFR-P-002 | MUST    | Approved | List view render time (initial page, 50 rows) | ≤800ms at p95 | Full-text search adds ≤300ms. |
| NFR-P-003 | MUST    | Approved | Master-detail click-to-render (cached) | ≤100ms at p95 | The detail view is cached after first load. |
| NFR-P-004 | MUST    | Approved | Mobile first-paint on a cold load over 4G | ≤2.0s | Includes framework download. |
| NFR-P-005 | SHOULD  | Approved | Report generation (10K rows) | ≤3s at p95 | Async for larger reports. |
| NFR-P-006 | SHOULD  | Approved | Concurrent users per instance | ≥500 | Without degradation. |
| NFR-P-007 | SHOULD  | Approved | Database query time (95% of queries) | ≤100ms | Indexed, pgbouncer-pooled. |
| NFR-P-008 | COULD   | Draft    | Mobile app cold start (after first visit) | ≤1.0s | Service worker + cached shell. |

### Security

| ID        | Priority | Status   | Metric / Rule | Target / Conformance |
|-----------|----------|----------|---------------|------------------------|
| NFR-S-001 | MUST    | Approved | All web traffic over TLS 1.2+ | 100% | HSTS enabled, no HTTP fallback. |
| NFR-S-002 | MUST    | Approved | Password storage | bcrypt cost ≥12 | Or Argon2id with equivalent work factor. |
| NFR-S-003 | MUST    | Approved | Session timeout (idle) | 8h | Configurable per tenant. |
| NFR-S-004 | MUST    | Approved | Authentication failures | Lock after 5 in 15 min | Per account, per IP. |
| NFR-S-005 | MUST    | Approved | CSRF protection | Enabled on all state-changing requests | Spring Security default. |
| NFR-S-006 | MUST    | Approved | SQL injection | Zero | Parameterised queries only (jOOQ / JPA criteria). |
| NFR-S-007 | MUST    | Approved | XSS | Zero stored, zero reflected | Output encoding by framework, CSP header. |
| NFR-S-008 | MUST    | Approved | Tenant data isolation | 100% | Verified by integration tests per release. |
| NFR-S-009 | MUST    | Approved | Audit log integrity | SHA-256 chain, tamper-evident | Any break surfaces within 24h. |
| NFR-S-010 | MUST    | Approved | GDPR right-to-erasure workflow | Operational | With financial-record retention exception. |
| NFR-S-011 | SHOULD  | Approved | 2FA for admin accounts | Required | TOTP, optional for non-admin. |
| NFR-S-012 | SHOULD  | Approved | Dependency CVE scan | Zero high / critical | Weekly scan, patch within 7 days. |
| NFR-S-013 | SHOULD  | Approved | Penetration test | Annual | External vendor, scope = full app. |

### Availability & Reliability

| ID        | Priority | Status   | Metric | Target |
|-----------|----------|----------|--------|--------|
| NFR-A-001 | MUST    | Approved | Uptime (rolling 30 days) | ≥99.9% |
| NFR-A-002 | MUST    | Approved | Recovery time objective (RTO) | ≤4h |
| NFR-A-003 | MUST    | Approved | Recovery point objective (RPO) | ≤15min (point-in-time recovery from the H2 transaction log / periodic file snapshot) |
| NFR-A-004 | MUST    | Approved | Backup frequency | Continuous WAL + daily full |
| NFR-A-005 | MUST    | Approved | Backup retention | 7 years (financial events), 30 days (operational logs) |
| NFR-A-006 | SHOULD  | Approved | Multi-AZ deployment | Yes for production |
| NFR-A-007 | SHOULD  | Approved | Active-active across two regions | Within 2027 |
| NFR-A-008 | MUST    | Approved | Graceful degradation | UI shows stale-data banner if backend is degraded; read-only mode if writes are blocked. |
| NFR-A-009 | MUST    | Approved | Zero data loss for committed transactions | Durability ≥ 11 nines. |

### Observability

| ID        | Priority | Status   | Metric / Rule | Target |
|-----------|----------|----------|---------------|--------|
| NFR-O-001 | MUST    | Approved | Structured logging (JSON) | 100% of server logs |
| NFR-O-002 | MUST    | Approved | Request tracing (correlation ID) | Propagated through all layers |
| NFR-O-003 | MUST    | Approved | Metrics (RED — Rate, Errors, Duration) | Exposed via /actuator/prometheus |
| NFR-O-004 | MUST    | Approved | Health endpoint | /actuator/health returns 200 if all deps OK, 503 otherwise |
| NFR-O-005 | SHOULD  | Approved | Audit log query API | Available to admin role, paginated, exportable |
| NFR-O-006 | SHOULD  | Approved | Synthetic monitoring | 5 user journeys probed every 5 min from 3 regions |
| NFR-O-007 | SHOULD  | Approved | Real-user monitoring (RUM) | Web vitals tracked per page |

### Maintainability

| ID        | Priority | Status   | Metric / Rule | Target |
|-----------|----------|----------|---------------|--------|
| NFR-M-001 | MUST    | Approved | Cyclomatic complexity (avg per method) | ≤10 |
| NFR-M-002 | MUST    | Approved | Test coverage (line) for `domain` packages | ≥80% |
| NFR-M-003 | MUST    | Approved | Test coverage (line) for `ui` packages | ≥60% |
| NFR-M-004 | MUST    | Approved | Architecture rules | Enforced by ArchUnit tests in CI |
| NFR-M-005 | MUST    | Approved | Static analysis | Zero high-severity issues (SpotBugs / SonarQube) |
| NFR-M-006 | MUST    | Approved | Dependency freshness | Major upgrades within 6 months of upstream GA |
| NFR-M-007 | SHOULD  | Approved | Time to onboard a new engineer to the codebase | ≤3 days (README + docs/vision.md + docs/architecture.md + 1 ticket walkthrough) |
| NFR-M-008 | SHOULD  | Approved | Time to add a new entity end-to-end (spec → code → test → deploy) | ≤1 sprint (2 weeks) |

### Usability

| ID        | Priority | Status   | Metric / Rule | Target |
|-----------|----------|----------|---------------|--------|
| NFR-U-001 | MUST    | Approved | Time for a new warehouse operator to perform their first receive | ≤10 minutes (with training video) |
| NFR-U-002 | MUST    | Approved | Mobile touch targets | ≥44×44 px (Apple HIG) |
| NFR-U-003 | MUST    | Approved | Color contrast (WCAG AA) | 100% of text vs background |
| NFR-U-004 | MUST    | Approved | Form labels | Always visible (no placeholder-only labels) |
| NFR-U-005 | SHOULD  | Approved | Keyboard navigation | All actions reachable, visible focus rings |
| NFR-U-006 | SHOULD  | Approved | Screen reader labels | All icons have `aria-label`, all interactive elements have accessible names |
| NFR-U-007 | SHOULD  | Approved | Reduced motion | Honored via `prefers-reduced-motion` |
| NFR-U-008 | COULD   | Draft    | User satisfaction (CSAT) | ≥4.2 / 5.0 quarterly |

### Internationalisation

| ID        | Priority | Status   | Metric / Rule | Target |
|-----------|----------|----------|---------------|--------|
| NFR-I-001 | MUST    | Approved | Languages supported (UI) | en, de, fr, es, it, nl, pt (PT/BR), pl (P0); additional locales on demand |
| NFR-I-002 | MUST    | Approved | Per-tenant default language | Configured in settings, applied on first login |
| NFR-I-003 | MUST    | Approved | Date / number / currency formats | Per-tenant per the regional settings (CON-T-005) |
| NFR-I-004 | MUST    | Approved | Timezone | Per-tenant, all timestamps stored in UTC and rendered in tenant TZ |
| NFR-I-005 | SHOULD  | Approved | Right-to-left layout | Out of scope for v1 but data model must not preclude it |
| NFR-I-006 | SHOULD  | Approved | Translation source strings | Stored in `.properties` files, not hard-coded |

### Scalability

| ID        | Priority | Status   | Metric | Target |
|-----------|----------|----------|--------|--------|
| NFR-SC-001 | MUST   | Approved | Tenants per instance | ≥100 |
| NFR-SC-002 | MUST   | Approved | Records per tenant per entity (typical) | ≤1M (50M total per instance) |
| NFR-SC-003 | SHOULD | Approved | Linear scaling with read-replica addition | Yes (H2 streaming replication) |
| NFR-SC-004 | SHOULD | Approved | Per-tenant database option for enterprise tier | Available (separate schema or DB) |

---

## Constraints

### Tenant & Multi-tenancy (CON-T)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-T-001 | MUST     | Technical  | Approved | Every domain query MUST execute against the current tenant's database schema. Isolation is enforced physically at the JDBC layer (`holon-saas` `tenant-data` module's `TenantAwareDataSource`, which resolves `TenantContext` and issues `SET search_path TO tenant_<id>` per connection) — not by a `tenant_id` discriminator column. No exception, including reports, exports, audit log, and backups. |
| CON-T-002 | MUST     | Technical  | Approved | The current tenant is established by the authenticated session; switching tenant requires re-authentication or explicit re-authorisation. |
| CON-T-003 | MUST     | Technical  | Approved | A user's roles and permissions are scoped to a tenant; the same user may be `ADMIN` in tenant A and `VIEWER` in tenant B. |
| CON-T-004 | MUST     | Technical  | Approved | Tenant data is physically isolated using the `SCHEMA_PER_TENANT` strategy (`holon-saas` `tenant-data` module): each tenant gets its own database schema (`tenant_<id>`), provisioned and Flyway-migrated during onboarding (`tenant-onboarding` + `TenantSchemaInitializer`). This is `holon-saas`'s recommended default, not an enterprise-tier opt-in. A shared-schema + `tenant_id`-column strategy remains available as a documented fallback for a future low-cost SMB tier but is out of scope for this release. |
| CON-T-005 | MUST     | Technical  | Approved | Numbering patterns, currency, language, timezone, tax codes, and fiscal calendar are all per-tenant. The active configuration is loaded at session start and cached. |
| CON-T-006 | MUST     | Technical  | Approved | A tenant cannot be hard-deleted while it has any data; soft-delete only, with a 7-year retention window for financial records. |
| CON-T-007 | MUST     | Technical  | Approved | Cross-tenant joins, cross-tenant reports, and cross-tenant data export are forbidden at the application layer and verified by integration tests. |

### Financial Integrity (CON-F)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-F-001 | MUST     | Regulatory | Approved | Every journal entry MUST have `Dr_total = Cr_total` exactly. Imbalance is a hard validation error. |
| CON-F-002 | MUST     | Regulatory | Approved | Every journal entry MUST carry `previousHash` (the prior entry's `hash`) and `contentHash` (SHA-256 over date, accounts, amounts, narration). The entry's `hash` is `SHA-256(previousHash + contentHash)`. |
| CON-F-003 | MUST     | Regulatory | Approved | A journal entry, once posted, is immutable. Reversal is a new entry that points back to the original via a `reversesId` field. |
| CON-F-004 | MUST     | Regulatory | Approved | A fiscal period, once closed, is hard-locked. No journal entry may be posted to it. Re-opening requires an explicit unlock action by the CFO, which is itself audited. |
| CON-F-005 | MUST     | Regulatory | Approved | Every GL-impacting event (invoice post, payment, credit note, return, adjustment, composite build, FX revaluation, period close) MUST emit a journal entry in the same transaction as the source event. |
| CON-F-006 | MUST     | Regulatory | Approved | The auto-GL mapping (event type → accounts) is data-driven and stored in `gl_mapping` per tenant, editable by finance. |
| CON-F-007 | MUST     | Regulatory | Approved | The 4-eyes principle applies to all outgoing payments above the per-tenant threshold (default €50K or equivalent in reporting currency). |
| CON-F-008 | MUST     | Regulatory | Approved | 3-way match (PO = goods receipt = vendor bill) is required at the line level before a vendor bill can be paid. Tolerance: ±0.01 in unit price, ±0 in quantity. |
| CON-F-009 | MUST     | Regulatory | Approved | All financial events carry the actor's `app_user.id`; service accounts are forbidden from posting financial events. |
| CON-F-010 | MUST     | Regulatory | Approved | SEPA payment batches are generated as `pain.001.001.03` XML; the XML content is itself hashed and stored alongside the batch record. |

### Audit & Compliance (CON-A)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-A-001 | MUST     | Regulatory | Approved | Every write operation against any entity MUST be recorded in the audit log (actor, timestamp, before-image, after-image, request correlation ID). |
| CON-A-002 | MUST     | Regulatory | Approved | The audit log itself is append-only; deletion of audit records is a hard error. |
| CON-A-003 | MUST     | Regulatory | Approved | Audit log retention: 7 years for financial events, 10 years for German tax-relevant documents (Abgabenordnung §147). |
| CON-A-004 | MUST     | Regulatory | Approved | Personal data is processed under a documented lawful basis per tenant; the lawful basis is recorded in the tenant's `data_processing_register` row. |
| CON-A-005 | MUST     | Regulatory | Approved | Right-to-erasure requests are honoured within 30 days, with the financial-record retention exception (CON-A-003) explicitly applied. |
| CON-A-006 | MUST     | Regulatory | Approved | Tenant data is stored in the tenant's region: EU tenants → EU-Frankfurt, US tenants → US-East, UK tenants → EU-London. |
| CON-A-007 | MUST     | Regulatory | Approved | Personal data is encrypted at rest (AES-256) and in transit (TLS 1.2+). |
| CON-A-008 | MUST     | Regulatory | Approved | GDPR DPA is signed per tenant before any personal data is processed; the signed DPA is stored alongside the tenant record. |
| CON-A-009 | MUST     | Regulatory | Approved | All access to personal data is recorded in the audit log with the access reason (where applicable). |
| CON-A-010 | MUST     | Regulatory | Approved | A daily job verifies the integrity of the journal hash chain and raises an alert on any break. |

### Data Model Invariants (CON-D)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-D-001 | MUST     | Technical  | Approved | Customer number is unique per tenant. |
| CON-D-002 | MUST     | Technical  | Approved | Vendor number is unique per tenant. |
| CON-D-003 | MUST     | Technical  | Approved | SKU is unique per tenant. |
| CON-D-004 | MUST     | Technical  | Approved | Bin location is unique per warehouse. |
| CON-D-005 | MUST     | Technical  | Approved | A sales order, once invoiced, cannot be deleted; only voided (with a reversing invoice). |
| CON-D-006 | MUST     | Technical  | Approved | A purchase order, once received, cannot be deleted; only voided (with a reversing goods receipt and a reversing bill). |
| CON-D-007 | MUST     | Technical  | Approved | A journal entry, once posted, cannot be edited; reversal only. |
| CON-D-008 | MUST     | Technical  | Approved | A fiscal period, once closed, cannot be re-opened without a CFO unlock event. |
| CON-D-009 | MUST     | Technical  | Approved | A composite-item build cannot be reversed by deleting the build record; reversal is a tear-down build with opposite quantities. |
| CON-D-010 | MUST     | Technical  | Approved | A payment cannot be deleted; reversal is a refund payment. |
| CON-D-011 | MUST     | Technical  | Approved | A return (RMA) cannot be deleted; closure only, with disposition recorded. |
| CON-D-012 | MUST     | Technical  | Approved | Stock balance in a bin MUST never go negative; a transfer / ship / adjustment that would take it negative is rejected at the database level. |
| CON-D-013 | MUST     | Technical  | Approved | The sum of allocations on a sales order MUST equal the order quantity per line. |

### Numbering (CON-N)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-N-001 | MUST     | Business   | Approved | Document numbers are generated by the application at the moment the document is first persisted; they are never reused. |
| CON-N-002 | MUST     | Business   | Approved | The numbering pattern is `{prefix}-{yyyy}-{NNNN}` (configurable); the counter resets at year boundary if year-reset is enabled. |
| CON-N-003 | MUST     | Business   | Approved | The counter is per-tenant per-document-type; cross-tenant numbers never collide. |
| CON-N-004 | MUST     | Business   | Approved | Gaps in the sequence are permitted (e.g. on void) but visible in the audit log; the next number is always `MAX + 1`. |

### Roles & Access (CON-R)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-R-001 | MUST     | Technical  | Approved | The six roles (TENANT_ADMIN, SALES, WAREHOUSE, PURCHASER, FINANCE, QUALITY) are defined in the application and applied to `app_user` rows. |
| CON-R-002 | MUST     | Technical  | Approved | Every routable view MUST declare its access rule with `@RolesAllowed(...)` or `@AnonymousAllowed`. The default (no annotation) is deny. |
| CON-R-003 | MUST     | Technical  | Approved | `app_user` and the domain user profile (e.g. `member`, `employee`, `vendor_contact`) are separate; the `app_user` table holds authentication only. |
| CON-R-004 | MUST     | Technical  | Approved | A `FINANCE` user can read all financial data within their tenant; a `SALES` user can read sales data; no role can read across tenants. |
| CON-R-005 | MUST     | Technical  | Approved | Permission checks are enforced in the `domain` layer, not the `ui` layer; the `ui` layer's annotations are for navigation visibility only. |
| CON-R-006 | MUST     | Technical  | Approved | `holon-saas`'s `TenantRole` (VIEWER/MEMBER/ADMIN/OWNER) and the CON-R-001 business roles (TENANT_ADMIN, SALES, WAREHOUSE, PURCHASER, FINANCE, QUALITY) are distinct, non-overlapping concepts and MUST NOT be conflated. `TenantRole` governs SaaS-plan/billing-tier entitlements for the tenant subscription (owned by `holon-saas` `tenant-billing`/`tenant-users`); the CON-R-001 roles are business-function roles the tenant organizes its own users and data around, assigned per `app_user` within the domain layer and enforced via Holon Auth `Permission`/`@Permitted`. A user's `TenantRole` never implies or restricts which CON-R-001 role(s) they hold. |

### UI & Visual (CON-U)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-U-001 | MUST     | Business   | Approved | Every detail view MUST start with a 5-cell summary strip on a dark navy gradient (the `5-cell strip` pattern, see `DESIGN.md` §7). |
| CON-U-002 | MUST     | Business   | Approved | Color = meaning. Gold is money out, teal is money in or stock, violet is customer / recurring, red is reverse / destructive. Never decorative. |
| CON-U-003 | MUST     | Business   | Approved | Every detail view ends with a "3-yr portfolio" annotation in a muted box — concrete numbers, never vague. |
| CON-U-004 | MUST     | Business   | Approved | No AI-suggestion blocks in the UI. The system is operator-driven. |
| CON-U-005 | MUST     | Business   | Approved | No JavaScript tab switching in mockups; tabs are static visual sections. |
| CON-U-006 | MUST     | Business   | Approved | No build step required to view a mockup. Single-file HTML, Google Fonts only. |
| CON-U-007 | MUST     | Business   | Approved | No emoji anywhere in the UI. |
| CON-U-008 | MUST     | Business   | Approved | The mobile UI is a deliberate, touch-first redesign — not a scaled-down desktop view. |
| CON-U-009 | MUST     | Business   | Approved | Outfit is the desktop UI font; Inter is the mobile UI font; JetBrains Mono for numbers. |
| CON-U-010 | MUST     | Business   | Approved | iPhone 14 Pro 390×844 frame for mobile mockups, with notch and 64px bottom tab bar. |

### Tech Stack (CON-S)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-S-001 | MUST     | Technical  | Approved | Java 25, Spring Boot 4.1.0, Holon Platform 12.0.0, Vaadin Flow 25.x for the UI layer. |
| CON-S-002 | MUST     | Technical  | Approved | H2 (2.x, server mode) for OLTP; daily full file-based backup plus point-in-time recovery via the H2 transaction log. Read replicas / multi-node scaling are out of scope for the H2-backed deployment; each tenant's schema (CON-T-004) lives in this same H2 instance, so a tenant needing dedicated infrastructure is migrated to a separate H2/Postgres instance as an escape hatch. |
| CON-S-003 | MUST     | Technical  | Approved | Holon Platform `Datastore` (JPA) for entity mapping and persistence, with plain JavaBean domain classes and `BeanPropertySet`; JPA only when a query cannot be expressed via the Holon Datastore, justified inline. No jOOQ. |
| CON-S-004 | MUST     | Technical  | Approved | Flyway for all schema migrations, versioned `V{NNN}__{name}.sql` files. |
| CON-S-005 | MUST     | Technical  | Approved | Maven for build, with the Spring Boot Maven plugin for packaging. |
| CON-S-006 | MUST     | Technical  | Approved | Testcontainers for integration tests; JUnit 5; AssertJ; Playwright + Mopo for E2E. |
| CON-S-007 | MUST     | Technical  | Approved | ArchUnit for architecture rules, run as part of `mvn test`. |
| CON-S-008 | MUST     | Technical  | Approved | Open-source stack only — no proprietary runtimes, no commercial application servers. |
| CON-S-009 | MUST     | Technical  | Approved | Observability via Micrometer + Prometheus + Grafana; structured (JSON) logging via Logback. |
| CON-S-010 | MUST     | Technical  | Approved | **Platform boundary:** tenant lifecycle, tenant settings/branding, user authentication, roles, invitations, SaaS billing/subscription-plan, onboarding, audit-log infrastructure, and per-tenant schema provisioning/data isolation are provided by the external `holon-saas` foundation (`tenant-core`, `tenant-data`, `tenant-settings`, `tenant-users`, `tenant-security`, `tenant-billing`, `tenant-onboarding`, `tenant-audit`, `tenant-permissions`, `tenant-quotas`, `tenant-invitations`, `tenant-vaadin` modules) and are **exempt** from CON-S-003's Holon Datastore rule and the plugin's Holon-Auth-only rule: that layer uses Spring Security (`UserDetailsService`, BCrypt) and Spring Data JPA / Hibernate, matching `holon-saas`'s own `SAAS_BUILD_RECOMMENDATION.md`. All domain modules (sales, purchasing, inventory, quality, finance — see `docs/entity_model.md`) remain Holon Datastore + Holon Auth per CON-S-003, and rely on `tenant-data`'s `SCHEMA_PER_TENANT` isolation (CON-T-001/CON-T-004) rather than a `tenant_id` column — the Holon Datastore's JDBC connection is transparently routed to the current tenant's schema by `TenantAwareDataSource`, resolved from `TenantContext`. |
| CON-S-011 | MUST     | Technical  | Approved | No domain service may implement its own authentication, tenant provisioning, or SaaS billing — these are delegated to the corresponding `holon-saas` module rather than re-implemented, to avoid duplicating the `TENANT`, `APP_USER`, `ROLE`, tenant-settings, or subscription-plan entities that `holon-saas` already owns. |

### Testing & Release (CON-Q)

| ID       | Priority | Category   | Status   | Rule |
|----------|----------|------------|----------|------|
| CON-Q-001 | MUST     | Operational | Approved | Every use-case spec has at least one end-to-end test. |
| CON-Q-002 | MUST     | Operational | Approved | Every domain class has unit tests for invariants and edge cases. |
| CON-Q-003 | MUST     | Operational | Approved | Tenant-isolation tests are run on every PR — any cross-tenant leak is a P0. |
| CON-Q-004 | MUST     | Operational | Approved | The hash-chain integrity test is run nightly. |
| CON-Q-005 | MUST     | Operational | Approved | A release is blocked if any P0 test fails, any ArchUnit rule fails, or any high-severity static-analysis finding is open. |
| CON-Q-006 | MUST     | Operational | Approved | Every release tag is signed; the tag includes the schema version (from Flyway), the artifact version, and the changelog. |

---

## Traceability summary

This is a sketch — when the entity model and use case specs are written, the
matrix below is filled in.

| FR ID    | UC IDs (planned) | Entities (planned) | Status |
|----------|------------------|---------------------|--------|
| FR-T-*   | UC-T-001 (configure tenant), UC-T-002 (onboard new tenant), UC-T-003 (switch tenant) | *Tenant/user/role delegated to holon-saas* (`tenant-core`, `tenant-settings`, `tenant-onboarding`, `tenant-users` — CON-S-010); *domain-owned:* fiscal_year, tax_code, numbering_pattern | Approved |
| FR-S-*   | UC-S-001 (create customer), UC-S-002 (issue quote), UC-S-003 (confirm order), UC-S-004 (post invoice), UC-S-005 (record payment), UC-S-006 (view AR aging), UC-S-007 (log activity), UC-S-008 (set up subscription) | customer, contact, quote, sales_order, invoice, payment, activity, subscription | Approved |
| FR-P-*   | UC-P-001 (create vendor), UC-P-002 (issue RFQ), UC-P-003 (place PO), UC-P-004 (receive goods), UC-P-005 (process bill), UC-P-006 (run payment batch), UC-P-007 (view AP aging), UC-P-008 (open vendor RMA) | vendor, rfq, purchase_order, goods_receipt, bill, payment_batch, payment_line, vendor_return | Approved |
| FR-I-*   | UC-I-001 (receive stock), UC-I-002 (ship stock), UC-I-003 (adjust stock), UC-I-004 (transfer stock), UC-I-005 (build composite), UC-I-006 (cycle count) | product, warehouse, bin, stock_balance, stock_transfer, stock_adjustment, composite_item, item_group | Approved |
| FR-Q-*   | UC-Q-001 (run inspection), UC-Q-002 (open RMA), UC-Q-003 (disposition returned stock) | quality_inspection, inspection_line, rma, rma_line | Approved |
| FR-F-*   | UC-F-001 (post journal entry), UC-F-002 (close fiscal period), UC-F-003 (run payment batch), UC-F-004 (FX revaluation) | journal_entry, journal_line, chart_of_account, fiscal_period, payment_batch, fx_rate | Approved |
| FR-R-*   | UC-R-001 (run report), UC-R-002 (export report) | (cross-cutting, all entities) | Approved |
| FR-X-*   | (cross-cutting) | (UI patterns) | Approved |
| FR-D-*   | UC-D-001 (boot demo) | (seed data) | Approved |

---

## Change log

| Date       | Change | Author |
|------------|--------|--------|
| 2026-08-30 | Initial catalog from `docs/vision.md` | Mavis (AIUP `/requirements` skill) |
| 2026-08-30 | Renamed "Tenant & Settings" to "Self-Service Configuration" (FR-T) to make explicit that these are tenant-admin self-service actions | Copilot |
| 2026-08-30 | AIUP validation fixes: added missing "so that" clauses to FR-P-006, FR-I-012, FR-R-001, FR-R-002, FR-X-002; added Priority + Category columns to all Constraints tables; corrected CON-S-002 and NFR-A-003 to describe actual H2 capabilities instead of Postgres-only terms (WAL, logical replication) | Copilot |
| 2026-08-30 | Added CON-S-010/CON-S-011 documenting the `holon-saas` platform boundary (tenant/user/auth/billing/onboarding/audit delegated to holon-saas, Spring Security + Spring Data JPA scoped exception); updated FR-T-* traceability accordingly | Copilot |
| 2026-08-30 | Changed `entity_model.md` `TENANT.id`/`APP_USER.id` and all referencing FK columns from `Long`/19 to `String`/50 to match `holon-saas`'s actual String-typed `tenantId`/`userId`; added CON-R-006 clarifying `TenantRole` (billing/plan tier, holon-saas-owned) is distinct from the CON-R-001 business roles (tenant-organized, domain-owned) | Copilot |
