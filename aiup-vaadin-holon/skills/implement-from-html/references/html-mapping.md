&#xFEFF;# HTML Region → Holon Vaadin Component Mapping

> **Source of truth**: verified against `com.holonplatform.vaadin.flow.demo.ui.views.*` (August 2026)
> and `vaithu/holon-vaadin-flow` branch `modernize/java-20260526093038`.
> Every API below compiles. Do **not** invent methods or use class names that differ from this table.

Use this table to select the correct Holon Vaadin Flow component for each HTML region
identified in a mockup. If no Holon equivalent exists, **stop immediately and ask the developer**.

---

## Layout / shell regions

| HTML pattern | Correct Holon component | Notes |
|-------------|-------------------------|-------|
| Full creation page (multi-step form + sticky bar) | `Components.entityCreationForm()...build()` → `EntityCreationForm` | See `NewCustomerDemoView` |
| Step card inside creation page | `Components.formStepCard().stepNumber(n).totalSteps(N).title("...").content(...).build()` → `FormStepCard` | |
| App shell / main layout | `Components.appShell().navbarBrand("...").nav(nav).build()` → `AppShellLayout` | |
| Side nav | `SideNavBuilder.create().withNavItem("Label", View.class, VaadinIcon.X.create()).add().buildWrapper()` | |
| Responsive viewport slots (mobile/desktop) | Prefer `ResponsiveDiv.configure(this).slotOnce(ViewMode.DESKTOP, ()->...).build()` for simpler cases; use CSS `@media` in `styles.css` for complex responsive behaviour | See `css-extraction.md` §"Responsive breakpoints with `@media`" |
| Breadcrumb | `BreadcrumbItem` / `BreadcrumbPage` (from `com.holonplatform.vaadin.flow.vaadinplus.components`) | |
| Tab bar (bare selector) | `TabsBuilder.create().withTab("A","B","C").build()` — produces a bare `Tabs` selector; wire content via `addSelectedChangeListener` | See `TabsDemoView`; use `LazyTabsBuilder` for built-in lazy switching |
| Tab bar with counter badges | `TabsBuilder.create().withTab("Inbox", 12).withTab("Drafts", 3).build()` | `withTab(label, count)` appends a numeric badge |
| Tab bar with icons | `TabsBuilder.create().withTab(new Tab(VaadinIcon.HOME.create(), new Span("Home"))).build()` | Pass `Tab` instances for icon+label |
| Tab bar equal-width | `.flexGrowForEnclosedTabs(1.0)` on `TabsBuilder` | |
| Tab bar vertical | `.orientation(Tabs.Orientation.VERTICAL)` on `TabsBuilder` | |
| Lazy tab content switching | `LazyTabsBuilder` (built-in lazy content management) | Use instead of `TabsBuilder` + manual panels |
| Card / panel | Plain `Div` with CSS class — no Holon wrapper needed | |
| Footer / sticky action bar | Included in `EntityCreationForm` (`barAction(...)`) or manual `Div` with CSS | |

---

## Data display regions

| HTML pattern | Correct Holon component |
|-------------|------------------------|
| Data table / grid | `Components.listing(T.class).columns(...).fetch((q,text,sort)->...).build()` — layout: `bundle.toolbar()` + `bundle.grid()` + `bundle.footer()` |
| Paginated list | Same `Components.listing(T.class)` with `.pageSizes(...)` and `.defaultPageSize(...)` |
| Read-only detail | `EntityFormPanel.bean(T.class)...readOnly().build()` + `form.setBean(bean)` |
| Master-detail split | `MasterDetailLayout<T>` from `com.iyensoft.vaadin.flow.components` — see `MasterDetailDemoV2` |
| Timeline / activity feed / audit log | `TimelineStepper` from `com.holonplatform.vaadin.flow.vaadinplus.components` — see `TimelineStepperDemoView`. Live prepend: `timeline.prependEntries(List.of(new AuditEntry(...).severity(Severity.SUCCESS)))` |
| Calendar / scheduler | `VaadinCalendar` from `com.holonplatform.vaadin.flow.calendar`. Usage: `new VaadinCalendar()`, `cal.setView(CalendarView.MONTH/WEEK/DAY/AGENDA)`, add events via `cal.addCalendarReadyListener(e -> cal.setEvents(events))`. Read-only: `cal.setReadOnly(true)`. Dark: `cal.setTheme(CalendarTheme.DARK)`. Events: `new CalendarEvent.Builder().title("...").start(ldt).end(ldt).color("#hex").build()` |
| Kanban board | `KanbanBoard.<T,C>builder().withColumns(...).withItemIdentifierProvider(...).withItemColumnProvider(...).withItemColumnUpdater(...).withCardRenderer(...).withMoveHandler(...).withItems(items).build()` — see `KanbanBoardDemoView` |
| Chart / graph | `ChartJs.builder().type(ChartType.BAR).categories(...).series(...).height("300px").build()` — from `com.holonplatform.vaadin.flow.components.chartjs`. Supports BAR, LINE, PIE, DOUGHNUT, POLAR_AREA, RADAR, BUBBLE, SCATTER. See `ChartJsDemoView` |
| List row with primary/secondary text and optional prefix/suffix | `new ListItem(primary, secondary)` or `new ListItem(prefixComponent, primary, secondary, suffixComponent)` — from `com.holonplatform.vaadin.flow.components.ListItem` |

