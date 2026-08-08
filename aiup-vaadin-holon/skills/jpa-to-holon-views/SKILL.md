---
name: jpa-to-holon-views
description: Create Vaadin views for a JPA entity using the standard Two-View Pattern (ListingBundle list view + MasterDetailLayout detail view, with responsive desktop/mobile behaviour). Use when asked to build or add views for an entity.
argument-hint: "[EntityName or package path]"
---

# Use Case Implementation (Holon Platform)

Implement the use case specified by $ARGUMENTS.

A use case is the unit of work, not a view. A single use case may produce one view, several views, or no UI at all — let the spec drive that, and keep tests grouped per use case as defined by `/use-case-tests`.

---

## ⚠️ Technology Constraints

### ✅ Required — use these exclusively

| Concern | API to use |
|---|---|
| Data model / value carriers | **JPA entity beans** with **`BeanPropertySet`** introspection |
| Data access (queries, saves, deletes) | **Holon Platform `Datastore`** (`query()`, `save()`, `delete()`, `bulkInsert()`, etc.) |
| Authentication & authorisation | **Holon Platform Security** (`AuthContext`, `Realm`, `@Authenticate`, permission checks) |
| Forms | **`BeanPropertyInputForm`** (via `Components.input.form(BeanPropertySet)` or `EntityFormPanel.bean(BeanClass)`) |
| Grids / listings | **`ListingBundle`** (via `Components.listing(BeanClass)`) with `LitRenderer` for card-style rows |
| Master-detail layout | **`MasterDetailLayout`** (via `Components.masterDetail(BeanClass)`) |
| Filter panels | **`DynamicFilterPanel`** (via `ListingBundleBuilder.withFilterPanel()`) |
| Mobile slide-in detail | **`Sheet`** (via `MasterDetailConfigurator.withMobileSheet()`) |

### 🚫 Forbidden — never use these

- **Raw Vaadin components** (`TextField`, `Grid`, `FormLayout`, `Binder`, `Button` instantiated directly, etc.)
  — always go through the Holon Platform Vaadin UI builder APIs instead.
- JPA `EntityManager` / Spring Data `Repository` for queries — use `Datastore` only.
- Spring Security annotations/classes for auth logic — use Holon Security only.
- Plain `PropertyBox` as the primary data carrier when a typed bean already exists — use the bean + `BeanPropertySet`.

> If you are unsure whether a class belongs to raw Vaadin or to Holon Platform Vaadin UI, check the package:
> `com.holonplatform.vaadin.*` ✅ &nbsp;&nbsp; `com.vaadin.*` directly ⛔

---

## Input

1. The use case document itself (in `spec/use-cases/`)
2. Related use cases
3. Generic specification files in `spec/`
4. `docs/entity_model.md` — **always read this before writing any data-access code**
5. Any potential images or other resources provided for the use case

---

## Standard Two-View Pattern

Every entity that needs a UI **must** be implemented as exactly two views. Let the spec drive the routes, but the structure below is mandatory.

### View 1 — List View (`<Entity>ListView`)

Responsible for **browsing** the entity collection. Adapts its presentation to the viewport:

```
Desktop  →  ListingBundle with DynamicFilterPanel
              │  row click  →  navigate to View 2 (detail route)
Mobile   →  same ListingBundle but master column rendered as LitRenderer card
              │  row click  →  Sheet slide-in with full entity details
```

**Implementation skeleton:**

