# Use Case: Manage Contacts

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Manage Contacts
**Primary Actor:** Sales Representative
**Goal:** Attach and browse contact people belonging to a customer
**Status:** Approved

## Preconditions

- The actor is authenticated via Holon Auth.
- The actor holds the `contacts:view` permission.
- At least one customer exists.

## Main Success Scenario

1. Sales Rep opens the "Contacts" view.
2. System lists all contacts with their owning customer.
3. Sales Rep selects a customer and enters the contact's first name, last name, email, and phone.
4. System validates that the email is well-formed.
5. System stores the contact linked to the selected customer and refreshes the list.

## Alternative Flows

### A1: Invalid Email

**Trigger:** Entered email fails format validation (step 4)
**Flow:**

1. System shows a validation message on the email field.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- A new contact exists linked to the chosen customer via `customer_id`.

### Failure Postconditions

- No contact is created when the email is invalid; the list is unchanged.

## Business Rules

- **BR-003:** A contact must reference an existing customer.
- **BR-004:** A contact email must be a valid email address.

## Traceability

Requirements: FR-004, FR-005, NFR-001
