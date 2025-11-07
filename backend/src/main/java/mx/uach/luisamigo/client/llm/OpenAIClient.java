package mx.uach.luisamigo.client.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import mx.uach.luisamigo.config.LLMProperties;
import mx.uach.luisamigo.exception.LLMProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Implementación de LLMClient para OpenAI API (GPT-3.5, GPT-4).
 */
@Component("openaiLLMClient")
public class OpenAIClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String PROVIDER_NAME = "OpenAI";

    private final WebClient webClient;
    private final LLMProperties.OpenAIConfig config;

    public OpenAIClient(LLMProperties llmProperties, WebClient.Builder webClientBuilder) {
        this.config = llmProperties.getOpenai();
        this.webClient = webClientBuilder
            .baseUrl(OPENAI_API_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public String generateResponse(String prompt, LLMConfig llmConfig) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        try {
            log.debug("Calling OpenAI API with model: {}", config.getModel());

            OpenAIRequest request = new OpenAIRequest(
                config.getModel(),
                List.of(new Message("user", prompt)),
                llmConfig.getTemperature(),
                llmConfig.getMaxTokens()
            );

            OpenAIResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .timeout(Duration.ofSeconds(llmConfig.getTimeoutSeconds()))
                .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LLMProviderException("Empty response from OpenAI", PROVIDER_NAME);
            }

            String content = response.choices().get(0).message().content();
            log.info("OpenAI API call successful. Tokens used: {} (prompt: {}, completion: {})",
                response.usage().totalTokens(),
                response.usage().promptTokens(),
                response.usage().completionTokens());

            return content;

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LLMProviderException(
                "OpenAI API error: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new LLMProviderException(
                "Error calling OpenAI: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public double estimateCost(int estimatedTokens) {
        // Costos aproximados (varían según modelo)
        // GPT-4: ~$0.03 por 1K tokens (input), ~$0.06 por 1K tokens (output)
        // GPT-3.5-turbo: ~$0.0015 por 1K tokens (input), ~$0.002 por 1K tokens (output)
        // Promedio: ~$0.04 per 1K tokens para GPT-4
        if (config.getModel().startsWith("gpt-4")) {
            return (estimatedTokens / 1000.0) * 0.04;
        } else {
            return (estimatedTokens / 1000.0) * 0.002;
        }
    }

    // DTOs para OpenAI API
    private record OpenAIRequest(
        String model,
        List<Message> messages,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens
    ) {}

    private record Message(String role, String content) {}

    private record OpenAIResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
    ) {}

    private record Choice(int index, Message message, @JsonProperty("finish_reason") String finishReason) {}

    private record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {}
}
