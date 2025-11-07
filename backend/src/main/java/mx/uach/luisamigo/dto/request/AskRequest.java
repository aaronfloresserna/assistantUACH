package mx.uach.luisamigo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Request para consulta al asistente jurídico.
 */
public record AskRequest(
    @NotBlank(message = "La pregunta no puede estar vacía")
    String question,

    // Opcional: filtrar por materia
    String materia,  // e.g., "constitucional", "civil", "penal"

    // Opcional: nivel de semestre (1-10)
    @Min(value = 1, message = "El semestre debe ser entre 1 y 10")
    @Max(value = 10, message = "El semestre debe ser entre 1 y 10")
    Integer semesterLevel,

    // Opcional: número de documentos a recuperar (default: 5)
    @Min(value = 1, message = "topK debe ser al menos 1")
    @Max(value = 20, message = "topK no puede exceder 20")
    Integer topK
) {
    // Constructor compacto con defaults
    public AskRequest {
        if (topK == null) {
            topK = 5;
        }
    }

    // Constructor simplificado solo con pregunta
    public AskRequest(String question) {
        this(question, null, null, 5);
    }
}
