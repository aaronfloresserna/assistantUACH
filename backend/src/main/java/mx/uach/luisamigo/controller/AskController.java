package mx.uach.luisamigo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.uach.luisamigo.dto.request.AskRequest;
import mx.uach.luisamigo.dto.response.AskResponse;
import mx.uach.luisamigo.dto.response.ErrorResponse;
import mx.uach.luisamigo.service.rag.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ask")
@Tag(name = "Ask", description = "Consultas al asistente jurídico UACH")
public class AskController {

    private static final Logger log = LoggerFactory.getLogger(AskController.class);
    private final RAGService ragService;

    public AskController(RAGService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    @Operation(summary = "Consultar al asistente jurídico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Respuesta exitosa",
            content = @Content(schema = @Schema(implementation = AskResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        log.info("Received ask request: '{}'", request.question());
        AskResponse response = ragService.ask(request);
        return ResponseEntity.ok(response);
    }
}
