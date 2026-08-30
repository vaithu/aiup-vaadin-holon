# LLM provider configuration

The `AIOrchestrator` talks to an LLM through the `LLMProvider` interface. Two implementations
ship with the free `vaadin-ai-core-flow` module: `SpringAILLMProvider` (default for this stack)
and `LangChain4JLLMProvider`. You can also implement `LLMProvider` yourself.

> **Memory window:** both built-in providers keep a 30-message working-memory window. The
> orchestrator's `getHistory()` retains the full conversation, but the model only sees the most
> recent 30 messages.

## Default — Spring AI (`SpringAILLMProvider`)

`org.springframework.ai.*` is an allowed LLM-framework integration (it is **not** the banned
Spring Security / Spring Data JPA). Wire the provider as a `@Bean` and read the API key from
configuration — **never hard-code secrets**.

`application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}     # supplied via environment variable
      chat:
        options:
          model: gpt-4o-mini
```

Provider configuration (constructor injection — no `@Autowired`):

```java
@Configuration
class AiConfig {

    // Prefer constructing from a ChatModel so history restoration via withHistory() works.
    @Bean
    LLMProvider llmProvider(ChatModel chatModel) {
        SpringAILLMProvider provider = new SpringAILLMProvider(chatModel);
        provider.setStreaming(true);   // default; requires @Push
        return provider;
    }
}
```

- From a `ChatModel`, the provider manages its own 30-message memory window and supports
  `withHistory()` restoration.
- From a `ChatClient`, memory must be configured on the client, and `setHistory()` throws
  `UnsupportedOperationException`. Prefer `ChatModel` when you need to restore history.

## Alternative — LangChain4j (`LangChain4JLLMProvider`)

`dev.langchain4j.*` is likewise an allowed integration. The mode is set by the model type:

```java
// Streaming (requires @Push)
StreamingChatModel streaming = OpenAiStreamingChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY")).modelName("gpt-4o-mini").build();
LLMProvider provider = new LangChain4JLLMProvider(streaming);

// Synchronous (no push required)
ChatModel sync = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY")).modelName("gpt-4o-mini").build();
LLMProvider provider = new LangChain4JLLMProvider(sync);
```

## Custom provider

Implement `LLMProvider` to connect any framework:

```java
public class MyLLMProvider implements LLMProvider {
    @Override
    public Flux<String> stream(LLMRequest request) {
        // request.userMessage(), request.attachments(),
        // request.systemPrompt(), request.tools()
        return ...;
    }

    @Override
    public void setHistory(List<ChatMessage> history,
            Map<String, List<AIAttachment>> attachmentsByMessageId) {
        // Restore conversation context.
    }
}
```

## Secrets

Read API keys from environment variables or externalized configuration only. Do not commit keys
to the repository or embed them in Java source — the secret scanner will flag them, and the
Pre-Emit Checklist requires config-driven keys.
