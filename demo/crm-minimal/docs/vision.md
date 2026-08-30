# Vision: IyenSoft CRM/ERP

> AI Unified Process (AIUP) vision document. This is the source of truth for
> *what* the system is and *who* it serves. The companion `requirements.md`
> breaks the vision down into traceable functional requirements, non-functional
> requirements, and constraints. Visual / UX details live in `DESIGN.md`; project
> status and entity coverage live in `vision.md` at the repo root.

---

## Mission

Industrial automation distributors run on **long, multi-step cycles** —
quote → sales order → invoice → payment on the customer side, and
RFQ → purchase order → goods receipt → vendor bill → payment on the supplier
side. Stock has to move between warehouses. Defects have to be inspected, returned,
credited, refunded. Taxes and fiscal periods have to close correctly under
different national regimes. Every step touches a different role, and every role
is currently staring at a different spreadsheet.

**IyenSoft CRM/ERP** is a single multi-tenant web application that handles the full
**order-to-cash** and **procure-to-pay** cycle for industrial automation
distributors — sales, purchasing, inventory, finance, quality, and returns — with
one consistent interface across desktop and mobile, one chart of accounts per
tenant, one audit trail that proves the numbers, and one configuration model
that lets a holding company run a German GmbH, a US Inc, and a UK Ltd out of the
same instance without their data, currencies, or fiscal calendars ever crossing.

The outcome is a finance team that can close the month in three days instead of
ten, a warehouse team that knows what's where in real time, a sales rep that can
quote, confirm, and invoice from a phone on the factory floor, and an auditor
who can trace any number on the balance sheet back to a tamper-evident chain of
journal entries.

---

## Target users

The system serves six distinct roles, each with its own primary view of the data.
The role definitions drive the access model; the views each role spends their day
in drive the design priorities.

- **Tenant administrator** — Configures a single tenant: brand, locale, numbering,
  tax, fiscal year, users, SSO, billing. Lives mostly in the Settings pages.
  Cares about correctness of configuration, not throughput.
- **Sales representative** — Owns the customer relationship: searches the
  catalog, prepares quotes, confirms sales orders, issues invoices, chases
  payments. Spends the day in the customer / quote / order / invoice views,
  often from a phone on the road.
- **Warehouse / fulfillment operator** — Receives incoming goods, picks and
  packs outgoing shipments, performs cycle counts and stock adjustments,
  transfers stock between bins, runs composite-item builds. Lives in the
  receive / goods-receipt / shipment / inventory views, almost always from
  a phone or tablet on the warehouse floor.
- **Purchaser** — Manages vendor relationships: maintains vendor records, runs
  RFQs, places purchase orders, confirms goods receipts, processes vendor bills,
  schedules payments. Spends the day in vendor / RFQ / PO / bill views.
- **Finance / controller** — Owns the books: reviews the chart of accounts,
  posts and approves journal entries, runs the SEPA payment batches, performs
  bank reconciliation, closes fiscal periods, files tax returns. Lives in
  journal-entry / payment / bill / tax / fiscal-year views. Needs the highest
  level of audit certainty.
- **Quality inspector** — Performs incoming and in-process inspections, flags
  defects, opens RMAs, dispositions returned stock (restock, return-to-vendor,
  scrap, credit). Spends the day in QC-inspection / RMA views.

A second axis is **mobile vs. desktop**: warehouse operators and field sales
reps live on the mobile UI; everyone else is on desktop. The mobile UI is not a
scaled-down desktop view — it is a deliberate, touch-first redesign of the same
master-detail pattern (see `DESIGN.md` §19).

---

## Goals

- **Cut order-to-cash cycle time by 50%.** Today the median is ~28 days from
  quote to cash; target is ≤14.
- **Cut month-end close to ≤3 business days** (from 10+). Driven by automated
  journal posting, fiscal-period locks, and a hash-chained audit trail that
  eliminates the "did we re-key this number?" question.
- **Achieve ≥99% on-time payment rate** to vendors, with automated SEPA batches
  and early-pay discount capture.
