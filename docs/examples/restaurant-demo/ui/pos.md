# UI-004 — POS / Order Taking

## Purpose

Provide a fast touch-friendly workspace for creating and modifying a restaurant order.

## Related requirements

- FR-004
- FR-005
- FR-006

## Related use case

- UC-001 Create Dine-In Order

## Application shell

Use the RestoPro standard app shell from `ui/design-system.md`.

### Left navigation

Dashboard, POS, Orders, Tables, Kitchen, Menu, Inventory, Purchase, Customers, Reports, Settings.

### Top bar

- Restaurant/outlet selector
- Global search
- Notifications
- Current user / role

## Main layout

```text
┌───────────────┬─────────────────────────────┬─────────────────────┐
│ App Navigation│ Menu / Item Selection       │ Current Order       │
│               │                             │                     │
│ POS           │ Search                      │ Dine-in · T5        │
│ Orders        │ Category tabs               │                     │
│ Tables        │ Item cards                  │ Items               │
│ Kitchen       │                             │ Qty   Amount        │
│ Menu          │                             │                     │
│ Inventory     │                             │ Subtotal            │
│ ...           │                             │ CGST                │
│               │                             │ SGST                │
│               │                             │ Total               │
│               │                             │                     │
│               │                             │ Save  Generate Bill  │
└───────────────┴─────────────────────────────┴─────────────────────┘
```

## Menu area

- Search field for menu items.
- Category tabs.
- Menu item cards.
- Each card shows image, item name, and price.
- Selecting an item adds it to the current order.

## Current order

Display:

- Order type
- Table number
- Item name
- Quantity
- Amount
- Subtotal
- Discount when applicable
- CGST
- SGST
- Total

## Actions

- Save
- Clear
- Generate Bill

Destructive actions such as clearing a non-empty order should require confirmation where appropriate.

## States

### Empty order

Show an instructional empty state and do not allow bill generation.

### Active order

Show items and calculated totals.

### Item unavailable

Unavailable menu items must not be selectable.

### Validation error

Keep the user on the POS screen and identify the invalid condition without losing entered data.

### Loading

Preserve the shell and provide clear loading feedback for operations that require server interaction.

## Responsive behavior

Desktop: menu and current order remain side-by-side.

Tablet: maintain the current order as an accessible panel; avoid forcing navigation away from menu selection.

## Visual reference

`../mockups/pos.html`

The HTML mockup is an approved visual reference only after product-owner review. It must remain traceable to this specification and UC-001.
