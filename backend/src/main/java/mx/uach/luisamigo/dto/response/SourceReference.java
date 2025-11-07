package mx.uach.luisamigo.dto.response;

/**
 * Referencia a una fuente jurídica citada en la respuesta.
 */
public record SourceReference(
    Long documentId,
    String text,
    String lawReference,  // e.g., "Artículo 123 CPEUM"
    String source,        // e.g., "Barcenas-Juridico-Mexicano-Dataset"
    Double similarityScore
) {}
