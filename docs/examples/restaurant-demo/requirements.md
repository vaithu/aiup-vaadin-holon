# RestoPro — Requirements

## Functional Requirements

| ID | Requirement | Priority | Trace |
|---|---|---|---|
| FR-001 | Users can authenticate and access functions permitted by their role. | Must | UI shell |
| FR-002 | Authorized users can configure restaurant tables and table status. | Must | UC-001 |
| FR-003 | Authorized users can maintain menu categories and menu items. | Must | UC-001 |
| FR-004 | A staff member can select an available table. | Must | UC-001 |
| FR-005 | A staff member can create a dine-in order for the selected table. | Must | UC-001 |
| FR-006 | A staff member can add, remove, and change quantities of order items before confirmation. | Must | UC-001 |
| FR-007 | The system can send an eligible order to the kitchen as a KOT. | Must | UC-002 |
| FR-008 | Kitchen staff can update preparation status. | Must | UC-003 |
| FR-009 | Staff can view order progress from placed through served. | Must | UC-004 |
| FR-010 | The system can generate a bill for a completed order. | Must | UC-005 |
| FR-011 | The system can record cash, card, and UPI payments. | Must | UC-006 |
| FR-012 | Staff can close the order and release the table after payment. | Must | UC-007 |
| FR-013 | Authorized users can view daily sales and order metrics. | Should | Reporting |
| FR-014 | Authorized users can maintain inventory items and stock levels. | Should | Inventory |

## Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-001 | POS interactions should be fast and touch-friendly. |
| NFR-002 | The UI should support desktop and tablet layouts. |
| NFR-003 | Important transaction state changes must be auditable. |
| NFR-004 | Business-critical transaction data must not be silently lost. |
| NFR-005 | Authentication and authorization must use the project's Holon Auth approach. |
| NFR-006 | The visual design should remain consistent through the shared design system. |

## UI requirement

The POS must use the approved UI specification and HTML mockup as the visual and interaction contract. Business behavior remains governed by the requirements and use-case specification.
