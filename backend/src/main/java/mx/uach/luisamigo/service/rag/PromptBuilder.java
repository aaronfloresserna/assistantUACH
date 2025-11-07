package mx.uach.luisamigo.service.rag;

import mx.uach.luisamigo.domain.DocumentEmbedding;
import mx.uach.luisamigo.domain.LegalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Construye el prompt para el LLM usando el template definido en la documentación.
 * Incluye el contexto recuperado y la pregunta del usuario.
 */
@Component
public class PromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilder.class);

    private static final String SYSTEM_PROMPT = """
        Eres un asistente jurídico académico para estudiantes de la Licenciatura en Derecho de la Universidad Autónoma de Chihuahua (UACH).

        ## Tu Rol

        Tu propósito es ayudar a estudiantes a comprender el derecho mexicano de forma clara, pedagógica y fundamentada. Actúas como un profesor paciente que explica conceptos jurídicos con claridad.

        ## Reglas ESTRICTAS

        1. **USAR EXCLUSIVAMENTE EL CONTEXTO PROPORCIONADO**
           - Solo responde basándote en la información del contexto jurídico que se te proporciona
           - NUNCA inventes artículos, números de tesis, o jurisprudencias
           - Si el contexto no contiene información suficiente, debes decir explícitamente:
             "Con la información disponible no puedo fundamentar con precisión jurídica esta respuesta."

        2. **SIEMPRE CITAR FUNDAMENTOS**
           - Cuando el contexto incluya leyes, artículos, o criterios, cítalos explícitamente
           - Formato: "De acuerdo con el [Ley/Artículo/Criterio]..."
           - Distingue claramente entre:
             * Norma jurídica (ley, reglamento)
             * Criterio jurisprudencial (tesis, jurisprudencia)
             * Opinión doctrinal

        3. **ESTILO PEDAGÓGICO**
           - Lenguaje claro y directo, apropiado para estudiantes de 18-25 años
           - Estructura tu respuesta en:
             a) Resumen corto (2-3 líneas)
             b) Explicación extendida (fundamentada en contexto)
             c) Ejemplo práctico (cuando sea posible)
           - Evita lenguaje excesivamente técnico sin explicarlo
           - NO uses lenguaje de "coach" o "vendedor"

        4. **DESCARGO DE RESPONSABILIDAD**
           - Siempre incluye al final:
             "Esto es material académico y no constituye asesoría jurídica profesional."
        """;

    /**
     * Construye el prompt completo para el LLM.
     *
     * @param question Pregunta del usuario
     * @param retrievedDocuments Documentos recuperados del vector store
     * @return Prompt completo listo para enviar al LLM
     */
    public String buildPrompt(String question, List<DocumentEmbedding> retrievedDocuments) {
        log.debug("Building prompt with {} retrieved documents", retrievedDocuments.size());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(SYSTEM_PROMPT);
        promptBuilder.append("\n\n");

        // Construir contexto
        promptBuilder.append("## Contexto Proporcionado\n\n");
        promptBuilder.append("A continuación se te proporciona el contexto jurídico relevante recuperado de nuestra base de conocimiento:\n\n");
        promptBuilder.append("---\n");

        int contextNumber = 1;
        for (DocumentEmbedding embedding : retrievedDocuments) {
            LegalDocument doc = embedding.getDocument();

            promptBuilder.append("CONTEXTO ").append(contextNumber).append(":\n");
            promptBuilder.append("[Fuente: ").append(doc.getSource()).append("]\n");

            if (doc.getLawReference() != null && !doc.getLawReference().isBlank()) {
                promptBuilder.append("Referencia Legal: ").append(doc.getLawReference()).append("\n");
            }

            if (doc.getMateria() != null && !doc.getMateria().isBlank()) {
                promptBuilder.append("Materia: ").append(doc.getMateria()).append("\n");
            }

            promptBuilder.append("\n");

            if (doc.getQuestion() != null && !doc.getQuestion().isBlank()) {
                promptBuilder.append("Pregunta Original: ").append(doc.getQuestion()).append("\n");
            }

            promptBuilder.append("Respuesta: ").append(doc.getAnswer()).append("\n");
            promptBuilder.append("\n---\n");

            contextNumber++;
        }

        // Agregar pregunta del usuario
        promptBuilder.append("\n## Pregunta del Estudiante\n\n");
        promptBuilder.append(question);
        promptBuilder.append("\n\n");

        // Instrucción final
        promptBuilder.append("## Tu Respuesta\n\n");
        promptBuilder.append("Proporciona una respuesta clara, fundamentada y pedagógica siguiendo las reglas anteriores.\n");

        String finalPrompt = promptBuilder.toString();
        log.debug("Prompt built successfully. Total length: {} characters", finalPrompt.length());

        return finalPrompt;
    }

    /**
     * Construye un prompt simplificado para casos sin contexto suficiente.
     *
     * @param question Pregunta del usuario
     * @return Prompt indicando falta de contexto
     */
    public String buildInsufficientContextPrompt(String question) {
        return SYSTEM_PROMPT + "\n\n" +
               "## Contexto Proporcionado\n\n" +
               "No se encontró contexto jurídico relevante en la base de conocimiento.\n\n" +
               "## Pregunta del Estudiante\n\n" +
               question + "\n\n" +
               "## Tu Respuesta\n\n" +
               "Indica claramente que no puedes responder con precisión jurídica por falta de contexto. " +
               "Sugiere al estudiante reformular la pregunta o consultar con su profesor.";
    }
}
