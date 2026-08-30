# Security & guardrails for the AI assistant

An AI chat surface lets an untrusted party (via the LLM) influence what queries run and what
data is surfaced. Apply every guardrail below.

## 1. Read-only database access

The LLM writes the SQL that a `DatabaseProvider` executes. Back the `DatabaseProvider` (and any
tool that runs queries) with a database account that has **read-only** access to exactly the
tables/views you intend to expose. This prevents the LLM from modifying or deleting data and
limits the blast radius of a prompt-injection attempt.

- Use a dedicated read-only role/connection for the AI path, separate from the app's read-write
  `Datastore`.
- Never expose write operations (INSERT/UPDATE/DELETE) through an AI tool unless the use case
  explicitly requires it and it is separately authorized.

## 2. Schema-only boundary — rows never reach the LLM

With `DatabaseProvider`, the model receives only the `getSchema()` string. Every row returned by
`executeQuery` is rendered in the UI component and discarded from the request cycle — it cannot
leak into a follow-up prompt, the conversation history, or the provider. Preserve this boundary:
do not echo raw row data back into tool results that are sent to the model unless it is
non-sensitive and intentional.

## 3. Minimal schema scope

Return only the tables, columns, and relationships the LLM needs. A smaller, well-described
schema produces better queries, uses fewer tokens, and reduces the chance of exposing sensitive
columns. Omit PII columns the assistant has no reason to see.

## 4. Prompt-injection awareness

Attacker-controlled text (user messages, uploaded file contents) can try to override the system
prompt or coax destructive actions. Mitigations:

- The read-only account (guardrail 1) neutralizes destructive SQL regardless of what the LLM is
  told to do.
- Keep a firm **system prompt** stating the assistant's scope and that it must only use the
  provided tools/schema.
- Prefer a **constrained custom `AIController`** (a fixed set of well-described tools backed by
  Holon services) over open-ended SQL when the use case allows — it bounds what the LLM can do.
- Validate/whitelist tool arguments in `execute` before using them.

## 5. Holon Auth guards the view

The AI chat route is a normal view — guard it with Holon Auth, not Spring Security:

```java
@Authenticate
@RolesAllowed("customers:read")
@Route(value = "customers/assistant", layout = MainLayout.class)
public class CustomerAssistantView extends VerticalLayout { ... }
```

Also gate the assistant's capabilities to the current user's permissions
(`AuthContext.require().isPermitted(...)`) — do not let the LLM surface data the signed-in user
is not authorized to see. Where feasible, scope the read-only queries to the authenticated
principal.

## 6. Secrets

LLM API keys come from environment variables / externalized configuration only — never
hard-coded in Java and never committed. See `llm-provider-config.md`.

## 7. Mandatory system prompt

Never build an `AIOrchestrator` without a system prompt. Without one the model has no guidance
beyond tool descriptions and behaves inconsistently, widening the injection surface.
