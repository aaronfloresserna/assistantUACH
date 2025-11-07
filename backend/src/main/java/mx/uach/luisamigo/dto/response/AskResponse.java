package mx.uach.luisamigo.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response con respuesta del asistente jurídico y fuentes citadas.
 */
public record AskResponse(
    String answer,
    List<SourceReference> sources,
    ResponseMetadata metadata,
    String disclaimer
) {
    // Constructor con disclaimer por defecto
    public AskResponse(String answer, List<SourceReference> sources, ResponseMetadata metadata) {
        this(answer, sources, metadata,
            "Esto es material académico y no constituye asesoría jurídica profesional.");
    }

    /**
     * Metadata adicional de la respuesta.
     */
    public record ResponseMetadata(
        int documentsRetrieved,
        String materia,
        Instant timestamp,
        long processingTimeMs
    ) {}
}