```java
@Route(value = "entities", layout = MainLayout.class)
public class EntityListView extends Div {

    @QueryParameter("id")        // for deep-link restore
    private String urlId;

    public EntityListView(EntityService service) {
        ResponsiveDiv.configure(this)
                .slotOnce(ViewMode.DESKTOP, () -> buildDesktopView(service))
                .slotOnce(ViewMode.MOBILE,  () -> buildMobileView(service))
                .fullHeight()
                .build();
    }

    private Component buildDesktopView(EntityService service) {
        // ListingBundle with filter panel; row click navigates to detail
        ListingBundle<Entity> bundle = Components.listing(Entity.class)
                .columns("col1", "col2", "col3")
                .search("Search…")
                .withFilterPanel()
                .fetch((q, text, filter, sort) -> service.fetch(q, text, filter, sort))
                .build();

        bundle.addItemClickListener(e ->
                Navigator.get().navigateTo(EntityDetailView.class,
                        Map.of("id", String.valueOf(e.getItem().getId()))));

        return bundle;
    }

    private Component buildMobileView(EntityService service) {
        Sheet sheet = Sheet.builder(Sheet.Side.RIGHT).build();

        MasterDetailLayout<Entity> layout = Components.masterDetail(Entity.class)
                .viewMode(ViewMode.MOBILE)
                .withMobileSheet(sheet)
                .master(m -> m
                        .listing(l -> l
                                .autoCreateColumns(false)
                                .mobileViewHeader(mobileCardHeader())
                                .mobileViewColumn(mobileCardColumn())
                                .search("Search…")
                                .fetch((q, text, filter, sort) ->
                                        service.fetch(q, text, filter, sort)))
                        .selectionKey(Entity::getId))
                .withDetailSync(entity -> populateSheet(sheet, entity))
                .build();

        return Components.vl().add(layout, sheet).build();
    }

    private LitRenderer<Entity> mobileCardColumn() {
        return Components.<Entity>mobileGridColumnLit()
                .withAvatarAsPrimary(Entity::getName)
                .withSecondaryText(Entity::getName)
                .withTertiaryText(e -> e.getSomeField().toString())
                .build();
    }

    private void populateSheet(Sheet sheet, Entity entity) {
        // Populate sheet content using BeanPropertyInputForm or key-value components
        BeanPropertyInputForm<Entity> form =
                Components.input.form(BeanPropertySet.create(Entity.class))
                        .readOnly()
                        .build();
        form.setBean(entity);
        sheet.setContent(form.getComponent());
        sheet.open();
    }
}
```

---

### View 2 — Detail View (`<Entity>DetailView`)

Responsible for **master-detail inspection and editing**. The master is a compact listing; the selected row shows the full detail panel.

```
Desktop  →  MasterDetailLayout
              master  : ListingBundle (minimal columns, LitRenderer card rows)
              detail  : EntityFormPanel or custom detail panel
              first row pre-selected on load; URL sync via ?id=

Mobile   →  MasterDetailLayout  (ViewMode.MOBILE)
              master  : same ListingBundle
              row tap : Sheet slide-in with full details (Sheet.Side.RIGHT)
```

**Implementation skeleton:**

```java
@Route(value = "entities/:id", layout = MainLayout.class)
public class EntityDetailView extends Div {

    @QueryParameter("id")
    private String urlId;

    private MasterDetailLayout<Entity> desktopLayout;

    // Live detail refs (assigned in lazyDetail's setup consumer)
    private Span            headingSpan;
    private EntityFormPanel<Entity> detailForm;

    public EntityDetailView(EntityService service) {
        ResponsiveDiv.configure(this)
                .slotOnce(ViewMode.DESKTOP, () -> buildLayout(ViewMode.DESKTOP, service))
                .slotOnce(ViewMode.MOBILE,  () -> buildLayout(ViewMode.MOBILE,  service))
                .fullHeight()
                .build();
    }

    private MasterDetailLayout<Entity> buildLayout(ViewMode viewMode, EntityService service) {
        MasterDetailLayout<Entity> layout = Components.masterDetail(Entity.class)
                .viewMode(viewMode)
                .withMobileSheet(Sheet.Side.RIGHT)
                .withUrlSync(
                        entity -> String.valueOf(entity.getId()),
                        id -> service.findById(Long.parseLong(id)))
                .master(m -> m
                        .header(h -> h.heading("All Entities").actions(newButton()))
                        .listing(l -> l
                                .autoCreateColumns(false)
                                .mobileViewHeader(masterCardHeader())
                                .mobileViewColumn(masterCardColumn())
                                .search("Search…")
                                .fetch((q, text, filter, sort) ->
                                        service.fetch(q, text, filter, sort)))
                        .selectionKey(Entity::getId))
                .lazyDetail(d -> d
                        .header(h -> h
                                .heading(headingSpan = new Span("Select an entity")))
                        .content(detailForm = buildDetailForm())
                        .withDetailSync(this::syncDetail))
                .build();

        if (!viewMode.isMobile()) {
            desktopLayout = layout;
            layout.addAttachListener(e -> initDesktopSelection());
        }
        return layout;
    }

    @OnShow
    private void onShow() {
        if (desktopLayout != null) initDesktopSelection();
    }

    private void initDesktopSelection() {
        if (urlId != null && !urlId.isBlank()) {
            desktopLayout.restoreFromUrl(urlId);
        } else {
            desktopLayout.selectFirst(ViewMode.DESKTOP);
        }
    }

    private EntityFormPanel<Entity> buildDetailForm() {
        return EntityFormPanel.bean(Entity.class)
                .withSaveButton(entity -> service.save(entity))
                .build();
    }

    private void syncDetail(Entity entity) {
        headingSpan.setText(entity.getName());
        detailForm.setBean(entity);
        if (desktopLayout != null)
            desktopLayout.pushUrlState(getElement(), entity, ViewMode.DESKTOP);
    }

    private LitRenderer<Entity> masterCardColumn() {
        return Components.<Entity>mobileGridColumnLit()
                .withAvatarAsPrimary(Entity::getName)
                .withSecondaryText(Entity::getName)
                .withTertiaryText(e -> e.getSomeField().toString())
                .build();
    }
}
```

