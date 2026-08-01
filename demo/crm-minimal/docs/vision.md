# Vision — Minimal CRM

## Product

**MiniCRM** is a minimal Customer Relationship Management application for a small sales
team. It lets sales representatives keep a clean list of customer companies and the people
(contacts) who work at them, so that follow-up calls and emails are never lost.

## Target users

| Role | Description | Primary need |
|------|-------------|--------------|
| Sales Representative | Front-line seller who owns customer relationships | Create and browse customers and their contacts |
| Sales Manager | Oversees the team and the pipeline | View all customers and archive stale ones |

## Goals

1. Maintain a single, deduplicated list of customer companies.
2. Attach one or more contact people to each customer.
3. Restrict destructive actions (archiving a customer) to managers.

## Non-goals (kept out to stay minimal)

- Opportunity / deal pipeline tracking
- Email or calendar integration
- Reporting and dashboards

## Success criteria

- A sales rep can add a customer and a contact in under a minute.
- Only a sales manager can archive a customer.
- Every screen is reachable only by an authenticated, authorized user.
