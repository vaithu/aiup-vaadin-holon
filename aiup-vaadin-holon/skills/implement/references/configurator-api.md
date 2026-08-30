# Holon Vaadin Flow — Fluent Configurator API (what already exists)

> **Source of truth**: `com.holonplatform.vaadin.flow.components.builders.*` in the
> Holon Vaadin Flow library. Signatures below are transcribed verbatim from the library
> source (commit `1f8e3812` of `holon-vaadin-flow`). Do **not** invent methods — every
> builder returned by `Components.*`, `Input.*`, `ButtonBuilder.create()`, etc. already
> inherits the fluent methods catalogued here.

Holon builders are assembled from small **configurator mix-in interfaces**. A concrete
builder (e.g. `StringInputBuilder`, `ButtonBuilder`, `ItemListingBuilder`) `extends` the
configurators appropriate for that component, so the same fluent method (`width(...)`,
`styleName(...)`, `visible(...)`, `withClickListener(...)`) is available on every builder
that supports the underlying capability.

**Do not reach for raw Vaadin `getElement().setAttribute(...)`, `setWidth(...)`,
`addClassName(...)`, `setVisible(...)` etc.** — use the configurator method so the call
stays inside the fluent chain and picks up `Localizable` / `Signal` support where offered.
If a capability you need is genuinely absent from every configurator below, **stop and ask
the developer** before emitting raw Vaadin.

---

## Inheritance root

```
ComponentConfigurator<C>
 ├── HasElementConfigurator<C>      (theme names + raw DOM event listeners)
 └── ConditionalConfigurable<C>     (applyIf / applyUnless / also)
```

Every component builder inherits `ComponentConfigurator`. Input builders additionally go
through `InputConfigurator` (which itself extends `ComponentConfigurator`). All other
`Has*Configurator` interfaces are independent mix-ins composed per builder.

> **`bindXxx(Signal<...>)`** reactive-binding overloads were added in `5.5.8` and exist on
> `ComponentConfigurator`, `HasEnabledConfigurator`, `HasTextConfigurator`,
> `HasHtmlTextConfigurator`, `HasLabelConfigurator`, `HasTitleConfigurator`,
> `HasPlaceholderConfigurator`, `HasHelperTextConfigurator`, `HasTooltipConfigurator`,
> and `InputConfigurator`.

---

## 1 · `ComponentConfigurator<C>` — inherited by every builder

| Method | Purpose |
|--------|---------|
| `id(String)` | Set the root element id (unique on the page); `null` removes it. |
| `visible(boolean)` | Show/hide; invisible components block server→client updates. |
| `hidden()` | Shortcut for `visible(false)`. |
| `bindVisible(Signal<Boolean>)` | Reactively bind visibility to a `Signal`. |
| `elementConfiguration(Consumer<Element>)` | Escape hatch to configure the raw DOM `Element`. |
| `withAttachListener(ComponentEventListener<AttachEvent>)` | Run code when the component attaches. |
| `withDetachListener(ComponentEventListener<DetachEvent>)` | Run code when the component detaches. |

### `HasElementConfigurator<C>` (parent)

| Method | Purpose |
|--------|---------|
| `withThemeName(String)` | Add a theme name to the element. |
| `withEventListener(String eventType, DomEventListener)` | Add a raw DOM event listener. |
| `withEventListener(String eventType, DomEventListener, String filter)` | DOM listener with a JS filter expression. |

### `ConditionalConfigurable<C>` (parent) — conditional chaining

| Method | Purpose |
|--------|---------|
| `applyIf(boolean, UnaryOperator<C>)` | Apply the configurator only when the condition is `true`. |
| `applyUnless(boolean, UnaryOperator<C>)` | Apply only when the condition is `false`. |
| `applyIf(Supplier<Boolean>, UnaryOperator<C>)` | Lazily evaluate the condition before applying. |
| `also(Consumer<C>)` | Always run a side-effect without breaking the chain (tap). |

```java
Input<String> code = Input.string()
    .label(Localizable.of("Code", "part.code"))
    .applyIf(readOnlyMode, b -> b.readOnly())
    .also(b -> b.styleName("mono"))
    .build();
```

---

## 2 · Sizing — `HasSizeConfigurator<C>`

Constant `FULL_SIZE = "100%"`.

