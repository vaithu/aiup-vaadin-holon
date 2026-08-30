# HTML Mockup → Holon Component Map

> **Purpose**: authoritative mapping from the Acme CRM HTML mockup patterns
> (desktop list, desktop detail, desktop new-form, mobile list, mobile detail,
> mobile new-form) to the exact Holon Platform + Vaadin Flow components and APIs
> that must be used to reproduce each region. Read this file **before** writing
> any view code for a customer-facing screen.

---

## Shell & Navigation

| HTML pattern | Holon component | Correct API |
|---|---|---|
| Top appbar — brand + global search + bell + user chip | `AppShellLayout` | `AppShellLayout.builder().navbarBrand(Localizable, RouteClass).search(Localizable.of("Search…","app.search")).notifications(...).user(...).configure(this)` |
| Icon-only 64 px side rail with hover tooltips | `SideNavBuilder` inside `AppShellLayout.nav(...)` | `SideNavBuilder.create().withNavItem(Localizable.of("Customers","nav.customers"), CustomerListView.class, VaadinIcon.USER.create()).add().buildWrapper()` |
| Breadcrumb (`CRM › Customers › C-2026-0023`) | `Breadcrumb` | `Components.breadcrumb().item(Localizable.of("CRM","nav.crm"), HomeView.class).item(Localizable.of("Customers","nav.customers"), CustomerListView.class).item(Localizable.of("C-2026-0023","")).build()` |
| Mobile back-chevron top bar + title + end action | `AppBar` | `new AppBar()` with `.addToStart(backBtn)`, `.addToEnd(actionBtn)` |

---

## List View (desktop `customer-list.html`)

| HTML region | Holon component | Correct API |
|---|---|---|
| 5-cell KPI stat row | `Highlight` (×5) inside `Components.hl()` | `Components.highlight(Localizable.of("Total customers","crm.kpi.totalCustomers"), "342").build()` |
| Filter chip row (`All 342`, `★ T1 28` …) | `ChipGroup` + `Chip` | `ChipGroup.create().ariaLabel(Localizable.of("Customer filters","crm.filter.ariaLabel")).addChip(Chip.of(Localizable.of("All","crm.filter.all"), 342), true).onSelect(this::onFilterChange).build()` |
| Search input in filter bar | wired into `ListingBundle` | `.search(Localizable.of("Search by name, VAT, city…","crm.customer.list.searchPlaceholder"))` on the bundle builder |
| Data table with sticky header + status + tier columns | `ListingBundle` | `Components.listing(Customer.class).columns("customerNumber","name","region","tier","owner","openAr","openSos","arr","statusId").pageSizes(10,25,50).defaultPageSize(10).fetch((q,t,s)->svc.findSlice(q.getOffset(),q.getLength(),t)).search(...).emptyState().noResultsState().build()` |
| Status pill (`● Active`, `14d overdue`) | `StatusBadge` | `Components.statusBadge(Localizable.of("Active","crm.status.active"), StatusBadge.Variant.SUCCESS)` |
| Tier badge (`★ T1`, `Prospect`) | `Tag` | `new Tag(Localizable.of("★ T1","crm.tier.t1"))` |
| Pagination bar | built into `ListingBundle` footer | `bundle.footer()` |
| "New customer" primary button | `ButtonBuilder` | `ButtonBuilder.create().text(Localizable.of("New customer","crm.action.newCustomer")).primary().icon(VaadinIcon.PLUS).onClick(e->Navigator.get().navigateTo(NewCustomerView.class)).build()` |
| Icon-only toolbar buttons (Filter / Export / Columns) | `ButtonBuilder` icon-only | `ButtonBuilder.create().icon(VaadinIcon.FILTER).icon().ariaLabel(Localizable.of("Filter","crm.action.filter")).build()` |
| Sort control | `ButtonBuilder` secondary + `Components.hl()` | secondary button wired to listing sort |

---

## Detail View (desktop `customer-detail.html`)

