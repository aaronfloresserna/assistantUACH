package mx.uach.luisamigo.service.ingestion;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normaliza y limpia textos jurídicos antes de procesarlos.
 */
@Component
public class TextNormalizer {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{3,}");
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

    /**
     * Normaliza un texto completo.
     */
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text;

        // Remover HTML tags si existen
        normalized = removeHtmlTags(normalized);

        // Normalizar unicode (NFD -> NFC)
        normalized = normalizeUnicode(normalized);

        // Limpiar espacios múltiples
        normalized = cleanWhitespace(normalized);

        // Trim
        normalized = normalized.trim();

        return normalized;
    }

    /**
     * Limpia texto pero preserva estructura básica.
     */
    public String cleanPreservingStructure(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text;

        // Remover HTML
        cleaned = removeHtmlTags(cleaned);

        // Normalizar espacios múltiples pero preservar saltos de línea
        cleaned = MULTIPLE_SPACES.matcher(cleaned).replaceAll(" ");

        // Reducir múltiples saltos de línea a máximo 2
        cleaned = MULTIPLE_NEWLINES.matcher(cleaned).replaceAll("\n\n");

        return cleaned.trim();
    }

    /**
     * Remueve etiquetas HTML.
     */
    private String removeHtmlTags(String text) {
        return HTML_TAGS.matcher(text).replaceAll("");
    }

    /**
     * Normaliza caracteres unicode.
     */
    private String normalizeUnicode(String text) {
        // Normalizar a forma canónica compuesta (NFC)
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /**
     * Limpia espacios en blanco múltiples.
     */
    private String cleanWhitespace(String text) {
        // Reemplazar múltiples espacios por uno solo
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");

        // Reducir múltiples saltos de línea
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");

        return text;
    }

    /**
     * Extrae referencia legal del texto si existe.
     * Busca patrones como "Artículo 123", "Art. 45", etc.
     */
    public String extractLegalReference(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Patrones comunes de referencias legales
        Pattern[] patterns = {
            Pattern.compile("(?i)artículo\\s+(\\d+[a-z]?(?:\\s+bis)?(?:\\s+ter)?)"),
            Pattern.compile("(?i)art\\.?\\s+(\\d+[a-z]?)"),
            Pattern.compile("(?i)(constitución|código|ley)\\s+[a-záéíóúñ\\s]+", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            var matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(0).trim();
            }
        }

        return null;
    }
}
