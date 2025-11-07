package mx.uach.luisamigo.service.ingestion;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para dividir textos largos en chunks manejables.
 * Respeta límites de oraciones y mantiene overlap para preservar contexto.
 */
@Component
public class ChunkingService {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 100;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[.!?;]\\s+");

    /**
     * Divide un texto en chunks con tamaño y overlap configurables.
     */
    public List<String> chunkText(String text, int maxChunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Si el texto es menor al tamaño máximo, retornar como único chunk
        if (text.length() <= maxChunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        List<String> sentences = splitIntoSentences(text);

        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (String sentence : sentences) {
            int sentenceLength = sentence.length();

            // Si agregar esta oración excede el límite
            if (currentSize + sentenceLength > maxChunkSize && currentSize > 0) {
                // Guardar chunk actual
                chunks.add(currentChunk.toString().trim());

                // Iniciar nuevo chunk con overlap
                currentChunk = new StringBuilder();
                currentSize = 0;

                // Agregar overlap del chunk anterior
                String overlapText = getOverlapText(chunks.get(chunks.size() - 1), overlap);
                if (!overlapText.isEmpty()) {
                    currentChunk.append(overlapText).append(" ");
                    currentSize = overlapText.length() + 1;
                }
            }

            // Agregar oración al chunk actual
            if (currentSize > 0) {
                currentChunk.append(" ");
                currentSize++;
            }
            currentChunk.append(sentence);
            currentSize += sentenceLength;
        }

        // Agregar último chunk si tiene contenido
        if (currentSize > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Divide texto usando configuración por defecto.
     */
    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * Divide texto en oraciones respetando puntuación.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }

        // Agregar última parte si no termina con puntuación
        if (lastEnd < text.length()) {
            String lastSentence = text.substring(lastEnd).trim();
            if (!lastSentence.isEmpty()) {
                sentences.add(lastSentence);
            }
        }

        return sentences;
    }

    /**
     * Obtiene los últimos N caracteres de un texto para usar como overlap.
     * Intenta cortar en límite de palabra para evitar palabras partidas.
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }

        String overlap = text.substring(text.length() - overlapSize);

        // Buscar primer espacio para evitar cortar palabras
        int firstSpace = overlap.indexOf(' ');
        if (firstSpace > 0 && firstSpace < overlap.length() - 1) {
            return overlap.substring(firstSpace + 1);
        }

        return overlap;
    }

    /**
     * Verifica si un texto necesita ser dividido en chunks.
     */
    public boolean needsChunking(String text, int maxChunkSize) {
        return text != null && text.length() > maxChunkSize;
    }

    /**
     * Calcula el número aproximado de chunks que generará un texto.
     */
    public int estimateChunkCount(String text, int maxChunkSize, int overlap) {
        if (text == null || text.length() <= maxChunkSize) {
            return 1;
        }

        // Estimación simplificada
        int effectiveChunkSize = maxChunkSize - overlap;
        return (int) Math.ceil((double) text.length() / effectiveChunkSize);
    }
}
