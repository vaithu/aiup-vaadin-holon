# Holon Datastore-backed data access for the LLM

The commercial `GridAIController` / `ChartAIController` / `FormAIController` (from
`vaadin-ai-extensions-flow`) are **banned** in this stack. Instead, expose application data to
the LLM through a **custom `AIController` / `DatabaseProvider`** whose logic delegates to the
Holon `Datastore` (or a `BeanDatastoreHelper<T>`) — the same persistence layer the rest of the
app uses. This keeps persistence Holon-first and needs no commercial subscription.

Two complementary approaches, both from the free `vaadin-ai-core-flow` module:

1. **`DatabaseProvider`** — exposes a schema + query execution so the LLM can write and run
   read-only SQL; results render in a UI component and are never returned to the model.
2. **Custom `AIController`** — contributes named tools (with JSON-Schema parameters) whose
   `execute` methods call your Holon `*Service` / `BeanDatastoreHelper` and return a string.

Choose `DatabaseProvider` for open-ended natural-language querying; choose a custom
`AIController` when you want to constrain the LLM to a fixed set of safe, well-described
operations backed by your service methods.

## Holon-backed `DatabaseProvider`

```java
public final class HolonDatabaseProvider implements DatabaseProvider {

    private final Datastore datastore;   // read-only Datastore, constructor-injected

    public HolonDatabaseProvider(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public String getSchema() {
        // Describe ONLY the tables/columns this use case is allowed to expose.
        // Add plain-English business context to improve the LLM's queries.
        return """
                customers(id BIGINT, name VARCHAR, email VARCHAR, tier_id BIGINT)
                customer_tiers(id BIGINT, name VARCHAR)
                Dialect: PostgreSQL. Join customers.tier_id = customer_tiers.id.
                """;
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        // Execute through the Datastore/DataSource backed by a READ-ONLY account.
        // Returned rows are rendered in the UI component only — never sent to the LLM.
        ...
    }
}
```

## Holon-backed custom `AIController` (tools)

```java
public final class CustomerAIController implements AIController {

    private final CustomerService service;   // existing Holon BeanDatastoreHelper-based service

    public CustomerAIController(CustomerService service) {
        this.service = service;
    }

    @Override
    public List<LLMProvider.ToolSpec> getTools() {
        return List.of(new LLMProvider.ToolSpec() {
            @Override public String getName() { return "Customer_findByTier"; }
            @Override public String getDescription() {
                return "Returns the number of customers in the given tier name.";
            }
            @Override public String getParametersSchema() {
                return """
                        { "type": "object",
                          "properties": { "tier": { "type": "string" } },
                          "required": ["tier"] }""";
            }
            @Override public String execute(JsonNode arguments) {
                String tier = arguments.get("tier").asString();
                // Delegates to the Holon service (BeanDatastoreHelper under the hood).
                return String.valueOf(service.countByTier(tier));
            }
        });
    }

    @Override
    public void onResponse(Throwable error) {
        if (error != null) {
            return;   // release any per-turn state captured in onRequest()
        }
        // Apply deferred UI/state changes here.
    }
}
```

### Rules

- **Tool names** must match `^[a-zA-Z0-9_-]{1,64}$`; prefix them (e.g. `Customer_findByTier`)
  to avoid collisions. Invalid names throw `IllegalArgumentException` at build time.
- **One controller per orchestrator.** To combine several tool sources, compose them into one
  controller that delegates, or also register tool objects via `withTools()` — the orchestrator
  merges the lists per request.
- **Delegate to Holon.** Tool/query bodies call the injected `Datastore` /
  `BeanDatastoreHelper` / `*Service`. Do **not** open raw JDBC connections handed to the LLM.
- **Schema only, never rows.** With `DatabaseProvider`, the LLM sees `getSchema()` output only;
  every row from `executeQuery` is rendered in the UI and discarded from the request cycle.
- **Controllers are not serialized.** After session restore, re-attach via
  `reconnect(provider).withController(controller).apply()`.
