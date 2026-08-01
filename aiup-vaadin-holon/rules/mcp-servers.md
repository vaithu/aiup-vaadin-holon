# MCP Servers

The `aiup-vaadin-holon` plugin configures the following MCP servers in
[`../.mcp.json`](../.mcp.json).

## Configured servers

| Server | URL / command | Purpose |
|--------|--------------|---------|
| `context7` | `https://mcp.context7.com/mcp` | Up-to-date library documentation for any package |
| `Vaadin` | `https://mcp.vaadin.com/docs` | Vaadin Flow component documentation (fallback component lookup) |
| `JavaDocs` | `https://www.javadocs.dev/mcp` | JavaDoc API lookup for Holon Platform and other Java libraries |
| `playwright` | `npx @playwright/mcp@latest` (stdio) | Playwright browser automation for E2E test authoring |

## Not included (intentionally)

| Server | Reason excluded |
|--------|----------------|
| `KaribuTesting` | Karibu is a Vaadin-specific test framework — Holon Vaadin tests use `vaadin-testbench-unit-junit5` instead |
| `jOOQ` | jOOQ is not part of the Holon stack — persistence is via Holon `Datastore` + `BeanPropertySet` |

## Usage in skills

- **`/implement`**: consult `Vaadin` MCP to verify that a requested UI component exists in Vaadin 25 before using it as a fallback; consult `JavaDocs` MCP for Holon Platform API signatures.
- **`/holon-vaadin-test`**: consult `Vaadin` MCP for Vaadin Browserless (`vaadin-testbench-unit-junit5`) test APIs.
- **`/playwright-test`**: the `playwright` stdio server provides browser automation for E2E test generation.
- **Any skill**: use `context7` to retrieve the latest documentation for any library (Holon, Spring Boot, Flyway, Testcontainers).

## How to add or override an MCP server

Edit `aiup-vaadin-holon/.mcp.json`. The format follows the standard MCP configuration:

```json
{
  "mcpServers": {
    "MyServer": {
      "type": "http",
      "url": "https://my-mcp-server.example.com/mcp"
    }
  }
}
```

For stdio servers (like Playwright):

```json
{
  "mcpServers": {
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["@playwright/mcp@latest"],
      "env": {}
    }
  }
}
```
