---
name: ui-specification
description: >
  Defines the UI/UX model as a first-class AIUP artifact for Vaadin applications.
  Creates screen specifications, design-system guidance, interaction/state rules,
  traceability to requirements and use cases, and HTML/CSS mockup contracts.
  Use when the user says "define the UI", "specify this screen", "create UI specs",
  "design the Vaadin UI", "make a UI mockup", or wants UI/UX to be part of the
  AIUP workflow before implementation.
---

# UI/UX Specification

The UI/UX model is a first-class AIUP artifact. It describes **how users interact
with the product** without replacing requirements, domain modeling, or use-case
specifications.

## Position in AIUP

The UI/UX model is elaborated in parallel with the domain model after requirements:

```text
Vision
  ↓
Requirements
  ├──────────────→ Entity Model
  │
  └──────────────→ UX/UI Model
                       ├── Design System
                       ├── App Shell
                       ├── Screen Specifications
                       ├── Interaction & State Rules
                       └── HTML/CSS Mockups
                              │
                              ▼
                         Use Cases
                              │
                              ▼
                       Use Case Specs
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
        Flyway/Domain                    UI Implementation
              │                               │
              └───────────────┬───────────────┘
                              ▼
                            Tests
```

UI artifacts must never become the source of truth for business rules or the
entity model. They express presentation, interaction, states, navigation,
and visual constraints derived from requirements and use cases.

## Prerequisites

Before creating UI specifications:

- `docs/vision.md` exists.
- `docs/requirements.md` exists.
- Requirements have stable IDs (`FR-*`, `NFR-*`) where possible.
- If a use case already exists, link the UI to its `UC-*` ID.
- Do not invent domain behavior solely from visual styling.

If requirements are missing, stop and run `/requirements` first.

## Outputs

The skill creates or updates:

```text
docs/
├── ui/
│   ├── design-system.md
│   ├── app-shell.md
│   └── screens/
│       ├── <screen>.md
│       └── ...
└── mockups/
    ├── <screen>.html
    ├── <screen>.css
    └── ...
```

HTML/CSS mockups are optional until a screen is ready for visual review, but
an approved screen should have a mockup before implementation when visual fidelity
is important.

## Step 1 — Define the design system

Create or update `docs/ui/design-system.md` with the project's shared visual
language:

- Color roles: primary, secondary, success, warning, danger, surface, background,
  text, muted text, border.
- Typography hierarchy.
- Spacing scale.
- Border radius and elevation.
- Desktop/tablet/mobile breakpoints.
- App shell dimensions.
- Standard navigation patterns.
- Buttons, badges, status indicators, forms, grids, cards, dialogs, tabs, search,
  notifications, and empty/error/loading states.
- Accessibility conventions.

Prefer semantic names over literal colors. Do not hard-code a color in a screen
spec when a design-system token exists.

## Step 2 — Define the application shell

Create or update `docs/ui/app-shell.md` describing:

- App bar/header.
- Brand/logo area.
- Outlet/tenant selector where applicable.
- Global search.
- Notifications.
- User/account menu.
- Side navigation.
- Breadcrumbs.
- Main content area.
- Responsive behavior.

The app shell is shared infrastructure and should not be repeated in every screen
specification.

## Step 3 — Create a screen specification

Create `docs/ui/screens/<screen>.md` using this structure:

```markdown
# UI-XXX — <Screen Name>

## Purpose

<What the screen helps the user accomplish.>

## Related Requirements

- FR-XXX
- NFR-XXX

## Related Use Cases

- UC-XXX

## Actors / Roles

| Role | Access | Actions |
|------|--------|---------|
| ...  | ...    | ...     |

## Layout

<Major regions and their relationship.>

## Components

| Region | Component | Content | Interaction |
|--------|-----------|---------|-------------|
| ...    | ...       | ...     | ...         |

## User Interactions

1. ...
2. ...

## States

- Initial / empty
- Loading
- Normal
- Validation error
- Permission denied
- Business-rule error
- Success

## Responsive Behavior

### Desktop
...

### Tablet
...

### Mobile
...

## Accessibility

...

## Visual Reference

`docs/mockups/<screen>.html`

## Implementation Notes

Only presentation/component guidance belongs here. Business rules remain in
requirements/use-case artifacts.
```

## Step 4 — Trace UI to behavior

Every interactive control must map to a requirement or use-case behavior.

Examples:

```text
FR-005 Create Dine-In Order
        ↓
UC-001 Create Dine-In Order
        ↓
UI-004 POS
        ↓
pos.html
        ↓
Vaadin POS View
```

A button such as `Generate Bill` must not appear as an unexplained UI action.
It should trace to a use case or an explicit requirement.

## Step 5 — Define states explicitly

Do not specify only the happy-path screenshot. For each important screen define
states that materially affect behavior or layout:

- Empty
- Loading
- Populated
- Disabled
- Validation error
- Permission restricted
- Business-rule error
- Success
- Offline/degraded state when applicable

## Step 6 — HTML/CSS mockups

HTML mockups are **visual contracts**, not the business-domain source of truth.

They should:

- Use realistic representative data.
- Follow `design-system.md`.
- Preserve the screen's major regions and interaction affordances.
- Be self-contained enough for visual review.
- Keep CSS separate from HTML when practical.
- Avoid embedding implementation-specific Vaadin code.

When the mockup is approved, it becomes input to `/implement-from-html`.

## Step 7 — Human approval gate

Before implementation, report:

- Screen specification path.
- Related requirements.
- Related use cases.
- Mockup path.
- Any visual regions that cannot be represented with standard Holon/Vaadin
  components.
- Any unresolved UX decisions.

Do not silently invent unresolved UI behavior.

## Step 8 — Implementation handoff

The preferred construction sequence is:

```text
Approved UI specification
        +
Approved HTML/CSS mockup
        +
Use-case specification
        +
Entity model
        +
Flyway schema
        ↓
/implement UC-XXX
or
/implement-from-html <approved-mockup.html>
```

`/implement-from-html` may infer missing details from HTML, but the UI specification
and use-case artifacts remain authoritative. If the HTML contradicts an approved
requirement or use case, stop and ask for clarification.

## Constraints

- Do not create entities from visual appearance alone.
- Do not encode business rules only in HTML/CSS.
- Do not make a UI screenshot the only UI specification.
- Do not introduce arbitrary colors, spacing, or component variants when the design
  system already defines a token or pattern.
- Do not use raw Vaadin components when the Holon stack has an approved equivalent;
  follow `rules/holon-stack.md` during implementation.
- UI specs must remain traceable to requirements and use cases.

## Completion Checklist

- [ ] `docs/requirements.md` exists.
- [ ] `docs/ui/design-system.md` exists or the existing design system is referenced.
- [ ] `docs/ui/app-shell.md` exists or the shared shell is referenced.
- [ ] Screen specification exists.
- [ ] Every major interactive control traces to a requirement/use case.
- [ ] Empty/loading/error/permission states are considered.
- [ ] Responsive behavior is defined where applicable.
- [ ] Accessibility expectations are stated.
- [ ] HTML/CSS mockup exists when visual approval is required.
- [ ] Unresolved visual regions are explicitly documented.
- [ ] Implementation has not started before the UI approval gate for UI-driven work.
