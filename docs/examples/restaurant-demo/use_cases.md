# RestoPro — Use Cases

## UC-001 — Create Dine-In Order

**Primary actors:** Captain / Waiter, Cashier

**Goal:** Create a dine-in order for a selected table.

### Main success scenario

1. Staff opens POS.
2. Staff selects a table.
3. System shows the table as selected.
4. Staff selects one or more menu items.
5. Staff changes quantities or adds item options when needed.
6. Staff reviews the current order.
7. Staff confirms the order.
8. System stores the order and marks it placed.

### Alternative flows

- If the table is unavailable, the system prevents selection and the use case ends.
- If an item is unavailable, the system prevents adding it and the staff member may choose another item.
- If the order contains no items, confirmation is not allowed and the use case continues at step 4.

## UC-002 — Send Order to Kitchen

1. Staff selects an eligible placed order.
2. Staff sends the order to the kitchen.
3. System creates a kitchen ticket.
4. Kitchen staff can see the ticket in the kitchen display.

## UC-003 — Prepare Kitchen Order

1. Kitchen staff opens an incoming kitchen ticket.
2. Kitchen staff starts preparation.
3. Kitchen staff marks the ticket ready.

## UC-004 — Serve Order

1. Staff sees that the order is ready.
2. Staff serves the order.
3. System marks the order served.

## UC-005 — Generate Bill

1. Staff opens the served order.
2. Staff requests the bill.
3. System calculates subtotal, discounts, applicable taxes, and total.
4. System displays the bill for review.

## UC-006 — Record Payment

1. Staff selects a payment method.
2. Staff enters or confirms the payment amount.
3. System records the payment.
4. System marks the bill paid when the outstanding amount reaches zero.

## UC-007 — Close Table

1. Staff closes the paid order.
2. System completes the order.
3. System releases the table.

## UI traceability

| Requirement | Use case | UI | Mockup |
|---|---|---|---|
| FR-004–FR-006 | UC-001 | UI-004 POS | `mockups/pos.html` |
| FR-007 | UC-002 | UI-006 Kitchen | future mockup |
| FR-008 | UC-003 | UI-006 Kitchen | future mockup |
| FR-009 | UC-004 | UI-007 Order Details | future mockup |
| FR-010 | UC-005 | UI-007 Billing | future mockup |
| FR-011 | UC-006 | UI-007 Billing | future mockup |
| FR-012 | UC-007 | UI-005 Tables | future mockup |
