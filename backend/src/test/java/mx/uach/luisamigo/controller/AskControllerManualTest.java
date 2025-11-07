package mx.uach.luisamigo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mx.uach.luisamigo.dto.request.AskRequest;
import mx.uach.luisamigo.dto.response.AskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba manual contra el servidor corriendo en localhost:8080.
 *
 * NOTA: Este test requiere que el servidor esté corriendo.
 * Para ejecutarlo: mvn test -Dtest=AskControllerManualTest
 */
class AskControllerManualTest {

    private static final String BASE_URL = "http://localhost:8080/api/ask";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Debe responder correctamente a pregunta sobre derecho constitucional")
    void shouldAnswerConstitutionalLawQuestion() throws Exception {
        // Given: Una pregunta sobre derecho constitucional
        AskRequest request = new AskRequest(
            "¿Qué es el amparo en México?",
            "constitucional",
            null,
            5
        );

        // When: Se envía la pregunta al asistente
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.statusCode()).isEqualTo(200);

        AskResponse body = objectMapper.readValue(response.body(), AskResponse.class);

        // Validar que la respuesta contiene una respuesta
        assertThat(body.answer()).isNotBlank();
        assertThat(body.answer().length()).isGreaterThan(50);

        // Validar que se recuperaron fuentes (puede ser 0 si no hay documentos relevantes)
        assertThat(body.sources()).isNotNull();
        assertThat(body.sources().size()).isLessThanOrEqualTo(5);

        // Validar metadata
        assertThat(body.metadata()).isNotNull();
        assertThat(body.metadata().documentsRetrieved()).isGreaterThanOrEqualTo(0);
        assertThat(body.metadata().timestamp()).isNotNull();
        assertThat(body.metadata().processingTimeMs()).isGreaterThan(0);

        // Validar disclaimer
        assertThat(body.disclaimer()).isNotBlank();

        // Log para inspección visual
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PREGUNTA: " + request.question());
        System.out.println("=".repeat(80));
        System.out.println("\nRESPUESTA:");
        System.out.println(body.answer());
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FUENTES CITADAS:");
        body.sources().forEach(source -> {
            System.out.printf("\n  [Score: %.4f] %s%n",
                source.similarityScore(),
                source.lawReference() != null ? source.lawReference() : "Sin referencia");
            if (source.text() != null && source.text().length() > 100) {
                System.out.println("  " + source.text().substring(0, 100) + "...");
            } else if (source.text() != null) {
                System.out.println("  " + source.text());
            }
        });
        System.out.println("\n" + "=".repeat(80));
        System.out.println("METADATA:");
        System.out.printf("  Documentos recuperados: %d%n", body.metadata().documentsRetrieved());
        System.out.printf("  Tiempo de procesamiento: %d ms%n", body.metadata().processingTimeMs());
        System.out.printf("  Materia: %s%n", body.metadata().materia());
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @DisplayName("Debe responder correctamente a pregunta sobre derecho laboral")
    void shouldAnswerLaborLawQuestion() throws Exception {
        // Given: Una pregunta sobre derecho laboral
        AskRequest request = new AskRequest(
            "¿Cuál es la jornada máxima de trabajo en México?",
            null,
            null,
            3
        );

        // When: Se envía la pregunta al asistente
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.statusCode()).isEqualTo(200);

        AskResponse body = objectMapper.readValue(response.body(), AskResponse.class);

        // Validar estructura básica
        assertThat(body.answer()).isNotBlank();
        assertThat(body.sources()).isNotEmpty();
        assertThat(body.metadata()).isNotNull();

        // Log para inspección visual
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PREGUNTA: " + request.question());
        System.out.println("=".repeat(80));
        System.out.println("\nRESPUESTA:");
        System.out.println(body.answer());
        System.out.println("\n" + "=".repeat(80));
    }

    @Test
    @DisplayName("Debe responder correctamente a pregunta general sobre el sistema jurídico mexicano")
    void shouldAnswerGeneralLegalQuestion() throws Exception {
        // Given: Una pregunta general
        AskRequest request = new AskRequest(
            "¿Qué es la Constitución Política de los Estados Unidos Mexicanos?"
        );

        // When: Se envía la pregunta al asistente
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.statusCode()).isEqualTo(200);

        AskResponse body = objectMapper.readValue(response.body(), AskResponse.class);

        // Validar estructura básica
        assertThat(body.answer()).isNotBlank();
        assertThat(body.sources()).isNotEmpty();
        assertThat(body.metadata()).isNotNull();
        assertThat(body.disclaimer()).contains("académico");

        // Log para inspección visual
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PREGUNTA: " + request.question());
        System.out.println("=".repeat(80));
        System.out.println("\nRESPUESTA:");
        System.out.println(body.answer());
        System.out.println("\n" + "=".repeat(80));
    }

    @Test
    @DisplayName("Debe rechazar pregunta vacía con error 400")
    void shouldRejectEmptyQuestion() throws Exception {
        // Given: Una pregunta vacía
        AskRequest request = new AskRequest(
            "",
            null,
            null,
            5
        );

        // When: Se envía la pregunta al asistente
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // Then: Debe retornar error 400
        assertThat(response.statusCode()).isEqualTo(400);

        System.out.println("\n=== VALIDACIÓN: Pregunta vacía rechazada correctamente ===");
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
