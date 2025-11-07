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
 * Implementación de LLMClient para Anthropic API (Claude).
 */
@Component("anthropicLLMClient")
public class AnthropicClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String PROVIDER_NAME = "Anthropic";

    private final WebClient webClient;
    private final LLMProperties.AnthropicConfig config;

    public AnthropicClient(LLMProperties llmProperties, WebClient.Builder webClientBuilder) {
        this.config = llmProperties.getAnthropic();
        this.webClient = webClientBuilder
            .baseUrl(ANTHROPIC_API_URL)
            .defaultHeader("x-api-key", config.getApiKey())
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public String generateResponse(String prompt, LLMConfig llmConfig) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        try {
            log.debug("Calling Anthropic API with model: {}", config.getModel());

            AnthropicRequest request = new AnthropicRequest(
                config.getModel(),
                List.of(new Message("user", prompt)),
                llmConfig.getMaxTokens(),
                llmConfig.getTemperature()
            );

            AnthropicResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnthropicResponse.class)
                .timeout(Duration.ofSeconds(llmConfig.getTimeoutSeconds()))
                .block();

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new LLMProviderException("Empty response from Anthropic", PROVIDER_NAME);
            }

            String content = response.content().get(0).text();
            log.info("Anthropic API call successful. Tokens used: {} (input: {}, output: {})",
                response.usage().inputTokens() + response.usage().outputTokens(),
                response.usage().inputTokens(),
                response.usage().outputTokens());

            return content;

        } catch (WebClientResponseException e) {
            log.error("Anthropic API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LLMProviderException(
                "Anthropic API error: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        } catch (Exception e) {
            log.error("Error calling Anthropic API", e);
            throw new LLMProviderException(
                "Error calling Anthropic: " + e.getMessage(),
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
        // Costos aproximados para Claude 3
        // Claude 3 Sonnet: ~$0.003 per 1K input tokens, ~$0.015 per 1K output tokens
        // Claude 3 Opus: ~$0.015 per 1K input tokens, ~$0.075 per 1K output tokens
        // Promedio: ~$0.009 per 1K tokens para Sonnet
        if (config.getModel().contains("opus")) {
            return (estimatedTokens / 1000.0) * 0.045;
        } else {
            return (estimatedTokens / 1000.0) * 0.009;
        }
    }

    // DTOs para Anthropic API
    private record AnthropicRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") int maxTokens,
        double temperature
    ) {}

    private record Message(String role, String content) {}

    private record AnthropicResponse(
        String id,
        String type,
        String role,
        List<Content> content,
        String model,
        @JsonProperty("stop_reason") String stopReason,
        @JsonProperty("stop_sequence") String stopSequence,
        Usage usage
    ) {}

    private record Content(String type, String text) {}

    private record Usage(
        @JsonProperty("input_tokens") int inputTokens,
        @JsonProperty("output_tokens") int outputTokens
    ) {}
}
