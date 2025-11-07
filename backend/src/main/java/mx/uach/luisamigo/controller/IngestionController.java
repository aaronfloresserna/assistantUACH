package mx.uach.luisamigo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import mx.uach.luisamigo.service.ingestion.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones de ingesta de datos.
 * Endpoints para cargar el dataset Barcenas desde Hugging Face.
 */
@RestController
@RequestMapping("/ingest")
@Tag(name = "Ingestion", description = "Endpoints para ingesta de datos jurídicos")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Inicia el proceso de ingesta del dataset Barcenas.
     * POST /api/ingest
     */
    @PostMapping
    @Operation(
        summary = "Ingestar dataset Barcenas",
        description = "Carga el dataset Barcenas-Juridico-Mexicano desde Hugging Face y genera embeddings"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Ingesta completada exitosamente",
        content = @Content(schema = @Schema(implementation = IngestionResult.class))
    )
    public ResponseEntity<IngestionResult> ingestDataset(
            @RequestBody(required = false) IngestionConfigRequest configRequest) {

        log.info("Received ingestion request");

        // Construir configuración
        IngestionConfig config = buildConfig(configRequest);

        // Ejecutar ingesta
        IngestionResult result = ingestionService.ingestBarcerasDataset(config);

        if (result.success()) {
            log.info("Ingestion completed successfully: {} documents processed", result.documentsProcessed());
            return ResponseEntity.ok(result);
        } else {
            log.error("Ingestion failed: {}", result.errorMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Valida que el dataset esté accesible y tenga formato correcto.
     * GET /api/ingest/validate
     */
    @GetMapping("/validate")
    @Operation(
        summary = "Validar dataset",
        description = "Verifica que el dataset Barcenas esté accesible desde Hugging Face"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Validación completada",
        content = @Content(schema = @Schema(implementation = ValidationResult.class))
    )
    public ResponseEntity<ValidationResult> validateDataset() {
        log.info("Received dataset validation request");

        ValidationResult result = ingestionService.validateDataset();

        if (result.valid()) {
            log.info("Dataset validation successful: {} documents found", result.documentCount());
            return ResponseEntity.ok(result);
        } else {
            log.warn("Dataset validation failed: {}", result.errors());
            return ResponseEntity.status(400).body(result);
        }
    }

    /**
     * Estima el tiempo y costo de ingestar el dataset completo.
     * GET /api/ingest/estimate
     */
    @GetMapping("/estimate")
    @Operation(
        summary = "Estimar ingesta",
        description = "Calcula el tiempo estimado y costo de ingestar el dataset completo"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Estimación completada",
        content = @Content(schema = @Schema(implementation = IngestionEstimate.class))
    )
    public ResponseEntity<IngestionEstimate> estimateIngestion() {
        log.info("Received ingestion estimation request");

        IngestionEstimate estimate = ingestionService.estimateIngestion();

        log.info("Estimation completed: {} documents, ~{} minutes, ~${} USD",
            estimate.documentCount(), estimate.estimatedMinutes(), estimate.estimatedCostUSD());

        return ResponseEntity.ok(estimate);
    }

    /**
     * Construye IngestionConfig desde el request o usa valores por defecto.
     */
    private IngestionConfig buildConfig(IngestionConfigRequest request) {
        if (request == null) {
            return IngestionConfig.defaultConfig();
        }

        IngestionConfig.Builder builder = IngestionConfig.builder();

        if (request.batchSize() != null) {
            builder.batchSize(request.batchSize());
        }
        if (request.overwrite() != null) {
            builder.overwrite(request.overwrite());
        }
        if (request.skipExisting() != null) {
            builder.skipExisting(request.skipExisting());
        }
        if (request.maxChunkSize() != null) {
            builder.maxChunkSize(request.maxChunkSize());
        }
        if (request.chunkOverlap() != null) {
            builder.chunkOverlap(request.chunkOverlap());
        }

        return builder.build();
    }

    /**
     * DTO para configuración de ingesta vía REST.
     */
    public record IngestionConfigRequest(
        @Schema(description = "Número de documentos a procesar por batch", example = "50")
        Integer batchSize,

        @Schema(description = "Sobrescribir documentos existentes", example = "false")
        Boolean overwrite,

        @Schema(description = "Saltar documentos existentes", example = "true")
        Boolean skipExisting,

        @Schema(description = "Tamaño máximo de chunk en caracteres", example = "1000")
        Integer maxChunkSize,

        @Schema(description = "Overlap entre chunks en caracteres", example = "100")
        Integer chunkOverlap
    ) {}
}
