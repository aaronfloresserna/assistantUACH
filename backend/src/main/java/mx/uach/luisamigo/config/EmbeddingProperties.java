package mx.uach.luisamigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para Embedding providers.
 * Mapeadas desde application.yml (luisamigo.embedding.*).
 */
@Component
@ConfigurationProperties(prefix = "luisamigo.embedding")
public class EmbeddingProperties {

    private String provider; // "openai"
    private OpenAIConfig openai = new OpenAIConfig();

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public OpenAIConfig getOpenai() { return openai; }
    public void setOpenai(OpenAIConfig openai) { this.openai = openai; }

    // Nested configuration class
    public static class OpenAIConfig {
        private String apiKey;
        private String model = "text-embedding-3-small";
        private int dimensions = 1536;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }
}
