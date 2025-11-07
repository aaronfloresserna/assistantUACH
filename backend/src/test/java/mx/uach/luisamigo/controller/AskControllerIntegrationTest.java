package mx.uach.luisamigo.controller;

import mx.uach.luisamigo.dto.request.AskRequest;
import mx.uach.luisamigo.dto.response.AskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración para AskController.
 * Verifica que el asistente jurídico responda correctamente a consultas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AskControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/ask";
    }

    @Test
    @DisplayName("Debe responder correctamente a pregunta sobre derecho constitucional")
    void shouldAnswerConstitutionalLawQuestion() {
        // Given: Una pregunta sobre derecho constitucional
        AskRequest request = new AskRequest(
            "¿Qué es el amparo en México?",
            "constitucional",
            null,
            5
        );

        // When: Se envía la pregunta al asistente
        ResponseEntity<AskResponse> response = restTemplate.postForEntity(
            getBaseUrl(),
            request,
            AskResponse.class
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AskResponse body = response.getBody();

        // Validar que la respuesta contiene una respuesta
        assertThat(body.answer()).isNotBlank();
        assertThat(body.answer().length()).isGreaterThan(50);

        // Validar que se recuperaron fuentes
        assertThat(body.sources()).isNotNull();
        assertThat(body.sources().size()).isGreaterThan(0);
        assertThat(body.sources().size()).isLessThanOrEqualTo(5);

        // Validar metadata
        assertThat(body.metadata()).isNotNull();
        assertThat(body.metadata().documentsRetrieved()).isGreaterThan(0);
        assertThat(body.metadata().timestamp()).isNotNull();
        assertThat(body.metadata().processingTimeMs()).isGreaterThan(0);

        // Validar disclaimer
        assertThat(body.disclaimer()).isNotBlank();

        // Log para inspección visual
        System.out.println("\n=== PREGUNTA ===");
        System.out.println(request.question());
        System.out.println("\n=== RESPUESTA ===");
        System.out.println(body.answer());
        System.out.println("\n=== FUENTES ===");
        body.sources().forEach(source -> {
            System.out.printf("- [Score: %.4f] %s%n",
                source.similarityScore(),
                source.lawReference());
        });
        System.out.println("\n=== METADATA ===");
        System.out.printf("Documentos recuperados: %d%n", body.metadata().documentsRetrieved());
        System.out.printf("Tiempo de procesamiento: %d ms%n", body.metadata().processingTimeMs());
    }

    @Test
    @DisplayName("Debe responder correctamente a pregunta sobre derecho laboral")
    void shouldAnswerLaborLawQuestion() {
        // Given: Una pregunta sobre derecho laboral
        AskRequest request = new AskRequest(
            "¿Cuál es la jornada máxima de trabajo en México?",
            null,
            null,
            3
        );

        // When: Se envía la pregunta al asistente
        ResponseEntity<AskResponse> response = restTemplate.postForEntity(
            getBaseUrl(),
            request,
            AskResponse.class
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AskResponse body = response.getBody();

        // Validar estructura básica
        assertThat(body.answer()).isNotBlank();
        assertThat(body.sources()).isNotEmpty();
        assertThat(body.metadata()).isNotNull();

        // Log para inspección visual
        System.out.println("\n=== PREGUNTA ===");
        System.out.println(request.question());
        System.out.println("\n=== RESPUESTA ===");
        System.out.println(body.answer());
    }

    @Test
    @DisplayName("Debe responder correctamente a pregunta general sobre el sistema jurídico mexicano")
    void shouldAnswerGeneralLegalQuestion() {
        // Given: Una pregunta general
        AskRequest request = new AskRequest(
            "¿Qué es la Constitución Política de los Estados Unidos Mexicanos?"
        );

        // When: Se envía la pregunta al asistente
        ResponseEntity<AskResponse> response = restTemplate.postForEntity(
            getBaseUrl(),
            request,
            AskResponse.class
        );

        // Then: La respuesta debe ser exitosa
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AskResponse body = response.getBody();

        // Validar estructura básica
        assertThat(body.answer()).isNotBlank();
        assertThat(body.sources()).isNotEmpty();
        assertThat(body.metadata()).isNotNull();
        assertThat(body.disclaimer()).contains("académico");

        // Log para inspección visual
        System.out.println("\n=== PREGUNTA ===");
        System.out.println(request.question());
        System.out.println("\n=== RESPUESTA ===");
        System.out.println(body.answer());
    }

    @Test
    @DisplayName("Debe rechazar pregunta vacía con error 400")
    void shouldRejectEmptyQuestion() {
        // Given: Una pregunta vacía
        AskRequest request = new AskRequest(
            "",
            null,
            null,
            5
        );

        // When: Se envía la pregunta al asistente
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl(),
            request,
            String.class
        );

        // Then: Debe retornar error 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