---

## Form / input regions

> All input builders are accessed via `com.holonplatform.vaadin.flow.components.Input` (static methods).
> **Not** `Components.input.*` — that namespace does **not** exist.

| HTML element | Inferred type | Correct Holon input |
|-------------|--------------|---------------------|
| `<input type="text">` | `String` | `Input.string().label("...").build()` |
| `<textarea>` | `String` | `Input.stringArea().label("...").build()` |
| `<input type="number">` (integer) | `Integer` | `Input.number(Integer.class).label("...").build()` |
| `<input type="number">` (decimal) | `Double` / `BigDecimal` | `Input.number(Double.class).label("...").build()` |
| `<input type="date">` | `LocalDate` | `Input.localDate().label("...").build()` |
| `<input type="datetime-local">` | `LocalDateTime` | `Input.localDateTime().label("...").build()` |
| `<input type="email">` | `String` | `Input.string().label("...").build()` (+ email validator) |
| `<input type="checkbox">` | `Boolean` | `Input.boolean_().label("...").build()` |
| Toggle / switch | `Boolean` | `Input.boolean_().label("...").styleName("switch").build()` |
| `<select>` (single) | `String` | `Input.singleSelect(String.class).items(...).label("...").build()` |
| `<select>` (lookup entity FK) | `Long` | `Input.singleSelect(Long.class).items(svc.findAll(), Entity::getId, Entity::getName).label("...").build()` |
| Radio button group | `String` | `Input.singleOptionSelect(String.class).items(...).label("...").build()` |
| `<select multiple>` / checkbox group | `Set<String>` | `Input.multiOptionSelect(String.class).items(...).label("...").build()` |
| `<input type="file">` | — | ⚠️ **Stop and ask the developer** |
| Complete `<form>` with multiple fields | bean | `EntityFormPanel.bean(MyBean.class).properties(...).autoLabels(true).bind("field", input).noFooter().build()` |
| Search / filter field (produces QueryFilter) | `String` | `FilterInput.string(PROPERTY)` — from `com.holonplatform.vaadin.flow.components.FilterInput` |
| Number filter | `N extends Number` | `FilterInput.number(PROPERTY, Long.class)` |
| Number range filter (from–to) | `FilterInput.Range<N>` | `FilterInput.numberRange(PROPERTY, Long.class)` |
| Date filter | `LocalDate` | `FilterInput.localDate(PROPERTY)` |
| Date range filter | `FilterInput.Range<LocalDate>` | `FilterInput.localDateRange(PROPERTY)` |
| DateTime filter | `LocalDateTime` | `FilterInput.localDateTime(PROPERTY)` |
| DateTime range filter | `FilterInput.Range<LocalDateTime>` | `FilterInput.localDateTimeRange(PROPERTY)` |
| Boolean tri-state filter | `Boolean` | `FilterInput.bool(PROPERTY)` |
| Lookup FK filter (single-select) | `Long` | `FilterInput.from(Input.singleSelect(Long.class).items(svc.findAll(), E::getId, E::getName).build(), v -> Optional.of(PROPERTY_ID.eq(v)))` |
| Auto-inferred filter from property type | any | `FilterInput.of(PROPERTY)` — infers type automatically |
| Custom filter wrapping any Input | any | `FilterInput.from(Input.string().build(), value -> Optional.of(PROP.startsWith(value)))` |
| Advanced multi-field dynamic search panel | bean | `DynamicFilterPanel.of(MyBean.class)` from `com.holonplatform.vaadin.flow.vaadinplus.components` — see `FilterPanelDemoView` |
| OTP / PIN input | — | ⚠️ No Holon equivalent — **stop and ask the developer** |

---

## Filter group regions

| HTML pattern | Correct Holon API |
|-------------|------------------|
| Group of filter inputs (drives a grid) | `FilterInputGroup` — add each `FilterInput` by property; get combined `Optional<QueryFilter>` via `getQueryFilter()` |
| Filter panel with apply / reset buttons | `FilterInputForm` — wraps `FilterInputGroup` with submit/reset buttons |
| Advanced dynamic filter builder | `DynamicFilterPanel.of(MyBean.class)` — introspects bean fields; in-memory: `.toPredicate()`; datastore: `.getQueryFilter()` |
| Wire filter panel to Kanban board | `board.refreshOnFilterChange(filterGroup)` or one-liner `board.bindFilters(filterGroup, (query, filter) -> ...)` |