| Method | Purpose |
|--------|---------|
| `width(String)` / `height(String)` | CSS width/height (`"320px"`, `"20em"`); `null` clears. |
| `width(float, Unit)` / `height(float, Unit)` | Numeric value + `Unit`; negative = unspecified. |
| `fullWidth()` / `fullHeight()` / `fullSize()` | Shorthand for `100%`. |
| `widthUndefined()` / `heightUndefined()` / `sizeUndefined()` | Clear the size setting. |
| `minWidth` / `maxWidth` / `minHeight` / `maxHeight (String)` | CSS min/max constraints *(since 5.2.3)*. |

---

## 3 · CSS classes — `HasStyleConfigurator<C>`

| Method | Purpose |
|--------|---------|
| `styleName(String)` | Add a single CSS class (space-separated list accepted). |
| `styleNames(String...)` | Add several CSS classes. |

> Prefer Lumo utility classes / CSS in `styles.css` over inline styles. Use `styleName(...)`,
> not raw `addClassName(...)`.

---

## 4 · Enabled state — `HasEnabledConfigurator<C>`

| Method | Purpose |
|--------|---------|
| `enabled(boolean)` | Enable / disable. |
| `disabled()` | Shortcut for `enabled(false)`. |
| `bindEnabled(Signal<Boolean>)` | Reactively bind enabled state *(since 5.5.8)*. |

---

## 5 · Textual content mix-ins (all `Localizable`-aware)

Every text-like method has three overloads: `(Localizable)`, `(String)`, and
`(String defaultText, String messageCode, Object... arguments)`. Reactive `bindXxx(Signal)`
variants exist where noted. **Prefer the `Localizable` overload** so text stays translatable.

| Interface | Key methods |
|-----------|-------------|
| `HasTextConfigurator` | `text(...)`, `bindText(Signal)` — plain-text content. |
| `HasHtmlTextConfigurator` | `htmlText(...)`, `bindHtmlText(Signal)` — HTML-capable content. |
| `HasLabelConfigurator` | `label(...)`, `bindLabel(Signal)` — form field label. |
| `HasTitleConfigurator` | `title(...)`, `bindTitle(Signal)`, plus `description(...)` aliases — HTML `title`/hover tooltip. |
| `HasPlaceholderConfigurator` | `placeholder(...)`, `bindPlaceholder(Signal)` — input placeholder hint. |
| `HasHelperTextConfigurator` | `helperText(...)`, `bindHelperText(Signal)`, `helperComponent(Component)` *(since 5.5.8)*. |
| `HasTooltipConfigurator` | `tooltip(...)`, `bindTooltip(Signal)`, `tooltipText(String)` — `<vaadin-tooltip>` *(since 5.5.4)*. |

---

## 6 · Accessibility — `HasAriaLabelConfigurator<C>` *(since 5.5.4)*

