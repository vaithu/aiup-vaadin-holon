# Use Case: Create Customer

## Overview

**Use Case ID:** UC-S-001
**Use Case Name:** Create Customer
**Primary Actor:** Sales Representative
**Goal:** Register a new buying organisation as a customer so that quotes, orders, and invoices can reference it
**Status:** Approved

## Preconditions

- Sales Representative is logged into the system within their tenant's workspace
- At least one tax code is configured for the tenant

## Main Success Scenario

1. Sales Representative selects "New Customer" from the customer catalog.
2. System displays the customer creation form.
3. Sales Representative enters the customer name and billing address.
4. Sales Representative enters the customer's tax identifier, if applicable.
5. Sales Representative selects the default currency for the customer.
6. Sales Representative selects the default tax code to apply to new documents.
7. Sales Representative enters the standard payment term, in days.
8. Sales Representative confirms the new customer.
9. System validates the entered information.
10. System generates a unique customer number.
11. System records the new customer and displays it in the customer catalog with its assigned customer number.

## Alternative Flows

### A1: Missing Required Information

**Trigger:** A required field (name, billing address, currency, tax code, or payment term) is left blank (step 9)
**Flow:**

1. System displays a message identifying the missing field(s).
2. Sales Representative completes the missing information.
3. Use case continues at step 8.

### A2: Invalid Payment Term

**Trigger:** Entered payment term falls outside the allowed range of 0 to 365 days (step 9)
**Flow:**

1. System displays a message indicating the payment term must be between 0 and 365 days.
2. Sales Representative corrects the payment term.
3. Use case continues at step 8.

### A3: Sales Representative Cancels Creation

**Trigger:** Sales Representative chooses to cancel before confirming (step 3-8)
**Flow:**

1. System discards the entered information.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new customer record exists with a unique, system-generated customer number
- The customer is available for selection when creating quotes, sales orders, invoices, and activities
- The customer's default currency, default tax code, and payment term are available to pre-fill future documents

### Failure Postconditions

- No customer record is created
- The customer catalog remains unchanged
- System displays an error message to the Sales Representative describing what must be corrected

## Business Rules

### BR-001: Unique Customer Number

Every customer number is unique within the tenant's schema.

### BR-002: Payment Term Range

The standard payment term must be between 0 and 365 days, inclusive.

### BR-003: Default Tax Code Required

Every customer must have a default tax code so that new quotes, orders, and invoices can compute tax without additional input.

---
