package mx.uach.luisamigo.service.ingestion;

/**
 * Configuración para proceso de ingesta.
 */
public class IngestionConfig {

    private final int batchSize;
    private final boolean overwrite;
    private final boolean skipExisting;
    private final int maxChunkSize;
    private final int chunkOverlap;

    private IngestionConfig(Builder builder) {
        this.batchSize = builder.batchSize;
        this.overwrite = builder.overwrite;
        this.skipExisting = builder.skipExisting;
        this.maxChunkSize = builder.maxChunkSize;
        this.chunkOverlap = builder.chunkOverlap;
    }

    public static IngestionConfig defaultConfig() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public int getBatchSize() { return batchSize; }
    public boolean isOverwrite() { return overwrite; }
    public boolean isSkipExisting() { return skipExisting; }
    public int getMaxChunkSize() { return maxChunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }

    public static class Builder {
        private int batchSize = 50;
        private boolean overwrite = false;
        private boolean skipExisting = true;
        private int maxChunkSize = 1000;
        private int chunkOverlap = 100;

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public Builder skipExisting(boolean skipExisting) {
            this.skipExisting = skipExisting;
            return this;
        }

        public Builder maxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        public Builder chunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public IngestionConfig build() {
            return new IngestionConfig(this);
        }
    }
}
