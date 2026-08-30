---
name: ai-assistant
description: >
  Adds an AI-powered chat assistant to a Holon Platform + Vaadin Flow use case using the
  free vaadin-ai-core-flow module: a Vaadin chat view (MessageList + MessageInput, optional
  Upload), an AIOrchestrator bean, an LLMProvider (default SpringAILLMProvider, config-driven
  API key), and a Holon Datastore-backed custom AIController / DatabaseProvider that exposes
  the use case's entities read-only. Use when the user asks to "add an AI assistant", "add a
  chatbot", "add AI chat to UC-XXX", "let users query data in natural language", or mentions
  AIOrchestrator, LLMProvider, Spring AI, LangChain4j, or an AI-powered Vaadin view. Uses the
  free Vaadin AI core module only — never the commercial Grid/Chart/Form AI controllers.
---

# Add AI Assistant

## Prerequisites

Before writing any code, verify that these artifacts exist:

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-XXX-*.md` (the use case the assistant supports) | `/use-case-spec` |
| `docs/entity_model.md` | `/entity-model` |
| An implemented service + bean/model for the use case (a `*Service` using `BeanDatastoreHelper<T>`) | `/implement UC-XXX` |

If any file is missing, **stop** and tell the user which skill to run first:
> "`docs/use_cases/UC-XXX-*.md` not found — run `/use-case-spec UC-XXX` first." (if the use case spec is missing)  
> "`docs/entity_model.md` not found — run `/entity-model` first." (if the entity model is missing)  
> "No implemented `*Service` for UC-XXX found — run `/implement UC-XXX` first." (if the use case has not been implemented)

Do not attempt to infer or recreate the missing artifact.

## Instructions

Add an AI chat assistant for the use case `$ARGUMENTS` using the **free** `vaadin-ai-core-flow`
module (Vaadin AI) wired to Holon Datastore for data access.

Read the Constraints section and the Pre-Emit Checklist before writing any code.
If the Vaadin or JavaDocs MCP servers are configured, consult them for `AIOrchestrator`,
`LLMProvider`, `AIController`, and `DatabaseProvider` signatures; otherwise rely on the
reference documents in this skill's `references/` folder.

Do **not** create test classes — use the dedicated `datastore-test`, `holon-vaadin-test`,
and `playwright-test` skills for that.

## Constraints

**Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) — §"AI integration
(Vaadin AI + Holon Datastore)" — before generating.**

### Allowed

- `com.holon-platform.*` / `com.holonplatform.*` — all Holon modules (persistence, auth, I18N)
- `com.vaadin.flow.ai.*` and `com.vaadin.flow.component.ai.*` — `AIOrchestrator`, `LLMProvider`,
  `AIController`, `DatabaseProvider` from the **free** `vaadin-ai-core-flow` module
- `com.vaadin.flow.component.messages.*` — `MessageList`, `MessageInput` chat components
  (**only** inside this AI view; no Holon equivalent — no `// FALLBACK:` comment needed)
- `com.vaadin.flow.component.upload.*` — `Upload` / `UploadManager` for file attachments
  (**only** inside this AI view; optional)
- `org.springframework.ai.*` — Spring AI, wired via `SpringAILLMProvider` (default provider)
- `dev.langchain4j.*` — LangChain4j, wired via `LangChain4JLLMProvider` (alternative provider)
- `com.vaadin.flow.component.page.Push` (`@Push`) on the `AppShellConfigurator` class — required
  for streaming responses
- `org.springframework.stereotype.{Service,Component}` / `@Configuration` / `@Bean` — permitted
  for wiring the provider/orchestrator when Spring lifecycle is required; inject via constructors

### Banned — refuse to emit

- `com.vaadin:vaadin-ai-extensions-flow` and the built-in commercial controllers
  `GridAIController`, `ChartAIController`, `FormAIController` — **commercial subscription
  required**; use a Holon-backed custom `AIController` / `DatabaseProvider` instead
- Raw JDBC / `java.sql.DataSource` handed directly to the LLM — data access MUST go through the
  Holon `Datastore` / `BeanDatastoreHelper`
- `com.holonplatform.core.property.PropertyBox` — use `Bean` + `BeanPropertySet` exclusively
- Any raw Vaadin core component outside the AI/chat/upload packages listed above — the rest of
  the view still follows the Holon-only rule; stop and ask the developer if no Holon equivalent
  exists
- `org.springframework.beans.factory.annotation.Autowired` — use constructor injection
- Returning query **rows** to the LLM — only the schema description may reach the model
- An `AIOrchestrator` built **without** a system prompt

## Pre-Emit Checklist

- [ ] Uses the **free** `vaadin-ai-core-flow` only — no `vaadin-ai-extensions-flow`, no
      `GridAIController` / `ChartAIController` / `FormAIController`
