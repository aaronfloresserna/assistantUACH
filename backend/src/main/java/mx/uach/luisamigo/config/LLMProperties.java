package mx.uach.luisamigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para LLM providers.
 * Mapeadas desde application.yml (luisamigo.llm.*).
 */
@Component
@ConfigurationProperties(prefix = "luisamigo.llm")
public class LLMProperties {

    private String provider; // "openai" o "anthropic"
    private OpenAIConfig openai = new OpenAIConfig();
    private AnthropicConfig anthropic = new AnthropicConfig();

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public OpenAIConfig getOpenai() { return openai; }
    public void setOpenai(OpenAIConfig openai) { this.openai = openai; }

    public AnthropicConfig getAnthropic() { return anthropic; }
    public void setAnthropic(AnthropicConfig anthropic) { this.anthropic = anthropic; }

    // Nested configuration classes
    public static class OpenAIConfig {
        private String apiKey;
        private String model = "gpt-4";
        private double temperature = 0.1;
        private int maxTokens = 2000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class AnthropicConfig {
        private String apiKey;
        private String model = "claude-3-sonnet-20240229";
        private double temperature = 0.1;
        private int maxTokens = 2000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }
}