| Method | Purpose |
|--------|---------|
| `ariaLabel(Localizable)` / `ariaLabel(String)` / `ariaLabel(String default, String code, Object...)` | Set `aria-label`. |
| `ariaLabelledBy(String)` | Set `aria-labelledby` (references another element's id). |

> Icon-only interactive components **must** call `ariaLabel(...)` — see the A11Y section of
> [`holon-vaadin-ui.md`](holon-vaadin-ui.md).

---

## 7 · Icons — `HasIconConfigurator<C>`

| Method | Purpose |
|--------|---------|
| `icon(VaadinIcon)` / `icon(Component)` / `icon(String collection, String icon)` | Set the icon. |
| `iconConfigurator(VaadinIcon \| Icon \| collection,name)` | Nested `IconConfigurator` for fine control. |

Nested `IconConfigurator<C>` (extends `HasStyleConfigurator`): `size(String)`, `color(String)`,
and `add()` to finalise and return to the parent builder.

---

## 8 · Prefix / suffix slots — `HasPrefixAndSuffixConfigurator<C>`

| Method | Purpose |
|--------|---------|
| `prefixComponent(Component)` | Slot a component before the field content. |
| `suffixComponent(Component)` | Slot a component after the field content. |

---

## 9 · Theme variants — `HasThemeVariantConfigurator<V, C>`

| Method | Purpose |
|--------|---------|
| `withThemeVariants(V... variants)` | Add typed (enum) theme variants for the component. |

---

## 10 · Focus & keyboard — `FocusableConfigurator<T, C>`

| Method | Purpose |
|--------|---------|
| `tabIndex(int)` | Sequential keyboard navigation order. |
| `withFocusListener(...)` / `withBlurListener(...)` | Focus / blur listeners. |
| `withFocusShortcutKey(Key, KeyModifier...)` | Keyboard shortcut that focuses the component *(since 5.2.3)*. |
| `withFocusShortcut(Key)` → `ShortcutConfigurator<C>` | Fluent shortcut configuration *(since 5.2.3)*. |

---

## 11 · Clicks — `ClickNotifierConfigurator<S, E, C>`

| Method | Purpose |
|--------|---------|
| `withClickListener(ClickEventListener<S,E>)` | Register a click listener. |
| `onClick(ClickEventListener<S,E>)` | Alias for `withClickListener(...)`. |
| `withClickShortcutKey(Key, KeyModifier...)` | Keyboard shortcut that triggers a click *(since 5.2.3)*. |
| `withClickShortcut(Key)` → `ShortcutConfigurator<C>` | Fluent click-shortcut configuration *(since 5.2.3)*. |

---

## 12 · Keyboard events — `KeyNotifierConfigurator<C>`

`withKeyDownListener`, `withKeyPressListener`, `withKeyUpListener` — each with an unfiltered
overload and a `(Key key, listener, KeyModifier... modifiers)` overload that fires only for
the given key + modifiers.

---

## 13 · Input-specific mix-ins

### `InputConfigurator<T, E, C>` (extends `ComponentConfigurator`)

| Method | Purpose |
|--------|---------|
| `readOnly(boolean)` / `readOnly()` | Make the input read-only. |
| `bindReadOnly(Signal<Boolean>)` | Reactively bind read-only state *(since 5.5.8)*. |
| `withReadonlyChangeListener(...)` | Notified when read-only changes. |
| `withValueChangeListener(ValueChangeListener<T,E>)` | Notified when the value changes. |
| `required(boolean)` / `required()` | Mark the input required. |
| `bindRequired(Signal<Boolean>)` | Reactively bind required state *(since 5.5.8)*. |
| `withAdapter(Class<A>, Function<Input<T>, A>)` | Register an adapter usable via `Input.as(Class)`. |

### Other input mix-ins

| Interface | Key methods |
|-----------|-------------|
| `HasValueChangeModeConfigurator` | `valueChangeMode(ValueChangeMode)` — EAGER / LAZY / TIMEOUT sync timing. |
| `HasAutocompleteConfigurator` | `autocomplete(Autocomplete)` — browser autocomplete attribute. |
| `HasAutofocusConfigurator` | `autofocus(boolean)` — focus on page load. |
| `HasClearButtonConfigurator` | `clearButtonVisible(boolean)`, `isClearButtonVisible()` *(since 5.5.4)*. |
| `HasPatternConfigurator` | `pattern(String)` (regex validation), `allowedCharPattern(String)` (restrict typed chars). |

---

## 14 · Grouped forms — `ComponentGroupConfigurator<P, T, E, G, C>`

| Method | Purpose |
|--------|---------|
| `usePropertyRendererRegistry(PropertyRendererRegistry)` | Override the renderer registry used by the group. |
| `withValueChangeListener(...)` | Group-level value-change listener. |

---

## 15 · Deferred localization — `DeferrableLocalizationConfigurator<C>`

| Method | Purpose |
|--------|---------|
| `withDeferredLocalization(boolean)` | Resolve i18n messages on UI attach instead of at build time. |
| `deferLocalization()` | Shortcut for `withDeferredLocalization(true)`. |

---

## Rules of thumb

- The fluent configurator method **already exists** for id, visibility, size, style, enabled
  state, labels, placeholders, helper text, tooltips, ARIA, icons, prefix/suffix, theme
  variants, focus, clicks, keyboard, read-only, required, value-change mode, autocomplete,
  autofocus, clear button, and regex pattern. Use it — do not hand-roll raw Vaadin calls.
- Every user-visible string goes through a `Localizable` overload (`Localizable.of("fallback",
  "message.key")`), never a bare `String`, per the I18N rules in
  [`component-dictionary.md`](component-dictionary.md).
- Reach for `bindXxx(Signal<...>)` when the value is reactive rather than manually wiring
  listeners.
- If none of the configurators above cover the capability you need, **stop and ask the
  developer** — do not silently emit raw `com.vaadin.flow.component.*` calls.
