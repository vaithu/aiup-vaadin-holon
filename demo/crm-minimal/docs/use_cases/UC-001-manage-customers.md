# Use Case: Manage Customers

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Manage Customers
**Primary Actor:** Sales Representative
**Goal:** Register and browse customer companies; archive stale ones (manager only)
**Status:** Approved

## Preconditions

- The actor is authenticated via Holon Auth.
- The actor holds the `customers:view` permission.

## Main Success Scenario

1. Sales Rep opens the "Customers" view.
2. System lists all active customers (name, industry, status).
3. Sales Rep enters a new customer's name, industry, and confirms.
4. System validates that the name is unique.
5. System stores the customer with status `ACTIVE` and refreshes the list.

## Alternative Flows

### A1: Duplicate Customer Name

**Trigger:** Entered name matches an existing customer (step 4)
**Flow:**

1. System shows a "customer already exists" message.
2. Use case continues at step 3.

### A2: Archive Customer (Manager only)

**Trigger:** Sales Manager selects a customer and chooses "Archive" (step 2)
**Flow:**

1. System verifies the actor holds `customers:archive`.
2. System sets the customer's status to `ARCHIVED`.
3. System removes the customer from the active list.

## Postconditions

### Success Postconditions

- A new customer exists with status `ACTIVE`, or an existing customer is `ARCHIVED`.

### Failure Postconditions

- No customer is created when the name is a duplicate; the list is unchanged.

## Business Rules

- **BR-001:** Customer names are unique across the system.
- **BR-002:** Only holders of `customers:archive` may archive a customer.

## Traceability

Requirements: FR-001, FR-002, FR-003, FR-006, NFR-002
