# Restaurant Management Demo — RestoPro

This example demonstrates the **first-class UI/UX workflow** introduced by AIUP for a realistic Vaadin + Holon application.

The demo uses a small Indian restaurant called **The Spice Corner** and follows one vertical slice: taking a dine-in order, sending it to the kitchen, preparing it, billing it, and recording payment.

## What this demonstrates

```text
Vision
  ↓
Requirements
  ├──────────────→ Entity Model
  └──────────────→ UI/UX Model
                       ↓
                  HTML Mockup
                       ↓
                    Review
                       ↓
                  Use Case Spec
                       ↓
             Flyway + Vaadin/Holon
                       ↓
                     Tests
```

The important change is that UI/UX is **specified before implementation**. The HTML mockup is a visual contract, not the source of business rules.

## Demo artifacts

| Artifact | Purpose |
|---|---|
| `vision.md` | Product intent and scope |
| `requirements.md` | Functional and non-functional requirements |
| `entity_model.md` | Domain entities needed by the slice |
| `use_cases.md` | Business use cases and traceability |
| `ui/design-system.md` | Shared visual language |
| `ui/pos.md` | POS screen specification |
| `mockups/pos.html` | Human-reviewable visual prototype |

## Traceability example

```text
FR-005 Create Dine-In Order
        ↓
UC-001 Create Dine-In Order
        ↓
UI-004 POS
        ↓
mockups/pos.html
        ↓
Vaadin POS View
        ↓
Playwright / Vaadin UI test
```

## How a developer uses the demo

1. Read `vision.md` to understand the product intent.
2. Review `requirements.md` and approve the required behavior.
3. Create/refine `entity_model.md` from the approved requirements.
4. Define the visual language in `ui/design-system.md`.
5. Define the POS interaction contract in `ui/pos.md`.
6. Open `mockups/pos.html` in a browser and iterate with the product owner until approved.
7. Write the use-case specification and maintain the requirement/UI traceability.
8. Generate the Flyway migration from the approved entity model.
9. Implement the use case with Holon + Vaadin, using the approved UI specification/mockup as the visual reference.
10. Add datastore, server-side Vaadin, and browser E2E tests.

## Key rule

The HTML mockup can define **presentation and interaction constraints**, but it must not silently invent domain behavior. Requirements and use cases remain authoritative for business behavior.