| HTML region | Holon component | Correct API |
|---|---|---|
| Master list panel (left 340 px) + fluid detail panel | `MasterDetailLayout` | `Components.masterDetail(Customer.class).master(m->m.listing(svc::findSlice).columns("customerNumber","name","openAr","statusId").search(...)).detail(d->d.content(c->buildDetail(c))).withUrlSync(c->String.valueOf(c.getId()),id->svc.findById(Long.valueOf(id))).build()` |
| Dark gradient 5-cell account strip | `HeroStrip` | `Components.heroStrip().variant(HeroStrip.Variant.DEFAULT).header(h->h.name(c.getName()).meta(c.getCustomerNumber()).starred(c.isVip())).tag(c.getTierLabel(), HeroStrip.TagVariant.CONTRAST).tag(c.getStatusLabel(), HeroStrip.TagVariant.INFO).cell(cl->cl.header("Account health").content(c.getAccountHealth())).cell(cl->cl.header("Open AR").content(formatAmount(c.getOpenAr())).valueVariant(HeroStrip.ValueVariant.DANGER)).cell(cl->cl.header("Open SOs").content(String.valueOf(c.getOpenSos()))).cell(cl->cl.header("ARR").content(formatAmount(c.getArr()))).cell(cl->cl.header("NPS").content(String.valueOf(c.getNps()))).build()` |
| Tab bar (Overview / Orders / Invoices / Activity / Files) | `Components.lazyTabs()` | `.tab(Localizable.of("Overview","crm.tab.overview"), ()->buildOverview(customer)).tab(Localizable.of("Orders","crm.tab.orders"), ()->buildOrders(customer))…build()` |
| Account & terms card (KV grid + editable fields) | `EntityFormPanel` read-only | `EntityFormPanel.bean(Customer.class).properties("customerNumber","vatId","statusId","tierId","regionId","industryId","paymentTerms","sepaMandateRef","ownerId","sourceId","website").readOnly().noFooter().build()` |
| Addresses card (2 address blocks side-by-side) | `Components.panel()` + `Components.row()` | two `Components.panel()` instances inside a `Components.row()` |
| AR aging bar | `ArAgingBar` | `ArAgingBarBuilder.create().header(h->h.title(Localizable.of("AR Aging","crm.arAging.title")).variant(ArAgingBar.Variant.WARNING)).content(c->c.segment(Localizable.of("0–30d","crm.arAging.current"),formatAmount(current),pct1,ArAgingBar.Variant.SUCCESS).segment(…)).footer(f->f.left(Localizable.of("DSO: 34d","crm.arAging.dso")).center(Localizable.of("96.2% collection","crm.arAging.collection")).right(Localizable.of("Next DD: 13 Aug","crm.arAging.nextDd"))).build()` |
| Active contacts grid (2-col avatar cards) | `ListingBundle` with custom row renderer + `Components.avatar()` | `Components.listing(Contact.class).columns("name","title","email").fetch(…).build()` — avatar via `Components.avatar(initials).name(fullName).colorIndex(i).build()` |
| Open orders compact table | `ListingBundle` | `Components.listing(SalesOrder.class).columns("orderNumber","description","lineCount","allocPct","pickedQty","totalAmount").fetch(q->orderSvc.findSlice(customerId,q.getOffset(),q.getLength())).build()` |
| Open invoices table (overdue row highlighted) | `ListingBundle` with row class provider | same pattern; overdue row receives CSS class via row renderer |
| Activity log / timeline | `TimelineStepper` | `Components.timelineStepper().pageSize(6).onLoadMore(page->activitySvc.findPage(customerId,page)).build()` |
| Files list with download action | `ListingBundle` | `Components.listing(CustomerFile.class).columns("name","uploadedBy","uploadedAt","sizeKb").withRowAction(VaadinIcon.DOWNLOAD, Localizable.of("Download","crm.action.download"), f->download(f)).build()` |
| Account 360 totals card (notes textarea + totals) | `Components.panel()` + `Input.stringArea()` + `Components.vl()` | notes field bound to `EntityFormPanel`; totals rows as `Components.hl()` pairs |
| Action buttons (Re-send dunning, WhatsApp, Export…) | `ButtonBuilder` | `.primary()` / `.secondary()` / `.error()` variants as appropriate |
| Destructive confirmation (delete / discard) | `AlertDialog` | `AlertDialog.builder().title(Localizable.of("Discard changes?","crm.dialog.discard.title")).variant(Alert.Variant.DESTRUCTIVE).cancelText(Localizable.of("Keep editing","crm.dialog.discard.cancel")).confirmText(Localizable.of("Discard","crm.dialog.discard.confirm")).onConfirm(()->Navigator.get().navigateTo(CustomerListView.class)).build().open()` |

