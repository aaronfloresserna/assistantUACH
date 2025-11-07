package mx.uach.luisamigo.service.rag;

import mx.uach.luisamigo.client.embedding.EmbeddingClient;
import mx.uach.luisamigo.client.embedding.EmbeddingClientFactory;
import mx.uach.luisamigo.client.llm.LLMClient;
import mx.uach.luisamigo.client.llm.LLMClientFactory;
import mx.uach.luisamigo.client.llm.LLMConfig;
import mx.uach.luisamigo.domain.DocumentEmbedding;
import mx.uach.luisamigo.domain.LegalDocument;
import mx.uach.luisamigo.dto.request.AskRequest;
import mx.uach.luisamigo.dto.response.AskResponse;
import mx.uach.luisamigo.exception.InsufficientContextException;
import mx.uach.luisamigo.service.vectorstore.SearchFilters;
import mx.uach.luisamigo.service.vectorstore.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio RAG (Retrieval-Augmented Generation).
 * Orquesta el flujo completo: embedding, búsqueda, prompt, generación y formateo.
 */
@Service
public class RAGServiceImpl implements RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGServiceImpl.class);
    private static final int MIN_DOCUMENTS_FOR_RESPONSE = 1;

    private final VectorStoreService vectorStoreService;
    private final LLMClientFactory llmClientFactory;
    private final EmbeddingClientFactory embeddingClientFactory;
    private final PromptBuilder promptBuilder;
    private final ResponseFormatter responseFormatter;
    private final HallucinationValidator hallucinationValidator;

    public RAGServiceImpl(
        VectorStoreService vectorStoreService,
        LLMClientFactory llmClientFactory,
        EmbeddingClientFactory embeddingClientFactory,
        PromptBuilder promptBuilder,
        ResponseFormatter responseFormatter,
        HallucinationValidator hallucinationValidator
    ) {
        this.vectorStoreService = vectorStoreService;
        this.llmClientFactory = llmClientFactory;
        this.embeddingClientFactory = embeddingClientFactory;
        this.promptBuilder = promptBuilder;
        this.responseFormatter = responseFormatter;
        this.hallucinationValidator = hallucinationValidator;
    }

    @Override
    public AskResponse ask(AskRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing RAG request. Question: '{}'", request.question());

        try {
            // Paso 1: Generar embedding de la pregunta
            log.debug("Step 1: Generating query embedding");
            EmbeddingClient embeddingClient = embeddingClientFactory.getClient();
            float[] queryEmbedding = embeddingClient.generateEmbedding(request.question());
            log.debug("Query embedding generated. Dimensions: {}", queryEmbedding.length);

            // Paso 2: Buscar documentos similares en vector store
            log.debug("Step 2: Searching for similar documents (topK={})", request.topK());
            SearchFilters filters = buildSearchFilters(request);
            List<DocumentEmbedding> retrievedDocuments = vectorStoreService.findSimilar(
                queryEmbedding,
                request.topK(),
                filters
            );

            // Verificar si hay documentos suficientes
            if (retrievedDocuments.size() < MIN_DOCUMENTS_FOR_RESPONSE) {
                log.warn("Insufficient context: only {} documents found", retrievedDocuments.size());
                return handleInsufficientContext(request, startTime);
            }

            log.info("Retrieved {} documents from vector store", retrievedDocuments.size());

            // Paso 3: Construir prompt con contexto
            log.debug("Step 3: Building prompt with context");
            String prompt = promptBuilder.buildPrompt(request.question(), retrievedDocuments);

            // Paso 4: Llamar al LLM
            log.debug("Step 4: Calling LLM");
            LLMClient llmClient = llmClientFactory.getClient();
            LLMConfig llmConfig = LLMConfig.defaultConfig();
            String llmAnswer = llmClient.generateResponse(prompt, llmConfig);
            log.debug("LLM response received. Length: {} characters", llmAnswer.length());

            // Paso 5: Validar respuesta (anti-hallucination)
            log.debug("Step 5: Validating response for hallucinations");
            List<LegalDocument> contextDocuments = retrievedDocuments.stream()
                .map(DocumentEmbedding::getDocument)
                .collect(Collectors.toList());

            HallucinationValidator.ValidationResult validation =
                hallucinationValidator.validate(llmAnswer, contextDocuments);

            if (validation.hasWarnings()) {
                log.warn("Validation warnings detected: {}", validation.warnings());
                // En producción, podrías rechazar la respuesta o marcarla con warnings
            }

            // Paso 6: Formatear respuesta con fuentes
            log.debug("Step 6: Formatting response");
            long processingTime = System.currentTimeMillis() - startTime;
            AskResponse response = responseFormatter.formatResponse(
                llmAnswer,
                retrievedDocuments,
                request.materia(),
                processingTime
            );

            log.info("RAG request completed successfully. Processing time: {}ms", processingTime);
            return response;

        } catch (InsufficientContextException e) {
            log.warn("Insufficient context for question: {}", request.question());
            return handleInsufficientContext(request, startTime);
        } catch (Exception e) {
            log.error("Error processing RAG request", e);
            throw new RuntimeException("Error processing request: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            boolean llmAvailable = llmClientFactory.hasAvailableProvider();
            boolean embeddingAvailable = embeddingClientFactory.hasAvailableProvider();
            long documentCount = vectorStoreService.countDocuments();

            boolean healthy = llmAvailable && embeddingAvailable && documentCount > 0;

            log.debug("Health check: LLM={}, Embedding={}, Documents={}",
                llmAvailable, embeddingAvailable, documentCount);

            return healthy;

        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Construye los filtros de búsqueda desde el request.
     */
    private SearchFilters buildSearchFilters(AskRequest request) {
        SearchFilters.Builder builder = SearchFilters.builder();

        if (request.materia() != null && !request.materia().isBlank()) {
            builder.materia(request.materia());
        }

        if (request.semesterLevel() != null) {
            builder.semesterLevel(request.semesterLevel());
        }

        return builder.build();
    }

    /**
     * Maneja el caso de contexto insuficiente.
     */
    private AskResponse handleInsufficientContext(AskRequest request, long startTime) {
        log.debug("Building insufficient context response");

        String prompt = promptBuilder.buildInsufficientContextPrompt(request.question());

        LLMClient llmClient = llmClientFactory.getClient();
        LLMConfig llmConfig = LLMConfig.defaultConfig();
        String llmAnswer = llmClient.generateResponse(prompt, llmConfig);

        long processingTime = System.currentTimeMillis() - startTime;

        return responseFormatter.formatInsufficientContextResponse(
            llmAnswer,
            request.materia(),
            processingTime
        );
    }
}
