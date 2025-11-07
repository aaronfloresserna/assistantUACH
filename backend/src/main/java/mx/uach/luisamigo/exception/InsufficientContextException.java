package mx.uach.luisamigo.exception;

/**
 * Excepción lanzada cuando no hay contexto suficiente para responder una pregunta.
 */
public class InsufficientContextException extends RuntimeException {

    private final String question;

    public InsufficientContextException(String message, String question) {
        super(message);
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
}
