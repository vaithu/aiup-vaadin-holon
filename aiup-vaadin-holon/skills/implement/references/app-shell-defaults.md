# App shell defaults: search, notifications, user menu, language switcher, i18n bundles

Every application shell built with this skill set **must** expose four default elements in the
AppBar, and support at least two locales with a working switcher. This is a **mandatory baseline**
for every `AppShellLayout` / `MainLayout`, not an optional nice-to-have.

## Mandatory AppBar elements

| Element | Purpose | Builder entry point |
|---|---|---|
| Global search field | Top-bar search box in the AppBar middle slot | `AppShellLayoutBuilder.search(placeholder, valueListener)` |
| Notifications bell | Badge + dropdown list of recent notifications | `AppShellLayoutBuilder.notifications(badgeCount, items...)` |
| User menu | Avatar showing the signed-in user, with a working **Sign out** action | see "Known library gap" below — build manually |
| Language switcher | Globe icon menu to switch the active locale (at minimum English + one additional locale) | see "Known library gap" below — build manually |

## Known library gap — `.languages(...)` and `.user(...).menu(...)` are inert

`AppShellLayoutConfigurator.languages(String...)` and `.user(Consumer<UserConfig>)` (with
`UserConfig.menu(...).item(String)`) render a globe button / avatar and a `ContextMenu` populated
with the given labels, but **do not attach any click listener** to those menu items in this library
version (`AbstractAppShellLayoutConfigurator` calls `menu::addItem` with the single-`String`-arg
overload only). Do **not** rely on them to perform an action — they are display-only.

**Always build the language switcher and the user/sign-out menu manually** using the
`customizeEnd(Consumer<AppBar>)` escape hatch together with `ButtonBuilder`, `AvatarBuilder` and
`ContextMenuBuilder` (all Holon component builders — no raw Vaadin `ContextMenu` construction
needed):

```java
Components.appShell()
    .navbarBrand("Acme CRM", CustomerListView.class)
    .search(translate("Search...", "shell.search.placeholder"), this::onSearch)
    .notifications(2, translate("New contact added", "shell.notification.newContact"),
                       translate("Invoice overdue", "shell.notification.overdueInvoice"))
    .nav(nav)
    // .languages(...) / .user(...) only render — no click wiring, see gap above
    .customizeEnd(this::addLanguageAndUserMenu)
    .configure(this);

private void addLanguageAndUserMenu(AppBar appBar) {
    Button languageButton = ButtonBuilder.create()
            .icon(VaadinIcon.GLOBE).icon().tertiary()
            .ariaLabel(translate("Language", "shell.language.ariaLabel"))
            .build();
    ContextMenuBuilder.create()
            .openOnClick(true)
            .withItem(Localizable.of("English", "shell.language.english"), e -> switchLocale(Locale.ENGLISH))
            .withItem(Localizable.of("Tamil", "shell.language.tamil"), e -> switchLocale(new Locale("ta")))
            .build(languageButton);
    appBar.addToEnd(languageButton);

    Avatar userAvatar = AvatarBuilder.create(currentUserName()).build();
    ContextMenuBuilder.create()
            .openOnClick(true)
            .withItem(Localizable.of("Sign out", "shell.user.signOut"),
                    e -> UI.getCurrent().getPage().setLocation("/logout"))
            .build(userAvatar);
    appBar.addToEnd(userAvatar);
}

private static void switchLocale(Locale locale) {
    LocalizationContext.require().localize(locale);
    UI.getCurrent().getPage().reload();
}

// The ONLY i18n resolution path — never getTranslation(...). Resolves against the same
// session-bound LocalizationContext that switchLocale(...) mutates, so every label — including
// AppBar chrome built outside a Localizable-aware builder overload — updates on locale switch.
private static String translate(String fallback, String messageCode) {
    return LocalizationContext.translate(Localizable.of(fallback, messageCode), true);
}
```

Sign-out uses a **full page navigation** (`UI.getCurrent().getPage().setLocation("/logout")`), not
`ui.navigate(...)`, so the browser issues a fresh HTTP request that Spring Security's logout filter
can intercept and the session is actually invalidated — mirroring the full-page navigation already
used for login (`getPage().setLocation("/customers")` after a successful sign-in).

## Global search wiring

The AppBar search field (`search(placeholder, valueListener)`) *is* correctly wired — the listener
receives the field's value on every change. Route it to the primary listing view with a query
parameter rather than leaving it inert:

```java
private void onSearch(String query) {
    if (query != null && !query.isBlank()) {
        UI.getCurrent().navigate(CustomerListView.class, QueryParameters.simple(Map.of("q", query)));
    }
}
```

The target view reads the parameter via `BeforeEnterObserver` and folds it into the same filter used
by its own `ListingBundle` search box / chips — see `CustomerListView` in `demo/crm-minimal` for the
reference implementation. Do not duplicate filtering logic; combine the AppBar query with whatever
the view's own search box already provides.

