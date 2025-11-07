package mx.uach.luisamigo.client.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import mx.uach.luisamigo.config.EmbeddingProperties;
import mx.uach.luisamigo.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de EmbeddingClient para OpenAI API.
 * Usa modelos como text-embedding-3-small, text-embedding-3-large, etc.
 */
@Component
public class OpenAIEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingClient.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";
    private static final String PROVIDER_NAME = "OpenAI";
    private static final int MAX_TOKENS = 8191; // text-embedding-3-small limit
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final WebClient webClient;
    private final EmbeddingProperties.OpenAIConfig config;

    public OpenAIEmbeddingClient(EmbeddingProperties embeddingProperties, WebClient.Builder webClientBuilder) {
        this.config = embeddingProperties.getOpenai();
        this.webClient = webClientBuilder
            .baseUrl(OPENAI_API_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            log.debug("Generating embedding for text (length: {})", text.length());

            EmbeddingRequest request = new EmbeddingRequest(
                config.getModel(),
                text
            );

            EmbeddingResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .block();

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("Empty response from OpenAI", PROVIDER_NAME);
            }

            List<Double> embedding = response.data().get(0).embedding();
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }

            log.debug("Embedding generated successfully. Dimensions: {}, Tokens used: {}",
                result.length, response.usage().totalTokens());

            return result;

        } catch (WebClientResponseException e) {
            log.error("OpenAI Embeddings API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException(
                "OpenAI Embeddings API error: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new EmbeddingException(
                "Error generating embedding: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Texts list cannot be null or empty");
        }

        try {
            log.debug("Generating embeddings for {} texts", texts.size());

            EmbeddingRequest request = new EmbeddingRequest(
                config.getModel(),
                texts
            );

            EmbeddingResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS * 2)) // Más tiempo para batch
                .block();

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("Empty response from OpenAI", PROVIDER_NAME);
            }

            List<float[]> results = new ArrayList<>();
            for (EmbeddingData data : response.data()) {
                List<Double> embedding = data.embedding();
                float[] result = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                results.add(result);
            }

            log.info("Batch embeddings generated successfully. Count: {}, Tokens used: {}",
                results.size(), response.usage().totalTokens());

            return results;

        } catch (WebClientResponseException e) {
            log.error("OpenAI Embeddings API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException(
                "OpenAI Embeddings API error: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        } catch (Exception e) {
            log.error("Error generating batch embeddings", e);
            throw new EmbeddingException(
                "Error generating batch embeddings: " + e.getMessage(),
                PROVIDER_NAME,
                e
            );
        }
    }

    @Override
    public int getDimensions() {
        return config.getDimensions();
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public int getMaxTokens() {
        return MAX_TOKENS;
    }

    // DTOs para OpenAI Embeddings API
    private record EmbeddingRequest(String model, Object input) {}

    private record EmbeddingResponse(
        String object,
        List<EmbeddingData> data,
        String model,
        Usage usage
    ) {}

    private record EmbeddingData(
        String object,
        List<Double> embedding,
        int index
    ) {}

    private record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {}
}
