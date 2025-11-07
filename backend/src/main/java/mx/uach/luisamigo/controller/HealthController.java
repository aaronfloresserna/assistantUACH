package mx.uach.luisamigo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mx.uach.luisamigo.service.rag.RAGService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final RAGService ragService;

    public HealthController(RAGService ragService) {
        this.ragService = ragService;
    }

    @GetMapping
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> health() {
        boolean isHealthy = ragService.isHealthy();
        Map<String, Object> health = new HashMap<>();
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("ragSystem", isHealthy ? "operational" : "unavailable");
        return isHealthy ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }
}