## Two-locale localization (English default + one additional language)

1. **English bundle** — the existing `src/main/resources/messages.properties` *is* the English /
   default-fallback bundle (no `_en` suffix needed; `MessageProvider.fromProperties` falls back to
   the no-suffix file for any unmatched locale).
2. **Additional-language bundle** — add a sibling file following `java.util.ResourceBundle` naming:
   `src/main/resources/messages_<language>.properties` (e.g. `messages_ta.properties` for Tamil).
   Mirror **every** key from `messages.properties` — never leave a key untranslated (that key will
   silently render as the English fallback, which is acceptable only if intentional).
3. Non-ASCII bundles **must** be read as UTF-8 — pass `.encoding("UTF-8")` to the
   `MessageProvider.fromProperties(...)` builder (default is ISO-8859-1, which corrupts non-Latin
   scripts).

## Wiring `LocalizationContext` per Vaadin session

`LocalizationContext` is not auto-configured by the Spring Boot starter in this library version.
Wire it explicitly with a single collaborator in a `@Configuration` class. **Do not** register a
Vaadin `I18NProvider` bean (e.g. `LocalizationContextI18NProvider.create(locales)`) alongside it —
that factory builds its own internal `LocalizationContext` instance, separate from the
session-bound one the `LocalizationSessionInitializer` below creates and the language switcher
mutates. With both in play, `getTranslation(...)` call sites resolve against the *wrong* context and
appear frozen when the user switches locale. Use only `Localizable.of(...)` in builder overloads
plus the `translate(fallback, messageCode)` helper above (backed by
`LocalizationContext.translate(...)`) for the rare case a plain `String` is required — never
`getTranslation(...)`.

```java
@Configuration
public class CrmLocalizationConfig {

    public static final Locale TAMIL = Locale.of("ta");

    // Ensures a session-bound LocalizationContext exists before the first view is built.
    @Component
    public static class LocalizationSessionInitializer implements VaadinServiceInitListener {
        @Override
        public void serviceInit(ServiceInitEvent event) {
            event.getSource().addUIInitListener(uiEvent -> VaadinSessionScope.require()
                    .putIfAbsent(LocalizationContext.CONTEXT_KEY,
                            LocalizationContext.builder()
                                    .withMessageProvider(MessageProvider.fromProperties("messages")
                                            .encoding("UTF-8").build())
                                    .withInitialLocale(Locale.ENGLISH)
                                    .build()));
        }
    }
}
```

- `VaadinServiceInitListener` + `@Component` is an accepted Spring-lifecycle exception (Vaadin
  auto-discovers Spring beans of this type) — it is **not** a case where plain Holon `Context` wiring
  applies, since the resource must be created once per `VaadinSession`, not once per application.
- `VaadinSessionScope` (`com.holonplatform.vaadin.flow.VaadinSessionScope`) is a Holon `ContextScope`
  bound to the current `VaadinSession`; it is auto-registered via `ServiceLoader` (see
  `META-INF/services/com.holonplatform.core.ContextScope`) — no manual scope registration needed.
- `LocalizationContext.require().localize(newLocale)` mutates the existing session-bound context
  in place and fires a `LocalizationChangeListener`; because most Holon Vaadin components resolve
  `Localizable` labels once at construction time rather than reactively, follow every `localize(...)`
  call with `UI.getCurrent().getPage().reload()` so the whole view tree re-renders in the new locale.

## Pre-emit checklist additions

- [ ] AppBar has a working search field (`search(placeholder, listener)`, listener wired to a real view/filter — not left as a no-op)
- [ ] AppBar has a notifications bell (`notifications(badgeCount, items...)`)
- [ ] AppBar has a user avatar with a **working** Sign out action, built via `customizeEnd(...)` + `AvatarBuilder` + `ContextMenuBuilder` — not the inert `.user(...).menu(...)` alone
- [ ] AppBar has a language switcher (globe icon) with **working** click handlers, built via `customizeEnd(...)` + `ButtonBuilder` + `ContextMenuBuilder` — not the inert `.languages(...)` alone
- [ ] At least two message bundles exist: `messages.properties` (English default) + `messages_<lang>.properties` for every additional supported language, with identical key sets
- [ ] Non-ASCII bundles are loaded with `.encoding("UTF-8")`
- [ ] No Vaadin `I18NProvider` bean is registered and no `getTranslation(...)` call sites exist — every label uses `Localizable.of(...)` (builder overload) or the `translate(fallback, messageCode)` helper backed by `LocalizationContext.translate(...)`
- [ ] A `VaadinServiceInitListener` ensures a session-bound `LocalizationContext` exists before any view is constructed
- [ ] Locale switch calls `LocalizationContext.require().localize(locale)` followed by a full page reload
