# AIUP UI/UX Workflow

## Purpose

UI/UX is a first-class AIUP artifact for Vaadin applications. The UI model defines
presentation, navigation, interaction, responsive behavior, accessibility, and visual
contracts without replacing requirements, domain modeling, or use-case specifications.

## Updated Process

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

## Source-of-Truth Rules

| Concern | Source of truth |
|---|---|
| Product intent | `docs/vision.md` |
| Business requirements | `docs/requirements.md` |
| Data/domain structure | `docs/entity_model.md` |
| Business behavior | `docs/use_cases/*.md` |
| UI/UX behavior and visual contract | `docs/ui/` + approved mockups |
| Database schema | Flyway migrations |
| Automated verification | `docs/test_cases/` + automated tests |

The UI never becomes the source of truth for business rules or domain structure.

## Traceability

A feature should be traceable end-to-end:

```text
FR-005 Create Dine-In Order
        ↓
UC-001 Create Dine-In Order
        ↓
UI-004 POS
        ↓
docs/mockups/pos.html
        ↓
Vaadin POS View
        ↓
Playwright / Vaadin / datastore tests
```

## UI Approval Gate

For UI-driven work, implementation should not start until:

1. The relevant requirement exists.
2. The use case exists or is explicitly identified.
3. The screen specification exists.
4. The design-system and app-shell rules are known.
5. The HTML/CSS mockup has been reviewed when visual fidelity matters.
6. Unresolved visual regions are explicitly recorded.

## Relationship to `/implement-from-html`

`/implement-from-html` remains the construction bridge from HTML to Holon/Vaadin,
but HTML is no longer treated as the only UI specification. An approved screen
specification and the related use case remain authoritative.

If HTML contradicts an approved requirement or use case, stop and request clarification
instead of silently changing the business behavior.

## Recommended Application Layout

```text
docs/
├── vision.md
├── requirements.md
├── entity_model.md
├── use_cases.puml
├── use_cases/
│   └── UC-XXX-*.md
├── test_cases/
│   └── TC-XXX-*.md
├── ui/
│   ├── design-system.md
│   ├── app-shell.md
│   └── screens/
│       └── *.md
└── mockups/
    ├── *.html
    └── *.css
```

## Example: Restaurant POS

```text
FR-005 Create Dine-In Order
        │
        ├── UC-001 Create Dine-In Order
        │       │
        │       └── UI-004 POS
        │
        └── UI-005 Tables

UI-004 POS
  ├── Menu category tabs
  ├── Menu item cards
  ├── Current order panel
  ├── Quantity controls
  ├── Tax/total summary
  └── Generate Bill action
```

The screenshot or HTML mockup is therefore a **visual contract**, while the requirement
and use-case documents remain the authoritative business contract.
