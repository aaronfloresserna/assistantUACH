package mx.uach.luisamigo.service.ingestion;

/**
 * Servicio principal para ingesta de datos jurídicos.
 * Orquesta el pipeline completo: carga, normalización, chunking, embeddings y persistencia.
 */
public interface IngestionService {

    /**
     * Ingesta el dataset Barcenas-Juridico-Mexicano desde Hugging Face.
     *
     * Pipeline:
     * 1. DatasetLoader descarga/carga el dataset
     * 2. TextNormalizer limpia y normaliza textos
     * 3. ChunkingService divide textos largos si es necesario
     * 4. EmbeddingClient genera vectores
     * 5. VectorStoreService persiste en PostgreSQL
     *
     * @param config Configuración de ingesta
     * @return Resultado con estadísticas de la ingesta
     */
    IngestionResult ingestBarcerasDataset(IngestionConfig config);

    /**
     * Valida el estado del dataset sin ingestar.
     * Útil para verificar que el dataset esté accesible y tenga formato correcto.
     *
     * @return Resultado de validación con estadísticas del dataset
     */
    ValidationResult validateDataset();

    /**
     * Estima el tiempo y costo de ingestar el dataset completo.
     *
     * @return Estimación de tiempo (minutos) y costo (USD)
     */
    IngestionEstimate estimateIngestion();
}
