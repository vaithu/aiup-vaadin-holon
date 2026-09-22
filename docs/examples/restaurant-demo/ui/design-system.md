# RestoPro — UI Design System

## Visual direction

A premium but operational restaurant UI: dark navy application navigation, bright content surfaces, compact spacing, rounded cards, clear status badges, and strong primary actions.

## Layout

- Desktop: persistent left navigation and top app bar.
- Tablet: compact navigation with touch-friendly controls.
- Content uses cards/panels for operational grouping.
- POS uses a menu area plus persistent current-order panel.

## Navigation

Primary sections:

1. Dashboard
2. POS
3. Orders
4. Tables
5. Kitchen
6. Menu
7. Inventory
8. Purchase
9. Customers
10. Reports
11. Settings

## Status semantics

| Status | Meaning |
|---|---|
| Available | Table/resource is ready for use |
| Occupied | Active dining session |
| Reserved | Reservation exists |
| Cleaning | Table is temporarily unavailable |
| Preparing | Kitchen is working |
| Ready | Kitchen completed preparation |
| Served | Order has been served |
| Paid | Bill settled |
| Cancelled | Transaction cancelled |

Use semantic component variants rather than hard-coded colors wherever the Holon/Vaadin component supports them.

## POS conventions

- Search is prominent and keyboard-friendly.
- Categories remain visible while browsing.
- Menu items are card-based for touch selection.
- Current order is always visible on desktop.
- Totals are visually prominent.
- Primary action is visually distinct from destructive actions.
- Destructive actions require appropriate confirmation.

## Typography

- Page title: strong, compact heading.
- Section title: medium heading.
- Body: readable operational text.
- Caption: secondary metadata.
- Monetary values: aligned and easy to scan.

## Accessibility

- Every input has a visible or accessible label.
- Icon-only actions have an accessible name.
- Status must not rely on color alone.
- Keyboard focus must remain visible.
- Touch targets should be appropriate for tablet operation.
