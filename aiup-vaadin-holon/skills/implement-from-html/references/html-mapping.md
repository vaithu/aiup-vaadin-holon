# HTML Region → Holon Vaadin Component Mapping

Use this table to select the correct Holon Vaadin Flow component for each HTML region
identified in a mockup. Mark a fallback entry as `// FALLBACK: no Holon equivalent for <thing>`
whenever a raw Vaadin component must be used.

---

## Layout / shell regions

| HTML pattern | Holon component | Fallback (raw Vaadin) |
|-------------|-----------------|----------------------|
| `<body>` / full-page wrapper | `VerticalLayout` / `HorizontalLayout` | — |
| Appbar / topbar (`<header>`, `<nav class="topbar">`) | — | `// FALLBACK: no Holon equivalent for AppLayout` → `com.vaadin.flow.component.applayout.AppLayout` |
| Sidenav / left navigation (`<nav class="sidenav">`, `<ul class="nav">`) | — | `// FALLBACK: no Holon equivalent for SideNav` → `com.vaadin.flow.component.sidenav.SideNav` |
| Breadcrumb (`<nav aria-label="breadcrumb">`) | — | `// FALLBACK: no Holon equivalent for breadcrumb` → `com.vaadin.flow.component.html.Span` chain |
| Tab bar (`<ul role="tablist">`, `<div class="tabs">`) | — | `// FALLBACK: no Holon equivalent for Tabs` → `com.vaadin.flow.component.tabs.Tabs` + `Tab` |
| Card / panel (`<div class="card">`, `<section>`) | `VerticalLayout` with card style | — |
| Footer / action bar (`<footer>`, `<div class="action-bar">`) | `HorizontalLayout` | — |

---

## Data display regions

| HTML pattern | Holon component | Fallback (raw Vaadin) |
|-------------|-----------------|----------------------|
| Data table (`<table>`, `<ag-grid>`, `<div role="grid">`) | `ListingBundle.builder(PROPERTIES).dataSource(ds, target).build()` | — |
| Paginated list with rows | `ListingBundle` with lazy Datastore data source | — |
| Read-only detail view (labels + values) | `EntityPanelForm.builder(PROPERTIES).readOnly(true).build()` | — |
| Master-detail split (`<div class="master"> / <div class="detail">`) | `HorizontalLayout` → left: `ListingBundle`, right: `EntityPanelForm` | — |
| Timeline / activity feed | — | `// FALLBACK: no Holon equivalent for activity timeline` → custom `VerticalLayout` with `Span` |

---

## Form / input regions

| HTML pattern | Inferred type | Holon input component |
|-------------|--------------|----------------------|
| `<input type="text">` | `String` | `Components.input.string().label("...").build()` |
| `<input type="number">`, `<input type="currency">` | `BigDecimal` | `Components.input.bigDecimal().label("...").build()` |
| `<input type="date">` | `LocalDate` | `Components.input.localDate().label("...").build()` |
| `<input type="datetime-local">` | `LocalDateTime` | `Components.input.localDateTime().label("...").build()` |
| `<input type="email">` | `String` | `Components.input.string().label("...").build()` (+ email validator) |
| `<input type="checkbox">` | `Boolean` | `Components.input.boolean_().label("...").build()` |
| `<textarea>` | `String` | `Components.input.string().multiLine().label("...").build()` |
| `<select>` (single) | `String` (or enum) | `Components.input.singleSelect(String.class).items(...).build()` |
| `<select multiple>` | `Set<String>` | `Components.input.multiSelect(String.class).items(...).build()` |
| `<input type="file">` | — | `// FALLBACK: no Holon equivalent for Upload` → `com.vaadin.flow.component.upload.Upload` |
| Complete `<form>` with multiple fields | all fields | `EntityPanelForm.builder(PROPERTIES).build()` |

---

## Action / button regions

| HTML pattern | Holon component |
|-------------|-----------------|
| Primary action button (`class="btn-primary"`, `type="submit"`) | `Components.button().text("...").themeVariants(ButtonVariant.LUMO_PRIMARY).onClick(...).build()` |
| Danger / destructive button (`class="btn-danger"`) | `Components.button().text("...").themeVariants(ButtonVariant.LUMO_ERROR).onClick(...).build()` |
| Secondary / cancel button | `Components.button().text("...").onClick(...).build()` |
| Icon-only button (`<button><svg>...</svg></button>`) | `// FALLBACK: no Holon equivalent for icon-only button with aria-label` → raw `Button` with `Icon` |
| Dropdown button / split button | `// FALLBACK: no Holon equivalent for MenuBar` → `com.vaadin.flow.component.menubar.MenuBar` |

---

## Feedback / overlay regions

| HTML pattern | Holon component | Fallback (raw Vaadin) |
|-------------|-----------------|----------------------|
| Success toast / snackbar | — | `// FALLBACK: no Holon equivalent for themed Notification` → `Notification` + `NotificationVariant.LUMO_SUCCESS` |
| Error toast | — | `// FALLBACK: no Holon equivalent for error-themed Notification` → `Notification` + `NotificationVariant.LUMO_ERROR` |
| Confirmation dialog (`<dialog>`, `<div role="dialog">`) | — | `// FALLBACK: no Holon equivalent for ConfirmDialog` → `com.vaadin.flow.component.confirmdialog.ConfirmDialog` |
| Loading spinner / skeleton | — | `// FALLBACK: no Holon equivalent for ProgressBar` → `com.vaadin.flow.component.progressbar.ProgressBar` |

---

## Mapping decision rules

1. **Prefer Holon** — always check whether Holon Vaadin Flow covers the region first.
2. **Fallback with comment** — if raw Vaadin is needed, add `// FALLBACK: no Holon equivalent for <thing>` before the import or instantiation.
3. **Verify with MCP** — if uncertain, query the Vaadin MCP server (`https://mcp.vaadin.com/docs`) or JavaDocs MCP to confirm availability in Holon 10.0.x / Vaadin 25.
4. **Unclear mapping** — state the ambiguity and ask the user to confirm before proceeding.
