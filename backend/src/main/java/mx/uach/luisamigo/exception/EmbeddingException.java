package mx.uach.luisamigo.exception;

/**
 * Excepción lanzada cuando hay errores al generar embeddings.
 */
public class EmbeddingException extends RuntimeException {

    private final String provider;

    public EmbeddingException(String message, String provider) {
        super(message);
        this.provider = provider;
    }

    public EmbeddingException(String message, String provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
