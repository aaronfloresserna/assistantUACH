package mx.uach.luisamigo.client.llm;

import mx.uach.luisamigo.config.LLMProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory para seleccionar el cliente LLM apropiado según configuración.
 * Permite cambiar entre OpenAI y Anthropic dinámicamente.
 */
@Component
public class LLMClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LLMClientFactory.class);

    private final LLMClient openAIClient;
    private final LLMClient anthropicClient;
    private final LLMProperties llmProperties;

    public LLMClientFactory(
        @Qualifier("openaiLLMClient") LLMClient openAIClient,
        @Qualifier("anthropicLLMClient") LLMClient anthropicClient,
        LLMProperties llmProperties
    ) {
        this.openAIClient = openAIClient;
        this.anthropicClient = anthropicClient;
        this.llmProperties = llmProperties;
    }

    /**
     * Retorna el cliente LLM según la configuración (luisamigo.llm.provider).
     *
     * @return Cliente LLM configurado
     * @throws IllegalStateException si el proveedor no está configurado o disponible
     */
    public LLMClient getClient() {
        String provider = llmProperties.getProvider();

        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("LLM provider not configured");
        }

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                if (!openAIClient.isAvailable()) {
                    throw new IllegalStateException("OpenAI client not available. Check API key configuration.");
                }
                log.debug("Using OpenAI LLM client: {}", openAIClient.getModelName());
                yield openAIClient;
            }
            case "anthropic" -> {
                if (!anthropicClient.isAvailable()) {
                    throw new IllegalStateException("Anthropic client not available. Check API key configuration.");
                }
                log.debug("Using Anthropic LLM client: {}", anthropicClient.getModelName());
                yield anthropicClient;
            }
            default -> throw new IllegalStateException("Unknown LLM provider: " + provider);
        };
    }

    /**
     * Retorna un cliente LLM específico por nombre.
     *
     * @param providerName "openai" o "anthropic"
     * @return Cliente LLM solicitado
     */
    public LLMClient getClient(String providerName) {
        return switch (providerName.toLowerCase()) {
            case "openai" -> openAIClient;
            case "anthropic" -> anthropicClient;
            default -> throw new IllegalArgumentException("Unknown provider: " + providerName);
        };
    }

    /**
     * Verifica si hay al menos un proveedor LLM disponible.
     *
     * @return true si hay un proveedor disponible
     */
    public boolean hasAvailableProvider() {
        return openAIClient.isAvailable() || anthropicClient.isAvailable();
    }
}