---

## Action / button regions

| HTML pattern | Correct Holon API |
|-------------|------------------|
| Primary action (`class="btn-primary"`, `type="submit"`) | `ButtonBuilder.create().text("...").primary().onClick(...).build()` |
| Destructive / danger button | `ButtonBuilder.create().text("...").error().onClick(...).build()` |
| Secondary / cancel button | `ButtonBuilder.create().text("...").secondary().onClick(...).build()` |
| Ghost / tertiary button | `ButtonBuilder.create().text("...").tertiary().onClick(...).build()` |
| Icon-only button | `ButtonBuilder.create().icon(VaadinIcon.X).icon().ariaLabel("Close").build()` |
| Pre-configured delete | `ButtonBuilder.create().preset(ButtonPreset.DELETE).onClick(...).build()` |
| Shortcut | `Components.button().text("...").primary().build()` — delegates to `ButtonBuilder` |

> ⛔ **Wrong**: `Components.button().text("...").themeVariants(ButtonVariant.LUMO_PRIMARY)` — do not use raw Vaadin `ButtonVariant`.
> Use semantic variant methods: `.primary()`, `.error()`, `.secondary()`, `.tertiary()`.

---

## Feedback / overlay regions

| HTML pattern | Correct Holon API |
|-------------|------------------|
| Success toast | `NotificationUtil.notificationSuccess("...")` or `NotificationBuilder.create().text("...").success().build().open()` |
| Error toast | `NotificationUtil.notificationError("...")` or `NotificationBuilder.create().text("...").error().build().open()` |
| Warning toast | `NotificationBuilder.create().text("...").warning().build().open()` |
| Confirmation dialog | `AlertDialog.builder().title("...").description("...").confirmText("...").variant(Alert.Variant.DESTRUCTIVE).onConfirm(()->...).open()` |
| Inline alert / banner | `Alert.builder(Alert.Variant.WARNING).title("...").description("...").build()` |
| Loading spinner | ⚠️ No Holon equivalent — **stop and ask the developer** |

> `NotificationUtil` is `com.holonplatform.vaadin.flow.components.utils.NotificationUtil` — it IS in Holon.
> `AlertDialog` is `com.holonplatform.vaadin.flow.vaadinplus.components.AlertDialog`.

---

## Badge / status indicator regions

| HTML pattern | Correct Holon component |
|-------------|------------------------|
| Inline status pill / tag | `new Badge(text)` or `new Badge(text, BadgeColor.SUCCESS)` — from `com.holonplatform.vaadin.flow.components.Badge`. Colors: `NORMAL`, `SUCCESS`, `ERROR`, `WARNING`, `CONTRAST`, `PRIMARY`. Size: `BadgeSize.S`. Shape: `BadgeShape.PILL` |
| Localizable badge text | `new Badge(Localizable.of("Done"), BadgeColor.SUCCESS)` |
| Status pill inside a Kanban card | `KanbanStatusBadge.success("Done")` / `.error("Blocked")` / `.warning("Review")` / `.info("In Progress")` / `.defaultVariant("Pending")` / `.of(text, KanbanStatusBadge.Variant.WARNING)` — from `com.holonplatform.vaadin.flow.components.kanban` |
| Column header color chip on Kanban | `KanbanColumn.of(Status.DONE, "Done", KanbanStatusBadge.COL_SUCCESS)` — constants: `COL_DEFAULT`, `COL_INFO`, `COL_WARNING`, `COL_SUCCESS`, `COL_ERROR` |

---

## Chat / messaging regions

| HTML pattern | Correct Holon component |
|-------------|------------------------|
| Real-time chat widget (single channel) | `LiveChat.builder(userInfo).room("channel-id").withTypingIndicator().build()` — from `com.holonplatform.vaadin.flow.chat.components.LiveChat` |
| Direct message (1-to-1) | `LiveChat.builder(userInfo).room(ChatRoom.direct("alice","bob").getId()).directMessage().build()` |
| Chat with DB persistence | `.withPersistence(chatService)` (service implements `ChatPersistenceService`) |
| Chat with lazy message loading | `.withPageSize(25)` — shows "↑ Load older messages" |
| Multi-channel chat with sidebar | `.withChannels(chatService).withRoomManagement()` |
| Group invitation dialog (user list mode) | `new InviteToGroupDialog(chatService, groupRoom, inviterInfo)` + `.setAvailableUsers(() -> ...)` |
| Group invitation dialog (manual ID mode) | `InviteToGroupDialog.open(chatService, groupRoom, inviterInfo)` |
| Chat room construction | `ChatRoom.group("id", "Name", "desc")` or `ChatRoom.direct("userId1", "userId2")` |

