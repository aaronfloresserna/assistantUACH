package mx.uach.luisamigo.service.ingestion;

/**
 * Estimación de tiempo y costo para ingesta.
 */
public record IngestionEstimate(
    int documentCount,
    long estimatedMinutes,
    double estimatedCostUSD,
    String breakdown
) {}