---

## New Customer Form (desktop `customer-new.html`)

| HTML region | Holon component | Correct API |
|---|---|---|
| Full-page multi-step scaffold (header + DRAFT badge + sticky save bar + step tabs) | `EntityCreationForm` + `FormStepCard` | `Components.entityCreationForm().title(Localizable.of("New customer","crm.page.newCustomer.title")).draftBadge(Localizable.of("Unsaved draft","crm.badge.unsavedDraft")).steps(step1,step2,step3,step4,step5).headerAction(cancelBtn,saveDraftBtn,createBtn).barAction(cancelBarBtn,saveDraftBarBtn,createBarBtn).build()` |
| Step progress indicator (Company / Address / Contacts / Banking / Workflow) | `FlowStepper` | `Components.stepper().steps(Localizable.of("Company","crm.step.company"),Localizable.of("Address","crm.step.address"),Localizable.of("Contacts","crm.step.contacts"),Localizable.of("Banking","crm.step.banking"),Localizable.of("Workflow","crm.step.workflow")).currentStep(0).ariaLabel(Localizable.of("Form steps","crm.stepper.ariaLabel")).build()` |
| Company form grid (legal name, VAT, DUNS, website…) | `EntityFormPanel` per step — no footer | `EntityFormPanel.bean(Customer.class).properties("customerNumber","legalName","displayName","tradingAs","vatId","dunsNumber","website").autoLabels(true).responsiveSteps(s->s.mobile(1).tablet(2).desktop(3)).noFooter().build()` |
| Address form grid | `EntityFormPanel` per step — no footer | `EntityFormPanel.bean(CustomerAddress.class).properties("street","postalCode","city","stateRegion","countryId").autoLabels(true).noFooter().build()` |
| Classification grid (tier, region, industry, source, owner, status) | `EntityFormPanel` per step — no footer | all lookup fields bound via `.bind("tierId", Input.singleSelect(Long.class).allowCustomValues(true).onCustomValueSet(…).build())` |
| Tier segmented pill selector | `Components.buttonGroup()` | `Components.buttonGroup().ariaLabel(Localizable.of("Tier","crm.customer.tier")).content(t1Btn,t2Btn,t3Btn,prospectBtn).build()` — styled via CSS token classes |
| Region / Industry / Country / Source dropdowns (creatable) | `Input.singleSelect(Long.class).allowCustomValues(true).onCustomValueSet(v->{ Long id=svc.findOrCreate(v); input.refresh(…); input.setValue(id); })` | bound via `.bind(...)` on `EntityFormPanel` |
| Toggle switches (auto-send email, sanctions check, DUNS enrichment, Slack notify) | `Input.boolean_().styleName("switch")` | bound via `.bind(...)` on the Workflow step `EntityFormPanel` |
| Live preview sidebar card | `LivePreviewCard` | `Components.livePreviewCard(Localizable.of("Live preview","crm.preview.title"))` — updated via `ValueChangeListener` on form fields |
| Required-to-save checklist | `ChecklistPanel` | `Components.checklistPanel().addItem(Localizable.of("Legal name","crm.checklist.legalName"),"",ItemState.DONE).addItem(Localizable.of("Primary contact","crm.checklist.primaryContact"),Localizable.of("recommended","crm.checklist.recommended"),ItemState.PENDING).build()` |
| Sticky bottom save bar (autosave status dot + Cancel + Save draft + Create) | `StickyActionBar` | `Components.stickyActionBar().left(autosaveIndicator).right(cancelBtn,saveDraftBtn,createBtn).build()` |
| Success / error toasts | `NotificationUtil` | `NotificationUtil.notificationSuccess(Localizable.of("Customer created","crm.notify.customerCreated"))` / `NotificationUtil.notificationError(...)` |
| Unsaved-changes inline warning | `Alert` | `Alert.builder(Alert.Variant.WARNING).title(Localizable.of("Unsaved changes","crm.alert.unsaved.title")).description(Localizable.of("Changes will be lost if you navigate away.","crm.alert.unsaved.description")).build()` |