- **Maintain ≥99.5% inventory accuracy** with real-time bin-level stock, cycle
  counts, and barcode-driven receive / ship.
- **Support ≥50 tenants in production** with strict data isolation, full
  per-tenant brand / locale / currency / tax / fiscal config, and zero
  cross-tenant leakage in any path (UI, API, reports, exports, backups).
- **Achieve ≥60% mobile usage** for warehouse and field-sales workflows.
- **Achieve 100% audit pass rate** with zero findings on annual external audit,
  backed by a SHA-256 hash-chained journal that is tamper-evident.
- **Onboard a new tenant in <1 business day** through a four-step wizard that
  captures brand, locale, numbering, tax, fiscal year, admin, and plan.

---

## Scope

### In scope

- **Multi-tenant SaaS** with strict per-tenant data isolation; tenant can be
  provisioned through a four-step onboarding wizard.
- **38 business entities** across sales, purchasing, inventory, finance,
  quality, returns, projects, and tenant settings (see `vision.md` §3 at the
  repo root for the full list and coverage status).
- **Master-detail UI pattern** with the 5-cell summary strip on every detail
  view (see `DESIGN.md` §7).
- **Desktop UI** (1280px shell, 228px sidebar, 340px left pane) and **mobile UI**
  (iPhone 14 Pro 390×844 frame, touch-first redesign of the same skeleton).
- **Role-based access** for the six target users above, expressed declaratively
  on every routable view (the `@RolesAllowed` pattern from the AIUP book-library
  reference).
- **Auto-GL posting engine** that emits a journal entry for every financially
  significant event (invoice post, payment, credit note, return, adjustment,
  composite build, exchange-rate revaluation). Each entry is **hash-chained
  with SHA-256** — the entry's `hash` is `SHA-256(previousHash + contentHash)`,
  making any past edit detectable.
- **3-way matching** (PO = goods receipt = vendor bill) with line-level
  tolerance and a blocked-payment state until all three reconcile.
- **SEPA batch payments** (`pain.001.001.03` XML) with early-pay discount
  capture, automatic retry on rejection, and per-bank balance guardrails.
- **Reverse flow** for sales returns and purchase returns (six-step workflow:
  request → RMA → receive → inspect → refund/credit) with shared components
  and per-side color coding.
- **Quality loop**: QC inspection → RMA → credit note, with three dispositions
  (restock, return-to-vendor, scrap).
- **Multi-currency** with daily ECB / Fed FX rate capture, per-tenant reporting
  currency, and unrealised FX revaluation on period close.
- **Multi-tax** with per-tenant active tax codes (EU VAT standard/reduced,
  reverse charge, exempt, export; US state sales tax; UK VAT).
- **Fiscal year** with monthly periods, hard lock on close, scheduled close
  reviews, and external-auditor sign-off.
- **Numbering** per tenant for nine document types (SO, INV, QU, PO, BIL, PAY,
  REC, JE, RMA) with year-reset and 4-digit padding.
- **Reports**: ten inventory reports (stock-on-hand, stock-valuation, slow
  movers, dead stock, ABC analysis, etc.) and ten financial reports
  (P&L, balance sheet, cash flow, AR aging, AP aging, trial balance, etc.).
- **Mobile** parity for the warehouse and field-sales workflows
  (receive, ship, transfer, count, RMA, expense, payment, view-360).
- **Demo data** seeded on first boot so the application is fully usable without
  external setup.

### Out of scope

- **Manufacturing execution (MES)** — routing, work orders, machine telemetry,
  labour tracking.
- **POS / retail checkout** — cash drawers, receipt printers, barcode POS.
- **Payroll** — employee pay, tax filings, benefits administration.
- **E-commerce storefront / customer self-service** — customers do not log in.
  Orders come from sales reps or are imported via API.
- **Marketing automation** — email campaigns, lead scoring, drip sequences.
- **Time tracking / project timesheets** — projects get budgeting and basic
  cost rollup, but no timesheet entry.
- **Multi-currency bank reconciliation beyond reporting currency** — each
  bank account has a single native currency; FX is tracked but not auto-matched.