---

## Data Model Pattern

Each entity must have a companion `*Model` interface in a `model` sub-package:

```java
public interface EntityModel {

    // BeanPropertySet — introspected from the JPA entity bean
    BeanPropertySet<Entity> PROPERTY_SET = BeanPropertySet.create(Entity.class);

    // Datastore target
    DataTarget<String> TARGET = DataTarget.named("entity_table");

    // Typed PathProperty constants — one per mapped field
    PathProperty<Long>   ID   = PROPERTY_SET.property("id",   Long.class);
    PathProperty<String> NAME = PROPERTY_SET.property("name", String.class);
    // … more fields …

    // Sub-sets for listing columns and form fields
    PropertySet<?> LISTING = PropertySet.builderOf(ID, NAME /*, … */).build();
    PropertySet<?> FORM    = PropertySet.builderOf(NAME /*, … */).build();
}
```

Use `BeanPropertySet` instead of hand-crafting a `PropertySet`. The bean's JPA/Bean Validation annotations (`@NotBlank`, `@Column`, `@Caption`, `@Identifier`, `@Sequence`) are picked up automatically.

---

## Implementation Steps

The following steps are mandatory and sequential. **Do not skip or reorder them.**

### Step 1: Read context
- Read `docs/entity_model.md` to understand the entity model before writing any code.
- Read the use case spec and any related specs in `spec/use-cases/`.
- Identify which `BeanPropertySet` / `*Model` definitions already exist and where they live.

### Step 2: Write the code
- Follow the **Standard Two-View Pattern** above for every entity that needs a UI.
- Use **`BeanPropertySet.create(Entity.class)`** for the property model; never hand-roll a `PropertySet` when a bean exists.
- Use **`ListingBundle`** (via `Components.listing(BeanClass)`) for all grids.
- Apply **`LitRenderer`** card columns (`Components.mobileGridColumnLit()`) for the mobile/master listing rows.
- Use **`DynamicFilterPanel`** (via `.withFilterPanel()` on the bundle builder) on the desktop list view.
- Use **`MasterDetailLayout`** (via `Components.masterDetail(BeanClass)`) for the detail view.
- On mobile, use **`Sheet`** (`Sheet.Side.RIGHT` or `Sheet.Side.BOTTOM`) for the slide-in detail; wire it via `withMobileSheet()` or `withDetailSync()`.
- Use **`EntityFormPanel.bean(BeanClass)`** for create/edit forms.
- Persist and query data exclusively via **`Datastore`** operations.
- Protect routes/actions with **Holon Platform Security** (`@Authenticate`, `AuthContext.require*()`, etc.).
- Whenever you are even slightly unsure about Holon Platform API usage, component behaviour, theme variables, or best practices — look it up in the project sources or official Holon documentation before guessing. Do **not** rely on memory for API specifics.

### Step 3: Write and run automated tests
- Verify everything that does **not** depend on visual appearance here first — main flow, alternative flows, business rules, and services. Use the fast browserless / `@SpringBootTest` mechanisms; they are far cheaper and faster than driving a browser, so cover as much behaviour as possible this way before touching Playwright.
- Follow the use-case-tests workflow and ensure all tests pass before moving on.

### Step 4: Visually verify with Playwright MCP
- **This step is mandatory.** Do not skip it, do not defer it.
- Behaviour is already covered by the automated tests, so use Playwright MCP **only** to verify what those tests can't: layout, spacing, rendering, and visual appearance.
- Follow the visual-verification workflow end-to-end: start the app, navigate every route (desktop and mobile viewport), take screenshots, and validate the visual appearance. Don't re-check functional behaviour already covered by Step 3.
- Fix any issues found before moving on.

### Step 5: Iterate
- Keep iterating until everything looks and works great. Prefer great results over finishing quickly.
- On every iteration, re-check the Technology Constraints table above — a review finding a raw Vaadin component is a blocker.

### Step 6: Commit
- Once all steps are complete and everything works, create a git commit with the changes.

**All steps must be completed before a use case is considered implemented.**

