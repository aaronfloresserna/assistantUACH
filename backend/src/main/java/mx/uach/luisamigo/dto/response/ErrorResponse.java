package mx.uach.luisamigo.dto.response;

import java.time.Instant;

/**
 * Response estándar de error.
 */
public record ErrorResponse(
    String error,
    String message,
    int status,
    Instant timestamp
) {
    public ErrorResponse(String error, String message, int status) {
        this(error, message, status, Instant.now());
    }
}
