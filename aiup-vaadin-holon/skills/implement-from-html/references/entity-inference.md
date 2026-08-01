# Entity Inference Reference

Infer JavaBeans (domain objects) from HTML data regions. Use this reference during
Step 2 of the `/implement-from-html` pipeline.

---

## Region → Bean mapping

| HTML region | Bean strategy |
|-------------|--------------|
| Master list (repeating rows in `<table>`, `<tr>`) | One **collection Bean** (e.g. `Bill`) — each visible column maps to a Bean field |
| Detail / edit form (`<form>`, labeled `<input>` groups) | Fields on the **same Bean** as the master list, or a separate **detail Bean** if clearly distinct |
| Sub-row list within detail (line items, allocations) | A **child Bean** (e.g. `BillLineItem`) with a FK field pointing to the parent |
| Read-only summary section | No new Bean — fields on the existing Bean, exposed in a read-only `PropertyForm` |
| Tabs with different entity types | One Bean per tab if columns/fields are structurally different |

---

## Field type inference

| HTML element / attribute | Inferred Java type | `@DataPath` note |
|--------------------------|-------------------|-----------------|
| `<input type="text">` | `String` | camelCase → snake_case |
| `<input type="number">` | `Integer` or `BigDecimal` (use `BigDecimal` if decimals implied) | — |
| Currency display (`$`, `€`, currency symbol in label) | `BigDecimal` | e.g. `total_amount` |
| `<input type="date">` | `LocalDate` | — |
| `<input type="datetime-local">` | `LocalDateTime` | — |
| `<input type="email">` | `String` | add email validator |
| `<input type="checkbox">` or toggle | `Boolean` | — |
| `<select>` with a short fixed list of options | `String` | `Not Null, Values: A, B, C` in entity model |
| `<select>` referencing another entity | `Long` (FK) | creates a FK field and a relationship |
| `<textarea>` | `String` (longer) | increase length hint to 2000 |
| ID / reference number column (read-only, sequence-looking) | `Long` with `@Identifier` | PK |
| Status column with badge colors | `String` with fixed values | `Not Null, Values: <from options>` |
| Date column (display only, no time) | `LocalDate` | — |
| Timestamp column | `LocalDateTime` | — |

---

## Naming conventions

1. **Bean class name**: PascalCase of the entity concept (e.g. `Bill`, `BillLineItem`, `PurchaseOrder`).
2. **Field name**: camelCase of the column header (e.g. "Invoice Number" → `invoiceNumber`).
3. **`@DataPath` value**: snake_case of the column header (e.g. `invoice_number`).
4. **Table name**: snake_case plural of the Bean class (e.g. `bill`, `bill_line_item`).

---

## Master-detail example

**HTML structure:**
```
┌─────────────────────────────────────────────────────────┐
│ Bills                               [New Bill] [Export] │
├─────┬───────────────┬──────────────┬──────────┬─────────┤
│  #  │ Vendor        │ Invoice No   │ Amount   │ Status  │
├─────┼───────────────┼──────────────┼──────────┼─────────┤
│ 101 │ ACME Corp     │ INV-2024-001 │ $1,500   │ PENDING │
│ 102 │ GlobalSupply  │ INV-2024-002 │ $3,200   │ APPROVED│
└─────┴───────────────┴──────────────┴──────────┴─────────┘

Detail pane (opens on row click):
  Vendor Name:    [ACME Corp          ]
  Invoice Number: [INV-2024-001       ]
  Invoice Date:   [2024-01-15         ]
  Total Amount:   [$1,500.00          ]
  Status:         [PENDING ▼          ]
  PO Number:      [PO-2024-100        ]  ← FK to PurchaseOrder
  [Save]  [Approve]  [Reject]
```

**Inferred Bean:**

```java
@DataPath("bill")
public class Bill {
    public static final BeanPropertySet<Bill> PROPERTIES = BeanPropertySet.create(Bill.class);

    @Identifier
    @DataPath("id")
    private Long id;                           // column "#"

    @DataPath("vendor_name")
    private String vendorName;                 // "Vendor"

    @DataPath("invoice_number")
    private String invoiceNumber;              // "Invoice No"

    @DataPath("invoice_date")
    private LocalDate invoiceDate;             // "Invoice Date"

    @DataPath("total_amount")
    private BigDecimal totalAmount;            // "Amount" — currency symbol → BigDecimal

    @DataPath("status")
    private String status;                     // "Status" — Values: PENDING, APPROVED, REJECTED

    @DataPath("purchase_order_id")
    private Long purchaseOrderId;              // "PO Number" → FK to PurchaseOrder
}
```

---

## Ambiguity handling

- **Cannot determine type**: default to `String`, emit a warning, and ask the user to confirm.
- **No ID column visible**: infer a synthetic `id` (`Long`, `@Identifier`) as the first field.
- **Relationship target unclear**: name the FK field `<entity>Id` (e.g. `purchaseOrderId`) and
  note that a `PurchaseOrder` Bean will be needed.
- **Sub-rows with no column headers**: skip them and warn — `WARNING: Sub-rows in '<region>' have no column headers. Cannot infer child entity fields. Please describe the data structure.`
