package mx.uach.luisamigo.service.ingestion;

import java.time.Duration;
import java.time.Instant;

/**
 * Resultado de un proceso de ingesta.
 * Contiene estadísticas y metadata sobre la ingesta ejecutada.
 */
public record IngestionResult(
    boolean success,
    int totalDocuments,
    int documentsProcessed,
    int documentsSkipped,
    int documentsFailed,
    Instant startTime,
    Instant endTime,
    String errorMessage
) {
    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    public static IngestionResult success(int totalDocuments, int processed, int skipped,
                                          Instant startTime, Instant endTime) {
        return new IngestionResult(
            true, totalDocuments, processed, skipped, 0, startTime, endTime, null
        );
    }

    public static IngestionResult failure(String errorMessage, Instant startTime, Instant endTime) {
        return new IngestionResult(
            false, 0, 0, 0, 0, startTime, endTime, errorMessage
        );
    }
}
