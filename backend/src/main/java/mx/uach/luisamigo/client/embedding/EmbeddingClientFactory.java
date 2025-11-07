package mx.uach.luisamigo.client.embedding;

import mx.uach.luisamigo.config.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory para seleccionar el cliente de embeddings apropiado según configuración.
 */
@Component
public class EmbeddingClientFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientFactory.class);

    private final EmbeddingClient openAIEmbeddingClient;
    private final EmbeddingProperties embeddingProperties;

    public EmbeddingClientFactory(
        OpenAIEmbeddingClient openAIEmbeddingClient,
        EmbeddingProperties embeddingProperties
    ) {
        this.openAIEmbeddingClient = openAIEmbeddingClient;
        this.embeddingProperties = embeddingProperties;
    }

    /**
     * Retorna el cliente de embeddings según la configuración (luisamigo.embedding.provider).
     *
     * @return Cliente de embeddings configurado
     * @throws IllegalStateException si el proveedor no está configurado o disponible
     */
    public EmbeddingClient getClient() {
        String provider = embeddingProperties.getProvider();

        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("Embedding provider not configured");
        }

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                if (!openAIEmbeddingClient.isAvailable()) {
                    throw new IllegalStateException("OpenAI embedding client not available. Check API key configuration.");
                }
                log.debug("Using OpenAI embedding client: {}", openAIEmbeddingClient.getModelName());
                yield openAIEmbeddingClient;
            }
            default -> throw new IllegalStateException("Unknown embedding provider: " + provider);
        };
    }

    /**
     * Verifica si hay un proveedor de embeddings disponible.
     *
     * @return true si hay un proveedor disponible
     */
    public boolean hasAvailableProvider() {
        return openAIEmbeddingClient.isAvailable();
    }
}
