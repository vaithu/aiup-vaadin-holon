# CSS Extraction Reference

Extract visual tokens from an HTML mockup's CSS and map them to **Lumo custom properties**
in a Vaadin theme file. This keeps all styling in `themes/<app-name>/styles.css` and out
of Java code.

---

## Output file location

```
src/main/resources/META-INF/resources/themes/<app-name>/styles.css
```

Place the CSS file in `META-INF/resources/` so Spring Boot / the servlet container serves it
as a static resource under the context root. Reference it from `MainLayout` (or any view) with:

```java
// FALLBACK: no Holon equivalent for @StyleSheet CSS loading
@StyleSheet("context://themes/<app-name>/styles.css")
public class MainLayout extends AppLayout { ... }
```

`context://` resolves to the context root, which maps to `src/main/resources/META-INF/resources/`.

> **Do not use `@Theme` + `src/main/frontend/themes/`.**  
> That approach requires a Vite frontend build and prevents runtime theme switching.
> The `@StyleSheet` + static-resource approach is the Vaadin 25-recommended pattern.

---

## Lumo custom property mapping

Lumo is Vaadin's built-in design system. Override its tokens in `:root` inside `styles.css`.

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
/* src/main/frontend/themes/ap-portal/styles.css */

:root {
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

## Application theme annotation

Apply `@StyleSheet` on `MainLayout` (or any shared layout/view) to load the custom CSS:

```java
// FALLBACK: no Holon equivalent for @StyleSheet CSS loading
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.applayout.AppLayout;

@StyleSheet("context://themes/ap-portal/styles.css")
public class MainLayout extends AppLayout { ... }
```

> Do **not** use `@Theme("ap-portal")` — that requires a Vite frontend build and is superseded
> by the `@StyleSheet` + static-resource pattern in Vaadin 25+.

---

## Rules

- Extract **only** the tokens that differ from Lumo defaults — do not copy the entire Lumo palette.
- Do **not** hardcode hex values in Java code — all visual tokens belong in `styles.css`.
- Place the CSS file in `src/main/resources/META-INF/resources/themes/<app-name>/styles.css` and
  load it via `@StyleSheet("context://themes/<app-name>/styles.css")` on `MainLayout`.
- Do **not** use `@Theme` + `src/main/frontend/themes/` — that approach requires a Vite bundle
  build and is superseded by the `@StyleSheet` static-resource pattern.
- If the mockup uses a CSS preprocessor (SCSS, Less), convert to plain CSS variables in the output.
- If a design token in the mockup has no Lumo equivalent, add it as a custom property prefixed
  with `--app-` (e.g. `--app-sidebar-width: 240px`) and use it in component-level style overrides.
