# Orchestrator setup — chat view + `AIOrchestrator`

The `AIOrchestrator` (free `vaadin-ai-core-flow` module) is a **non-visual** coordination
engine. It wires the chat UI components to an `LLMProvider` and handles streaming, history,
attachments, and tool calling. Do **not** add the orchestrator itself to a layout.

## 1. Enable the feature flag

Vaadin AI components are a preview feature in Vaadin 25.1+. Enable them in
`src/main/resources/vaadin-featureflags.properties`:

```properties
com.vaadin.experimental.aiComponents=true
```

## 2. Enable server push on the app shell

Streaming responses (the default) push partial tokens as they arrive, which requires server
push. Add `@Push` to the `AppShellConfigurator` class:

```java
@Push
@SpringBootApplication
public class Application implements AppShellConfigurator {
    // @StyleSheet(Lumo.STYLESHEET), @StyleSheet("styles.css"), etc.
}
```

> Synchronous mode does not require push. If push is unavailable, disable streaming on the
> provider (`provider.setStreaming(false)`); a warning is logged at runtime otherwise.

## 3. Build the chat view

Use the stock Vaadin chat components — `MessageList` + `MessageInput` — and, optionally,
`Upload` / `UploadManager` for file attachments. These are the **only** raw Vaadin components
allowed here because Holon Vaadin Flow has no chat equivalent. Guard the route with Holon Auth
exactly like every other view.

```java
@Authenticate
@RolesAllowed("customers:read")
@Route(value = "customers/assistant", layout = MainLayout.class)
public class CustomerAssistantView extends VerticalLayout {

    public CustomerAssistantView(LLMProvider provider, AIController holonController) {
        // Fresh component instances — each belongs to exactly ONE orchestrator.
        MessageList messageList = new MessageList();
        MessageInput messageInput = new MessageInput();

        // System prompt is mandatory — set role, tone, constraints, domain rules.
        String systemPrompt = LocalizationContext.require().getMessage(
                "customer.assistant.systemPrompt",
                "You are a helpful assistant for the customer module. "
                        + "Answer concisely and only from the data available to you.");

        AIOrchestrator orchestrator = AIOrchestrator
                .builder(provider, systemPrompt)
                .withMessageList(messageList)
                .withInput(messageInput)
                .withController(holonController)   // Holon Datastore-backed custom controller
                .withUserName("You")
                .withAssistantName("Assistant")
                .build();

        // The orchestrator is NOT added to the layout — only the UI components are.
        add(messageList, messageInput);
        expand(messageList);
    }
}
```

### Rules recap

- **One orchestrator per instance** — a given `LLMProvider`, `MessageList`, `MessageInput`,
  file receiver, and `AIController` may be passed to only one `AIOrchestrator`. Sharing throws
  `IllegalStateException` at build time. Create a fresh set per view.
- **Always provide a system prompt** — never call `builder(provider)` without one.
- **Non-visual orchestrator** — add the components to the layout, not the orchestrator.
- **Optional file attachments** — add `.withFileReceiver(new UploadManager(...))` (or an
  `Upload`) when the use case needs document input.
