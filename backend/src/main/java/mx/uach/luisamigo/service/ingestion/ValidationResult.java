package mx.uach.luisamigo.service.ingestion;

import java.util.List;

/**
 * Resultado de validación de dataset.
 */
public record ValidationResult(
    boolean valid,
    int documentCount,
    List<String> errors,
    List<String> warnings
) {
    public static ValidationResult valid(int documentCount) {
        return new ValidationResult(true, documentCount, List.of(), List.of());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, 0, errors, List.of());
    }
}
