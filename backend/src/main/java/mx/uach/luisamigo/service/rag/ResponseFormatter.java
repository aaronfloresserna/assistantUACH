package mx.uach.luisamigo.service.rag;

import mx.uach.luisamigo.domain.DocumentEmbedding;
import mx.uach.luisamigo.domain.LegalDocument;
import mx.uach.luisamigo.dto.response.AskResponse;
import mx.uach.luisamigo.dto.response.SourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Formatea respuestas del RAG system con referencias a fuentes citadas.
 */
@Component
public class ResponseFormatter {

    private static final Logger log = LoggerFactory.getLogger(ResponseFormatter.class);

    /**
     * Formatea la respuesta del LLM con las fuentes recuperadas.
     *
     * @param llmAnswer Respuesta del LLM
     * @param retrievedDocuments Documentos recuperados del vector store
     * @param materia Materia filtrada (opcional)
     * @param processingTimeMs Tiempo de procesamiento en ms
     * @return AskResponse formateado con fuentes
     */
    public AskResponse formatResponse(
        String llmAnswer,
        List<DocumentEmbedding> retrievedDocuments,
        String materia,
        long processingTimeMs
    ) {
        log.debug("Formatting response with {} sources", retrievedDocuments.size());

        // Construir lista de referencias de fuentes
        List<SourceReference> sources = new ArrayList<>();
        for (DocumentEmbedding embedding : retrievedDocuments) {
            LegalDocument doc = embedding.getDocument();

            // Construir texto de la fuente (extracto de la respuesta)
            String sourceText = buildSourceText(doc);

            SourceReference sourceRef = new SourceReference(
                doc.getId(),
                sourceText,
                doc.getLawReference(),
                doc.getSource(),
                null // similarity score no está calculado aún
            );

            sources.add(sourceRef);
        }

        // Construir metadata
        AskResponse.ResponseMetadata metadata = new AskResponse.ResponseMetadata(
            retrievedDocuments.size(),
            materia,
            Instant.now(),
            processingTimeMs
        );

        // Asegurar que la respuesta incluye el disclaimer
        String finalAnswer = ensureDisclaimer(llmAnswer);

        log.info("Response formatted successfully. Sources: {}, Processing time: {}ms",
            sources.size(), processingTimeMs);

        return new AskResponse(finalAnswer, sources, metadata);
    }

    /**
     * Construye el texto de la fuente (extracto de máximo 200 caracteres).
     */
    private String buildSourceText(LegalDocument doc) {
        String answer = doc.getAnswer();
        if (answer == null || answer.isBlank()) {
            return "";
        }

        // Limitar a 200 caracteres para el extracto
        if (answer.length() <= 200) {
            return answer;
        }

        return answer.substring(0, 197) + "...";
    }

    /**
     * Asegura que la respuesta incluye el disclaimer académico.
     * Si no está presente, lo agrega al final.
     */
    private String ensureDisclaimer(String answer) {
        String disclaimer = "Esto es material académico y no constituye asesoría jurídica profesional.";

        if (answer.contains(disclaimer)) {
            return answer;
        }

        // Agregar disclaimer al final
        return answer + "\n\n" + disclaimer;
    }

    /**
     * Formatea una respuesta de error cuando no hay contexto suficiente.
     */
    public AskResponse formatInsufficientContextResponse(
        String llmAnswer,
        String materia,
        long processingTimeMs
    ) {
        log.debug("Formatting insufficient context response");

        AskResponse.ResponseMetadata metadata = new AskResponse.ResponseMetadata(
            0,
            materia,
            Instant.now(),
            processingTimeMs
        );

        String finalAnswer = ensureDisclaimer(llmAnswer);

        return new AskResponse(finalAnswer, List.of(), metadata);
    }
}
