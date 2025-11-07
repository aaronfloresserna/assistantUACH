package mx.uach.luisamigo.service.ingestion;

import mx.uach.luisamigo.client.embedding.EmbeddingClient;
import mx.uach.luisamigo.domain.LegalDocument;
import mx.uach.luisamigo.service.vectorstore.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio de ingesta.
 * Orquesta el pipeline completo de carga de datos desde Hugging Face.
 */
@Service
public class IngestionServiceImpl implements IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionServiceImpl.class);
    private static final String BARCENAS_SOURCE = "Barcenas-Juridico-Mexicano-Dataset";
    private static final String BARCENAS_URL = "https://huggingface.co/datasets/Danielbrdz/Barcenas-Juridico-Mexicano-Dataset";

    private final DatasetLoader datasetLoader;
    private final TextNormalizer textNormalizer;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreService vectorStoreService;

    public IngestionServiceImpl(
            DatasetLoader datasetLoader,
            TextNormalizer textNormalizer,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            VectorStoreService vectorStoreService) {
        this.datasetLoader = datasetLoader;
        this.textNormalizer = textNormalizer;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    @Transactional
    public IngestionResult ingestBarcerasDataset(IngestionConfig config) {
        Instant startTime = Instant.now();
        log.info("Starting ingestion of Barcenas dataset with config: batchSize={}, maxChunkSize={}, chunkOverlap={}",
            config.getBatchSize(), config.getMaxChunkSize(), config.getChunkOverlap());

        try {
            // 1. Cargar dataset desde Hugging Face
            List<DatasetLoader.BarcerasDatasetEntry> entries = datasetLoader.loadBarcerasDataset();
            int totalDocuments = entries.size();
            log.info("Loaded {} entries from Barcenas dataset", totalDocuments);

            if (entries.isEmpty()) {
                return IngestionResult.failure("No entries found in dataset", startTime, Instant.now());
            }

            // 2. Verificar si debemos limpiar documentos existentes
            if (config.isOverwrite()) {
                log.info("Overwrite mode enabled, deleting existing documents from source: {}", BARCENAS_SOURCE);
                int deleted = vectorStoreService.deleteBySource(BARCENAS_SOURCE);
                log.info("Deleted {} existing documents", deleted);
            }

            // 3. Procesar en batches
            int processed = 0;
            int skipped = 0;
            int failed = 0;

            List<VectorStoreService.DocumentWithEmbedding> batch = new ArrayList<>();

            for (int i = 0; i < entries.size(); i++) {
                DatasetLoader.BarcerasDatasetEntry entry = entries.get(i);

                try {
                    // Generar ID único
                    String externalId = generateExternalId(entry, i);

                    // Verificar si ya existe (si skipExisting está activado)
                    if (config.isSkipExisting() && documentExists(externalId)) {
                        skipped++;
                        log.debug("Skipping existing document: {}", externalId);
                        continue;
                    }

                    // Normalizar textos
                    String normalizedQuestion = textNormalizer.normalize(entry.question());
                    String normalizedAnswer = textNormalizer.normalize(entry.answer());

                    // Extraer referencia legal si existe
                    String lawReference = textNormalizer.extractLegalReference(normalizedAnswer);

                    // Crear documento
                    LegalDocument document = LegalDocument.builder()
                        .externalId(externalId)
                        .question(normalizedQuestion)
                        .answer(normalizedAnswer)
                        .lawReference(lawReference)
                        .materia(inferMateria(entry))
                        .source(BARCENAS_SOURCE)
                        .sourceUrl(BARCENAS_URL)
                        .build();

                    // Generar texto para embedding (pregunta + respuesta)
                    String embeddingText = normalizedQuestion + " " + normalizedAnswer;

                    // Verificar si necesita chunking
                    if (chunkingService.needsChunking(embeddingText, config.getMaxChunkSize())) {
                        log.debug("Document {} requires chunking", externalId);
                        List<String> chunks = chunkingService.chunkText(
                            embeddingText,
                            config.getMaxChunkSize(),
                            config.getChunkOverlap()
                        );

                        // Por ahora usar solo el primer chunk
                        // TODO: En futuro, considerar estrategia multi-chunk
                        embeddingText = chunks.get(0);
                    }

                    // Generar embedding
                    float[] embedding = embeddingClient.generateEmbedding(embeddingText);

                    // Agregar a batch
                    batch.add(new VectorStoreService.DocumentWithEmbedding(document, embedding));

                    // Persistir batch cuando alcance el tamaño configurado
                    if (batch.size() >= config.getBatchSize()) {
                        int stored = vectorStoreService.storeBatch(batch);
                        processed += stored;
                        log.info("Progress: {}/{} documents processed", processed, totalDocuments);
                        batch.clear();
                    }

                } catch (Exception e) {
                    failed++;
                    log.error("Error processing entry {}: {}", i, e.getMessage(), e);
                }
            }

            // Persistir batch restante
            if (!batch.isEmpty()) {
                int stored = vectorStoreService.storeBatch(batch);
                processed += stored;
                batch.clear();
            }

            Instant endTime = Instant.now();
            log.info("Ingestion completed: processed={}, skipped={}, failed={}, total={}",
                processed, skipped, failed, totalDocuments);

            return IngestionResult.success(totalDocuments, processed, skipped, startTime, endTime);

        } catch (Exception e) {
            log.error("Fatal error during ingestion", e);
            return IngestionResult.failure(e.getMessage(), startTime, Instant.now());
        }
    }

    @Override
    public ValidationResult validateDataset() {
        log.info("Validating Barcenas dataset from Hugging Face");

        try {
            List<DatasetLoader.BarcerasDatasetEntry> entries = datasetLoader.loadBarcerasDataset();

            if (entries.isEmpty()) {
                return ValidationResult.invalid(List.of("Dataset is empty"));
            }

            List<String> warnings = new ArrayList<>();

            // Validar estructura de las entradas
            int invalidEntries = 0;
            for (int i = 0; i < Math.min(entries.size(), 100); i++) {
                DatasetLoader.BarcerasDatasetEntry entry = entries.get(i);
                if (entry.question() == null || entry.question().isBlank()) {
                    invalidEntries++;
                }
                if (entry.answer() == null || entry.answer().isBlank()) {
                    invalidEntries++;
                }
            }

            if (invalidEntries > 0) {
                warnings.add(String.format("Found %d invalid entries in sample of 100", invalidEntries));
            }

            log.info("Dataset validation successful: {} entries found", entries.size());
            return new ValidationResult(true, entries.size(), List.of(), warnings);

        } catch (Exception e) {
            log.error("Error validating dataset", e);
            return ValidationResult.invalid(List.of(e.getMessage()));
        }
    }

    @Override
    public IngestionEstimate estimateIngestion() {
        log.info("Estimating ingestion time and cost");

        try {
            List<DatasetLoader.BarcerasDatasetEntry> entries = datasetLoader.loadBarcerasDataset();
            int documentCount = entries.size();

            // Estimaciones basadas en experiencia con APIs de embeddings
            // OpenAI text-embedding-3-small: ~$0.00002 per 1K tokens
            // Promedio de 200 tokens por documento (pregunta + respuesta)
            double tokensPerDocument = 200.0;
            double totalTokens = documentCount * tokensPerDocument;
            double costPer1kTokens = 0.00002;
            double estimatedCost = (totalTokens / 1000.0) * costPer1kTokens;

            // Tiempo estimado: ~50 documentos por minuto (con rate limits de API)
            long estimatedMinutes = Math.max(1, documentCount / 50);

            String breakdown = String.format(
                "Documents: %d\n" +
                "Estimated tokens: %.0f\n" +
                "Processing rate: ~50 docs/min\n" +
                "API: OpenAI text-embedding-3-small ($%.5f per 1K tokens)",
                documentCount, totalTokens, costPer1kTokens
            );

            log.info("Estimation complete: {} documents, ~{} minutes, ~${}",
                documentCount, estimatedMinutes, String.format("%.4f", estimatedCost));

            return new IngestionEstimate(documentCount, estimatedMinutes, estimatedCost, breakdown);

        } catch (Exception e) {
            log.error("Error estimating ingestion", e);
            return new IngestionEstimate(0, 0, 0.0, "Error: " + e.getMessage());
        }
    }

    /**
     * Genera un ID externo único para un documento.
     */
    private String generateExternalId(DatasetLoader.BarcerasDatasetEntry entry, int index) {
        // Usar hash del contenido para generar ID determinístico
        String content = entry.question() + "|" + entry.answer();
        int hash = content.hashCode();
        return String.format("barcenas-%d-%08x", index, hash);
    }

    /**
     * Verifica si un documento con el external_id ya existe.
     * Por ahora retorna false, se puede implementar una consulta al repositorio si se necesita.
     */
    private boolean documentExists(String externalId) {
        // TODO: Implementar consulta al repositorio si se necesita
        // Por ahora confiamos en la constraint UNIQUE de la base de datos
        return false;
    }

    /**
     * Intenta inferir la materia del documento basado en el contenido.
     */
    private String inferMateria(DatasetLoader.BarcerasDatasetEntry entry) {
        String text = (entry.question() + " " + entry.answer()).toLowerCase();

        // Patrones simples para inferir materia
        if (text.contains("constitucional") || text.contains("constitución")) {
            return "Constitucional";
        }
        if (text.contains("penal") || text.contains("delito")) {
            return "Penal";
        }
        if (text.contains("civil") || text.contains("contrato")) {
            return "Civil";
        }
        if (text.contains("laboral") || text.contains("trabajo")) {
            return "Laboral";
        }
        if (text.contains("mercantil") || text.contains("comercio")) {
            return "Mercantil";
        }
        if (text.contains("administrativo") || text.contains("administración pública")) {
            return "Administrativo";
        }

        // Por defecto
        return "General";
    }
}
