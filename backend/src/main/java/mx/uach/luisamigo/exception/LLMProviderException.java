package mx.uach.luisamigo.exception;

/**
 * Excepción lanzada cuando hay errores al comunicarse con proveedores LLM.
 */
public class LLMProviderException extends RuntimeException {

    private final String provider;

    public LLMProviderException(String message, String provider) {
        super(message);
        this.provider = provider;
    }

    public LLMProviderException(String message, String provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