- [ ] Data exposed to the LLM goes through a Holon-backed custom `AIController` /
      `DatabaseProvider` that delegates to the injected `Datastore` / `BeanDatastoreHelper` —
      no raw JDBC handed to the LLM
- [ ] The `DatabaseProvider` / query tools are backed by a **read-only** database account
- [ ] `getSchema()` returns a plain-text schema description only; `executeQuery` results are
      rendered in the UI and **never** returned to the model
- [ ] `AIOrchestrator.builder(provider, systemPrompt)` is always given a **system prompt**
- [ ] Each `LLMProvider` / `MessageList` / `MessageInput` / controller belongs to exactly **one**
      `AIOrchestrator` (fresh instances per view)
- [ ] `AIOrchestrator` is treated as non-visual — it is **not** added to a layout
- [ ] `@Push` is present on the `AppShellConfigurator` class (streaming requires server push)
- [ ] The `com.vaadin.experimental.aiComponents` feature flag is enabled in
      `src/main/resources/vaadin-featureflags.properties`
- [ ] The LLM API key is read from configuration (`application.yml` / environment variable) —
      **no hard-coded API keys or secrets** in Java
- [ ] The chat route is guarded with `@Authenticate` + `@RolesAllowed`, like every other view
- [ ] No `@Autowired` — provider, orchestrator, and services injected via constructors
- [ ] All user-visible strings (labels, placeholders, system-prompt UI text) use Holon I18N
      (`Localizable` / `LocalizationContext`)
- [ ] Full compilation verified (`./mvnw compile` or `./gradlew compileJava`)

## Workflow

1. Read the use case specification from `docs/use_cases/UC-XXX-*.md` and the entity model.
2. Identify the existing `*Service` / `*Model` / bean for the use case (from `/implement`).
3. Read [`references/orchestrator-setup.md`](references/orchestrator-setup.md) — chat view +
   `AIOrchestrator` builder wiring, `@Push`, feature flag.
4. Read [`references/llm-provider-config.md`](references/llm-provider-config.md) — `SpringAILLMProvider`
   (default) / `LangChain4JLLMProvider`, config-driven API key.
5. Read [`references/holon-datastore-provider.md`](references/holon-datastore-provider.md) —
   Holon `Datastore`-backed custom `AIController` / `DatabaseProvider` and tools.
6. Read [`references/security-and-guardrails.md`](references/security-and-guardrails.md) —
   read-only account, prompt-injection notes, Holon Auth guarding the view, schema-only boundary.
7. **Feature package**: place the chat view, provider configuration, and the custom
   `AIController` / `DatabaseProvider` in the same `com.example.<app>.<feature>` package as the
   use case's existing bean/model/service.
8. **App shell**: ensure `@Push` is on the `AppShellConfigurator` class and the
   `aiComponents` feature flag is enabled.
9. **LLM provider**: create a `SpringAILLMProvider` (default) `@Bean` from a Spring AI
   `ChatModel`/`ChatClient`, with the API key read from configuration.
10. **Data layer**: implement a Holon-backed custom `AIController` / `DatabaseProvider` that
    delegates to the injected `Datastore` / `BeanDatastoreHelper` and is backed by a read-only
    account; `getSchema()` describes only the use case's tables/columns.
11. **View**: create a Holon-guarded (`@Authenticate` + `@RolesAllowed`) Vaadin chat view with
    `MessageList` + `MessageInput` (and optional `Upload`), build the `AIOrchestrator` with a
    system prompt, and attach the provider, components, and custom controller.
12. Run the Pre-Emit Checklist — fix any violation before proceeding.
13. Verify the implementation compiles successfully.

## Resources

- [`references/orchestrator-setup.md`](references/orchestrator-setup.md) — `AIOrchestrator`
  builder, chat components, `@Push`, feature flag
- [`references/llm-provider-config.md`](references/llm-provider-config.md) — `SpringAILLMProvider`
  / `LangChain4JLLMProvider`, config-driven API key, custom `LLMProvider`
- [`references/holon-datastore-provider.md`](references/holon-datastore-provider.md) —
  Holon Datastore-backed `AIController` / `DatabaseProvider` and tools
- [`references/security-and-guardrails.md`](references/security-and-guardrails.md) — read-only
  account, prompt-injection mitigation, Holon Auth, schema-only boundary
- If configured, use the Vaadin MCP server (`https://mcp.vaadin.com/docs`) for the AI-support docs
- If configured, use the JavaDocs MCP server (`https://www.javadocs.dev/mcp`) for Holon Platform API
- See [`../../rules/mcp-servers.md`](../../rules/mcp-servers.md) to configure MCP servers