> `LiveChat` requires Vaadin Collaboration Engine on the classpath and `@Push` enabled on the application class.

---

## Kanban board — extended reference

| Pattern | API |
|---------|-----|
| Minimal board | `KanbanBoard.<T,C>builder()` with required: `.withColumns`, `.withItemIdentifierProvider`, `.withItemColumnProvider`, `.withItemColumnUpdater`, `.withCardRenderer`, `.withMoveHandler`, `.withItems` |
| Move pipeline | `beforeMove(req) → boolean` → `onMove(req) → KanbanMoveResult` → `afterMove(req, result)` |
| Lazy slice data provider | `.withDataProvider((query, filter) -> repo.findSlice(query.columnId(), query.offset(), query.limit(), filter))` |
| True column count badge | `.withColumnCountProvider((columnId, filter) -> repo.countByStatus(columnId))` |
| Page size | `.withColumnPageSize(20)` (default 100) |
| Card action buttons | `.withCardActionHandler(new KanbanCardActionHandler<T>() { onOpen; onEdit; onDelete })` |
| Column action buttons | `.withColumnActionHandler(new KanbanColumnActionHandler<C>() { onOptions; onAddCard })` |
| Comment threads | `.withCommentProvider(item -> ...)` + `.withCommentHandler((item, comment) -> ...)`. Programmatic: `board.addComment(item, KanbanComment.of("author", "text"))` |
| Custom button labels | `.withI18n(KanbanI18n.defaults().columnOptions("⚙ Options").addCard("+ New").open("View").edit("Edit").delete("Remove"))` |
| Programmatic move | `board.moveItem(item, toColumn)` → `boolean` |
| Get current column | `board.getColumnOf(item)` → `Optional<C>` |
| Audit trail | `board.getMoveAuditTrail()` / `board.clearMoveAuditTrail()` |
| Filter wiring | `board.refreshOnFilterChange(filterGroup)` or `board.bindFilters(filterGroup, dataProvider)` |

---

## Chart (ChartJs) — type reference

| Chart type | Minimal API |
|------------|-------------|
| Bar | `ChartJs.builder().type(ChartType.BAR).categories("Jan","Feb",...).series("Label", 100, 120,...).height("300px").build()` |
| Line | Same as Bar + `ChartType.LINE`; dataset: `.tension(0.4).fill(false)` |
| Pie | `ChartJs.builder(ChartType.PIE, ChartJsData.builder().labels(...).dataset(d).build()).title("...").build()` |
| Doughnut | Same as Pie with `ChartType.DOUGHNUT` |
| Polar Area | Same as Pie with `ChartType.POLAR_AREA` |
| Radar | Same multi-dataset pattern with `ChartType.RADAR` |
| Bubble | Dataset: `ChartJsDataset.builder().property("data", List.of(Map.of("x",88,"y",320,"r",18),...)).build()` |
| Scatter | Dataset: `Map.of("x", 9.99, "y", 3.2)` — same as Bubble without `"r"` |

---

## Mapping decision rules

1. **Use exact API** — `EntityFormPanel.bean(T.class)` (NOT `EntityPanelForm`, NOT `EntityFormPanel.builder()`);
   `Components.listing(T.class)` (NOT `ListingBundle.builder(PROPERTIES)`);
   `Input.string()` (NOT `Components.input.string()`);
   `KanbanBoard.<T,C>builder()` (NOT `new KanbanBoard<>(...)`).
2. **Feature packages** — all emitted classes go in `com.example.<app>.<feature>`. No `domain/`, `service/`, or `ui/` layer packages.
3. **No Holon equivalent → stop and ask** — do not emit raw Vaadin or unofficial helper classes without explicit developer approval.
4. **Verify with MCP** — use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) or JavaDocs MCP when uncertain.
5. **Unclear mapping** — state the ambiguity and ask the user to confirm before proceeding.
6. **Tab bar** — `TabsBuilder` produces a **bare selector** only. For built-in lazy content management use `LazyTabsBuilder`. Never use raw Vaadin `TabsVariant` — use `.primary()`, `.secondary()` etc.
7. **Kanban** — always use `KanbanBoard.<T,C>builder()`. Card renderer returns any `Component`; use `DivBuilder`/`LabelBuilder`/`CardBuilder` for standard card layouts.
8. **Charts** — always import from `com.holonplatform.vaadin.flow.components.chartjs`. Chart.js loads from CDN lazily — no additional npm dependency needed.
9. **LiveChat** — requires Vaadin Collaboration Engine on the classpath and `@Push` on the application class.
10. **FilterInput vs Input** — use `FilterInput` when a field must produce a `QueryFilter`; use `Input` when a field edits a raw domain value.