- **Inventory lot / serial tracking at unit level** — bin-level stock only;
  serial numbers are a future extension.
- **3PL / drop-ship fulfilment integration** — assumed all stock is in
  tenant-owned warehouses.
- **Mobile native apps** — the mobile UI is a responsive web app, not a
  native iOS / Android build.

---

## Constraints

- **Multi-tenant data isolation** is a regulatory and trust requirement.
  Cross-tenant leakage in any path (UI, API, report, export, backup, log) is
  a P0 incident.
- **EU + UK + US regulatory compliance**: GDPR, UK Data Protection Act 2018,
  US state-level privacy laws (CCPA / CPRA). Each tenant stores data in its
  own region (EU tenants → EU-Frankfurt, US tenants → US-East, etc.).
- **ISO 27001** certification roadmap — current state is "in flight",
  target certification 2027.
- **SOC 2 Type II** is a sales-driven requirement for enterprise tenants;
  audit-ready logging is mandatory.
- **Hash-chained audit trail** (SHA-256, see Goals) is a hard requirement for
  journal entries — no exceptions, even for reversing entries.
- **7-year audit log retention** for all financial events; **10-year** for
  German tax-relevant documents (German Abgabenordnung §147).
- **Hard fiscal-period locks**: once a month is closed, no journal entry can
  be posted to it without an explicit unlock by the CFO, which is itself
  audited.
- **GDPR** lawful-basis registration, right-to-erasure workflow (with the
  usual financial-record retention exception), and signed DPA per tenant.
- **4-eyes principle** for all outgoing payments > €50K (or equivalent in
  tenant reporting currency): required secondary approver, recorded in the
  audit log.
- **SEPA pain.001.001.03** is the only accepted format for EU SEPA batches;
  no legacy formats.
- **3-way match** is required for every vendor bill: PO + goods receipt +
  invoice must reconcile at the line level within tolerance before payment
  is allowed.
- **Tenant data residency**: EU tenants → EU-Frankfurt, US tenants → US-East
  (with US-West failover), UK tenants → EU-London. Cross-region replication
  is forbidden for personal data.
- **Open-source stack** is a soft constraint: prefer Holon Platform, Vaadin Flow, Spring Boot,
  H2, JPA, Flyway, and Apache-2 / MIT libraries. No
  proprietary runtimes or commercial application servers.
- **No AI-suggestion blocks in the UI** — explicitly removed by product
  decision. The system is operator-driven, not AI-driven.
- **Single-tenant deployments are not supported** — the application is
  multi-tenant by design; deploying a single-tenant instance is out of scope
  for this product (a partner may build a fork).

---

## Success measures

The team uses these to decide whether the product is healthy at any point in
time. Each is measurable from the application's own data.

| Measure                                        | Target     | Source of truth              |
|------------------------------------------------|------------|------------------------------|
| Order-to-cash cycle (quote → cash)             | ≤14 days   | invoice + payment timestamps |
| Month-end close duration                       | ≤3 days    | fiscal-period close log      |
| Vendor on-time payment rate                    | ≥99%       | payment batches              |
| Inventory accuracy                             | ≥99.5%     | cycle count variance report  |
| AR collection cycle (D SO)                     | ≤45 days   | AR aging report              |
| Active tenants in production                   | ≥50        | tenant registry              |
| Mobile share of warehouse / field workflows    | ≥60%       | user-agent analytics         |
| External audit findings                        | 0          | auditor letter               |
| Tenant onboarding time                         | ≤1 day     | tenant registry              |
| Application uptime (rolling 30 days)           | ≥99.9%     | observability                |
| P0 incident count (rolling 90 days)            | ≤1         | incident tracker             |
| SEPA batch failure rate (post-validation)      | <1%        | payment service              |
| FX rate freshness (last business day)          | 100%       | FX rate log                  |
| Journal hash-chain integrity check             | 100% pass  | nightly validator            |

A failure on any of these is reviewed in the next steering meeting and turned
into either a bug fix, a use-case spec change, or a deliberate decision to
adjust the target.