---

## Mobile Views

| HTML pattern | Holon component | Notes |
|---|---|---|
| Mobile customer list (FAB + chip filter + card rows) | `ListingBundle` with `.mobileViewColumn(LitRendererBuilder.<T>gridCell()...)` | FAB = `ButtonBuilder.create().primary().icon(VaadinIcon.PLUS).ariaLabel(Localizable.of("New customer","crm.action.newCustomer"))` — floated via `styles.css` |
| Mobile detail (hero + quick-action bar + card stack) | `MasterDetailLayout` with `.withMobileSheet(Sheet.Side.BOTTOM)` | Detail opens as bottom `Sheet`; master fills full screen on mobile |
| Mobile new form (draft badge + tab bar + bottom save bar) | Same `EntityCreationForm` scaffold | `ResponsiveDiv` + `@media` CSS collapses to single column; `StickyActionBar` sticks to bottom |
| Dark gradient hero header + mini metric strip | `HeroStrip` | Same component as desktop — stacks to 2×2 metric grid on mobile via built-in responsive CSS |
| Horizontal scrollable chip filter bar | `ChipGroup` | Rendered as `role="group"` with `aria-label`; horizontal overflow handled in `styles.css` |
| Bottom sticky save / create bar | `StickyActionBar` | Same component; CSS positions it at viewport bottom |
| Slide-in detail panel (mobile) | `Sheet` | `Components.sheet(Sheet.Side.BOTTOM).title(Localizable.of("Customer detail","crm.sheet.customerDetail.title")).content(detailComponent).build()` |
| Quick-action bar (Call / Email / WhatsApp / More) | `Components.hl()` of `ButtonBuilder` icon buttons | each button `.ariaLabel(Localizable.of("Call","crm.action.call"))` |

---

## Global Rules Derived from This Mapping

1. **Every categorical field** (status, tier, region, industry, country, source, payment terms) is a `Long` FK to a lookup table — rendered as `Input.singleSelect(Long.class).allowCustomValues(true)`.
2. **No raw status strings** in grid cells — use `StatusBadge` for status, `Tag` for tier/category.
3. **Every listing** must call `.emptyState()` and `.noResultsState()` — never leave the grid blank.
4. **Every fetch callback** uses `svc.findSlice(q.getOffset(), q.getLength(), …)` — `svc.findAll()` is banned inside fetch lambdas.
5. **HeroStrip** is the mandatory hero card for all entity detail views — no hand-rolled gradient `Div`.
6. **ArAgingBar** is the mandatory AR aging visualisation — no hand-rolled bar via CSS widths.
7. **MasterDetailLayout** handles both desktop split and mobile sheet automatically — do not build two separate views.
8. **EntityCreationForm + FormStepCard** is the mandatory scaffold for multi-section create forms — do not assemble a tab sheet + form manually.
9. **StickyActionBar** is the mandatory save bar at the bottom of create/edit forms — do not use a raw `HorizontalLayout` with `position:sticky`.
10. **LivePreviewCard + ChecklistPanel** are mandatory right-panel companions on all new-entity forms.
