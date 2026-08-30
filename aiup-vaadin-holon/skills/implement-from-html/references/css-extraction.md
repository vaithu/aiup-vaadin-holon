# CSS Extraction Reference

Extract visual tokens from an HTML mockup's CSS and map them to **Lumo custom properties**
in a plain CSS file. This keeps all styling in `src/main/resources/META-INF/resources/styles.css`
and out of Java code.

---

## Output file location

```
src/main/resources/META-INF/resources/styles.css
```

This file is loaded as a document-level stylesheet by the `@StyleSheet` annotation on the
`AppShellConfigurator` class — declared **after** the theme stylesheet so overrides apply on top.

> **Responsive design policy — `ResponsiveDiv` for simple cases, CSS for complex cases.** For
> **simpler** responsive behaviour (mobile/desktop viewport slot swaps, column counts,
> hiding/showing regions) prefer the Lumo / Holon **component** responsive APIs
> (`ResponsiveDiv` slots, `FormLayout.responsiveSteps(...)`, `MasterDetailLayout` + `Sheet`,
> `mobileViewColumn(...)`). For **complex** responsive behaviour (fine-grained breakpoints,
> spacing, font sizes, sticky bars, card vs. table presentation, per-breakpoint Grid renderers,
> etc.) use **pure CSS `@media` queries and styles in `styles.css`**, targeting a CSS class you
> add to the component.

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.StyleSheet;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@StyleSheet(Lumo.STYLESHEET)   // load the Lumo theme first
@StyleSheet("styles.css")      // then apply your custom overrides
@SpringBootApplication
public class Application implements AppShellConfigurator { ... }
```

> **Do not use the deprecated `@Theme("<app-name>")` annotation** — it was removed in Vaadin 25.2.
> The `src/main/frontend/themes/<app-name>/` directory approach is no longer supported.

---

## Lumo custom property mapping

Lumo is Vaadin's built-in design system. Override its tokens in `html` inside `styles.css`.

### Colors

| Source CSS variable / value | Lumo custom property |
|-----------------------------|----------------------|
| Primary brand color (e.g. `--color-primary: #1565C0`) | `--lumo-primary-color` |
| Primary text on brand | `--lumo-primary-contrast-color` |
| Primary color 10% opacity | `--lumo-primary-color-10pct` |
| Primary color 50% opacity | `--lumo-primary-color-50pct` |
| Success / positive color | `--lumo-success-color` |
| Error / danger color | `--lumo-error-color` |
| Warning color | `--lumo-warning-color` |
| Body text color | `--lumo-body-text-color` |
| Secondary text | `--lumo-secondary-text-color` |
| Disabled text | `--lumo-disabled-text-color` |
| Background base | `--lumo-base-color` |
| Contrast 5% (subtle border) | `--lumo-contrast-5pct` |
| Contrast 20% (border) | `--lumo-contrast-20pct` |

### Typography

| Source CSS | Lumo custom property |
|-----------|----------------------|
| `font-family` (body) | `--lumo-font-family` |
| Base font size | `--lumo-font-size-m` |
| Small text | `--lumo-font-size-s` |
| Large heading | `--lumo-font-size-xxl` |
| Line height | `--lumo-line-height-m` |

### Spacing / sizing

| Source CSS | Lumo custom property |
|-----------|----------------------|
| Base spacing unit | `--lumo-space-m` |
| Small spacing | `--lumo-space-s` |
| Large spacing | `--lumo-space-l` |
| Border radius | `--lumo-border-radius-m` |
| Component height (inputs, buttons) | `--lumo-size-m` |

---

## Example `styles.css`

```css
/* src/main/resources/META-INF/resources/styles.css */

html {
  /* Brand colors from mockup */
  --lumo-primary-color: #1565C0;
  --lumo-primary-contrast-color: #FFFFFF;
  --lumo-primary-color-10pct: rgba(21, 101, 192, 0.1);
  --lumo-primary-color-50pct: rgba(21, 101, 192, 0.5);

  /* Semantic colors */
  --lumo-success-color: #2E7D32;
  --lumo-error-color: #C62828;

  /* Typography */
  --lumo-font-family: 'Inter', 'Roboto', sans-serif;
  --lumo-font-size-m: 0.875rem;

  /* Spacing */
  --lumo-space-m: 1rem;
  --lumo-border-radius-m: 4px;
}
```

---

## Application stylesheet annotation

Apply `@StyleSheet` on the `AppShellConfigurator` implementation (typically the `@SpringBootApplication` class).
Always load the base theme **before** any custom overrides:

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.page.StyleSheet;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@StyleSheet(Lumo.STYLESHEET)   // Lumo base theme — always first
@StyleSheet("styles.css")      // custom Lumo token overrides
@SpringBootApplication
public class Application implements AppShellConfigurator {
    public static void main(String[] args) { ... }
}
```

> Do **not** put `@StyleSheet` on `MainLayout`, `AppLayout`, or any `@Route` view.
> Do **not** use the deprecated `@Theme("<app-name>")` annotation.

---

## Responsive breakpoints with `@media` (preferred)

Express responsive behaviour with plain CSS media queries in `styles.css` first. Target the
component's host element or a semantic CSS class you add via `component.addClassName("...")`
(or `Components...styleName("...")`); keep the breakpoint logic in CSS, not in Java.

```css
/* src/main/resources/META-INF/resources/styles.css */

/* Custom breakpoint tokens (optional, for reuse) */
html {
  --app-breakpoint-s: 40em;
  --app-breakpoint-m: 60em;
}

/* Mobile-first defaults */
.customer-form {
  display: grid;
  grid-template-columns: 1fr;          /* single column on small screens */
  gap: var(--lumo-space-m);
}

/* Tablet and up */
@media (min-width: 40em) {
  .customer-form { grid-template-columns: 1fr 1fr; }
}

/* Desktop and up */
@media (min-width: 60em) {
  .customer-form { grid-template-columns: 1fr 1fr 1fr; }
  .app-sidebar   { width: var(--app-sidebar-width, 240px); }
}

/* Hide a region below a breakpoint instead of building a second view */
@media (max-width: 40em) {
  .desktop-only { display: none; }
}
```

Use `min-width` (mobile-first) breakpoints and, where possible, the same `em` thresholds the
mockup uses so the generated CSS mirrors the source design.

---

## Rules

- **Prefer the component responsive APIs (`ResponsiveDiv`, `responsiveSteps`,
  `MasterDetailLayout`, `mobileViewColumn`) for simpler responsive cases**; use pure CSS
  `@media` queries and styles for complex responsive behaviour.
- Extract **only** the tokens that differ from Lumo defaults — do not copy the entire Lumo palette.
- Do **not** hardcode hex values in Java code — all visual tokens belong in `styles.css`.
- Place the CSS file in `src/main/resources/META-INF/resources/styles.css` and load it via `@StyleSheet("styles.css")`
  on the `AppShellConfigurator` class, **after** `@StyleSheet(Lumo.STYLESHEET)`.
- Do **not** use the deprecated `@Theme("<app-name>")` annotation or the `themes/<app-name>/` directory structure.
- If the mockup uses a CSS preprocessor (SCSS, Less), convert to plain CSS variables in the output.
- If a design token in the mockup has no Lumo equivalent, add it as a custom property prefixed
  with `--app-` (e.g. `--app-sidebar-width: 240px`) and use it in component-level style overrides.

