package mx.uach.luisamigo.client.llm;

/**
 * Configuración para llamadas a LLM.
 * Encapsula parámetros como temperatura, max tokens, etc.
 */
public class LLMConfig {

    private final double temperature;
    private final int maxTokens;
    private final double topP;
    private final int timeoutSeconds;
    private final boolean stream;

    private LLMConfig(Builder builder) {
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.topP = builder.topP;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.stream = builder.stream;
    }

    public static LLMConfig defaultConfig() {
        return builder()
            .temperature(0.1)  // Baja temperatura para respuestas más determinísticas
            .maxTokens(2000)
            .topP(1.0)
            .timeoutSeconds(30)
            .stream(false)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }
    public double getTopP() { return topP; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isStream() { return stream; }

    public static class Builder {
        private double temperature = 0.1;
        private int maxTokens = 2000;
        private double topP = 1.0;
        private int timeoutSeconds = 30;
        private boolean stream = false;

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(double topP) {
            this.topP = topP;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public LLMConfig build() {
            return new LLMConfig(this);
        }
    }
}
