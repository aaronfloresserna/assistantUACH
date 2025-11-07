package mx.uach.luisamigo.service.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga datasets desde Hugging Face usando la API de datasets.
 * Específicamente para Barcenas-Juridico-Mexicano-Dataset.
 */
@Component
public class DatasetLoader {

    private static final Logger log = LoggerFactory.getLogger(DatasetLoader.class);
    private static final String HF_DATASETS_API = "https://datasets-server.huggingface.co";
    private static final String BARCENAS_DATASET = "Danielbrdz/Barcenas-Juridico-Mexicano-Dataset";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DatasetLoader(ObjectMapper objectMapper) {
        // Create WebClient directly without using injected builder
        // to avoid potential configuration conflicts
        this.webClient = WebClient.builder()
            .baseUrl(HF_DATASETS_API)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Luis-Amigo/0.1.0)")
            .defaultHeader("Accept", "application/json")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Carga el dataset Barcenas completo desde Hugging Face.
     * Usa la API de parquet rows para obtener los datos.
     */
    public List<BarcerasDatasetEntry> loadBarcerasDataset() {
        log.info("Starting to load Barcenas dataset from Hugging Face");

        try {
            // Primero obtener información del dataset
            String splits = getDatasetSplits();
            log.debug("Dataset splits: {}", splits);

            // Cargar rows usando la API de parquet
            List<BarcerasDatasetEntry> entries = loadRows();

            log.info("Successfully loaded {} entries from Barcenas dataset", entries.size());
            return entries;

        } catch (Exception e) {
            log.error("Error loading Barcenas dataset", e);
            throw new RuntimeException("Failed to load dataset from Hugging Face: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene los splits disponibles del dataset.
     */
    private String getDatasetSplits() {
        String url = "/splits?dataset=" + BARCENAS_DATASET;

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .block();
    }

    /**
     * Carga las filas del dataset usando la API de rows.
     */
    private List<BarcerasDatasetEntry> loadRows() {
        List<BarcerasDatasetEntry> allEntries = new ArrayList<>();
        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        while (hasMore) {
            log.debug("Loading rows with offset={}, limit={}", offset, limit);

            String url = String.format("/rows?dataset=%s&config=default&split=train&offset=%d&length=%d",
                BARCENAS_DATASET, offset, limit);

            try {
                String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

                // Parsear response JSON
                JsonNode root = objectMapper.readTree(response);
                JsonNode rows = root.get("rows");

                if (rows == null || !rows.isArray() || rows.size() == 0) {
                    hasMore = false;
                    break;
                }

                // Convertir cada row a BarcerasDatasetEntry
                for (JsonNode row : rows) {
                    JsonNode rowData = row.get("row");
                    if (rowData != null) {
                        BarcerasDatasetEntry entry = parseEntry(rowData);
                        if (entry != null) {
                            allEntries.add(entry);
                        }
                    }
                }

                offset += limit;

                // Si recibimos menos de 'limit' rows, no hay más datos
                if (rows.size() < limit) {
                    hasMore = false;
                }

                // Pequeña pausa para no saturar la API
                Thread.sleep(100);

            } catch (Exception e) {
                log.warn("Error loading rows at offset {}: {}", offset, e.getMessage());
                hasMore = false;
            }
        }

        return allEntries;
    }

    /**
     * Parsea una entrada del dataset.
     */
    private BarcerasDatasetEntry parseEntry(JsonNode rowData) {
        try {
            String question = rowData.has("question") ? rowData.get("question").asText() : null;
            String answer = rowData.has("answer") ? rowData.get("answer").asText() : null;
            String context = rowData.has("context") ? rowData.get("context").asText() : null;

            if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
                return null;
            }

            return new BarcerasDatasetEntry(question, answer, context);

        } catch (Exception e) {
            log.warn("Error parsing entry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Representa una entrada del dataset Barcenas.
     */
    public record BarcerasDatasetEntry(
        String question,
        String answer,
        String context
    ) {}
}
