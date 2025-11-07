package mx.uach.luisamigo.service.rag;

import mx.uach.luisamigo.dto.request.AskRequest;
import mx.uach.luisamigo.dto.response.AskResponse;

/**
 * Servicio principal del pipeline RAG (Retrieval-Augmented Generation).
 * Orquesta el flujo completo: búsqueda vectorial, construcción de prompt,
 * generación de respuesta y formateo con fuentes.
 */
public interface RAGService {

    /**
     * Procesa una pregunta del usuario siguiendo el pipeline RAG completo.
     *
     * Pipeline:
     * 1. Genera embedding de la pregunta
     * 2. Busca documentos similares en vector store (filtrado por materia si aplica)
     * 3. Construye prompt con contexto recuperado
     * 4. Llama al LLM con el prompt
     * 5. Formatea respuesta con citas
     * 6. Valida referencias legales
     *
     * @param request Pregunta y filtros opcionales (materia, semester_level)
     * @return Respuesta con explicación y fuentes citadas
     * @throws InsufficientContextException si no hay contexto suficiente
     * @throws LLMProviderException si falla la llamada al LLM
     */
    AskResponse ask(AskRequest request);

    /**
     * Valida que el sistema RAG esté completamente funcional.
     * Verifica disponibilidad de LLM, embedding client y vector store.
     *
     * @return true si todos los componentes están operativos
     */
    boolean isHealthy();
}
