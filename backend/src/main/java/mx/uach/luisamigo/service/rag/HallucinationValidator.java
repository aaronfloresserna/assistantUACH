package mx.uach.luisamigo.service.rag;

import mx.uach.luisamigo.domain.LegalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valida respuestas del LLM para detectar posibles alucinaciones.
 * Verifica que las referencias legales mencionadas existan en el contexto proporcionado.
 */
@Component
public class HallucinationValidator {

    private static final Logger log = LoggerFactory.getLogger(HallucinationValidator.class);

    // Patrones para detectar referencias legales en la respuesta
    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
        "(?i)artículo\\s+(\\d+[a-z]?(?:\\s+bis)?(?:\\s+ter)?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LAW_PATTERN = Pattern.compile(
        "(?i)(Constitución|Código|Ley)\\s+[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Valida una respuesta del LLM contra el contexto proporcionado.
     * Detecta posibles alucinaciones de referencias legales.
     *
     * @param llmAnswer Respuesta del LLM
     * @param contextDocuments Documentos que fueron proporcionados como contexto
     * @return Resultado de validación con warnings si detecta problemas
     */
    public ValidationResult validate(String llmAnswer, List<LegalDocument> contextDocuments) {
        log.debug("Validating LLM response for potential hallucinations");

        List<String> warnings = new ArrayList<>();

        // Extraer referencias mencionadas en la respuesta
        List<String> mentionedArticles = extractArticles(llmAnswer);
        List<String> mentionedLaws = extractLaws(llmAnswer);

        // Extraer referencias disponibles en el contexto
        List<String> contextReferences = extractContextReferences(contextDocuments);

        // Validar artículos mencionados
        for (String article : mentionedArticles) {
            if (!isReferenceInContext(article, contextReferences)) {
                String warning = "Posible alucinación: Se menciona '" + article +
                                "' pero no está en el contexto proporcionado";
                warnings.add(warning);
                log.warn(warning);
            }
        }

        // Validar leyes mencionadas
        for (String law : mentionedLaws) {
            if (!isLawInContext(law, contextReferences)) {
                log.debug("Ley mencionada no encontrada exactamente en contexto: {}", law);
                // No agregamos warning para leyes porque puede ser una paráfrasis válida
            }
        }

        boolean isValid = warnings.isEmpty();
        log.info("Validation complete. Valid: {}, Warnings: {}", isValid, warnings.size());

        return new ValidationResult(isValid, warnings);
    }

    /**
     * Extrae números de artículos mencionados en la respuesta.
     */
    private List<String> extractArticles(String text) {
        List<String> articles = new ArrayList<>();
        Matcher matcher = ARTICLE_PATTERN.matcher(text);

        while (matcher.find()) {
            String article = matcher.group(1).trim();
            articles.add(article);
        }

        log.debug("Extracted {} article references from response", articles.size());
        return articles;
    }

    /**
     * Extrae nombres de leyes mencionadas en la respuesta.
     */
    private List<String> extractLaws(String text) {
        List<String> laws = new ArrayList<>();
        Matcher matcher = LAW_PATTERN.matcher(text);

        while (matcher.find()) {
            String law = matcher.group(0).trim();
            laws.add(law);
        }

        log.debug("Extracted {} law references from response", laws.size());
        return laws;
    }

    /**
     * Extrae todas las referencias legales del contexto.
     */
    private List<String> extractContextReferences(List<LegalDocument> documents) {
        List<String> references = new ArrayList<>();

        for (LegalDocument doc : documents) {
            if (doc.getLawReference() != null && !doc.getLawReference().isBlank()) {
                references.add(doc.getLawReference());
            }

            // También extraer referencias del texto de respuesta
            if (doc.getAnswer() != null) {
                references.addAll(extractArticles(doc.getAnswer()));
            }
        }

        return references;
    }

    /**
     * Verifica si una referencia está presente en el contexto.
     */
    private boolean isReferenceInContext(String reference, List<String> contextReferences) {
        String normalizedReference = reference.toLowerCase().trim();

        for (String contextRef : contextReferences) {
            String normalizedContextRef = contextRef.toLowerCase().trim();
            if (normalizedContextRef.contains(normalizedReference)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si una ley está presente en el contexto (match más flexible).
     */
    private boolean isLawInContext(String law, List<String> contextReferences) {
        String normalizedLaw = law.toLowerCase().trim();

        for (String contextRef : contextReferences) {
            String normalizedContextRef = contextRef.toLowerCase().trim();
            // Match flexible: si comparten palabras clave
            if (shareKeywords(normalizedLaw, normalizedContextRef)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si dos strings comparten palabras clave significativas.
     */
    private boolean shareKeywords(String str1, String str2) {
        String[] keywords1 = str1.split("\\s+");
        String[] keywords2 = str2.split("\\s+");

        int matches = 0;
        for (String keyword1 : keywords1) {
            if (keyword1.length() > 3) { // Solo palabras significativas
                for (String keyword2 : keywords2) {
                    if (keyword1.equals(keyword2)) {
                        matches++;
                        break;
                    }
                }
            }
        }

        // Si comparten al menos 2 palabras significativas, consideramos match
        return matches >= 2;
    }

    /**
     * Resultado de validación.
     */
    public record ValidationResult(boolean isValid, List<String> warnings) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
