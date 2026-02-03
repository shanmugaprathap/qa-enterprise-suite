package com.enterprise.qa.core.ai.datagen;

import com.enterprise.qa.core.config.ConfigManager;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Client for interacting with OpenAI's LLM API.
 * Provides a simple interface for text completion tasks.
 */
@Slf4j
public class LLMClient {

    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private OpenAiService service;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public LLMClient() {
        this(DEFAULT_MODEL, DEFAULT_MAX_TOKENS, DEFAULT_TEMPERATURE);
    }

    public LLMClient(String model, int maxTokens, double temperature) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        initializeService();
    }

    /**
     * Initializes the OpenAI service with API key from configuration.
     */
    private void initializeService() {
        String apiKey = ConfigManager.getInstance().getOpenAiApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                this.service = new OpenAiService(apiKey, DEFAULT_TIMEOUT);
                log.info("OpenAI service initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize OpenAI service: {}", e.getMessage());
                this.service = null;
            }
        } else {
            log.warn("OpenAI API key not configured. AI features will be disabled.");
            this.service = null;
        }
    }

    /**
     * Sends a completion request to the LLM.
     *
     * @param prompt the prompt to complete
     * @return the completion text, or null if failed
     */
    public String complete(String prompt) {
        if (service == null) {
            log.warn("OpenAI service not available. Returning null.");
            return null;
        }

        try {
            ChatMessage userMessage = new ChatMessage(
                    ChatMessageRole.USER.value(),
                    prompt
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(userMessage))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            ChatCompletionResult result = service.createChatCompletion(request);

            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                String content = result.getChoices().get(0).getMessage().getContent();
                log.debug("LLM completion successful. Tokens used: {}",
                        result.getUsage().getTotalTokens());
                return content;
            }

            log.warn("LLM returned empty response");
            return null;

        } catch (Exception e) {
            log.error("LLM completion failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sends a completion request with a system message for context.
     *
     * @param systemPrompt the system prompt to set context
     * @param userPrompt   the user prompt to complete
     * @return the completion text, or null if failed
     */
    public String completeWithContext(String systemPrompt, String userPrompt) {
        if (service == null) {
            log.warn("OpenAI service not available. Returning null.");
            return null;
        }

        try {
            ChatMessage systemMessage = new ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    systemPrompt
            );
            ChatMessage userMessage = new ChatMessage(
                    ChatMessageRole.USER.value(),
                    userPrompt
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(systemMessage, userMessage))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            ChatCompletionResult result = service.createChatCompletion(request);

            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                return result.getChoices().get(0).getMessage().getContent();
            }

            return null;

        } catch (Exception e) {
            log.error("LLM completion with context failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the LLM service is available.
     *
     * @return true if the service is initialized and ready
     */
    public boolean isAvailable() {
        return service != null;
    }

    /**
     * Gets the model being used.
     *
     * @return the model name
     */
    public String getModel() {
        return model;
    }
}
