package mx.uach.luisamigo.service.vectorstore;

import mx.uach.luisamigo.client.embedding.EmbeddingClient;
import mx.uach.luisamigo.client.embedding.EmbeddingClientFactory;
import mx.uach.luisamigo.domain.DocumentEmbedding;
import mx.uach.luisamigo.domain.LegalDocument;
import mx.uach.luisamigo.repository.DocumentEmbeddingRepository;
import mx.uach.luisamigo.repository.LegalDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de VectorStoreService.
 * Maneja operaciones CRUD sobre PostgreSQL + pgvector.
 */
@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreServiceImpl.class);

    private final LegalDocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final EmbeddingClientFactory embeddingClientFactory;

    public VectorStoreServiceImpl(
        LegalDocumentRepository documentRepository,
        DocumentEmbeddingRepository embeddingRepository,
        EmbeddingClientFactory embeddingClientFactory
    ) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingClientFactory = embeddingClientFactory;
    }

    @Override
    @Transactional
    public Long storeDocument(LegalDocument document, float[] embedding) {
        log.debug("Storing document with external_id: {}", document.getExternalId());

        // Guardar documento
        LegalDocument savedDocument = documentRepository.save(document);

        // Crear y guardar embedding
        EmbeddingClient embeddingClient = embeddingClientFactory.getClient();
        DocumentEmbedding documentEmbedding = new DocumentEmbedding(
            savedDocument,
            embedding,
            embeddingClient.getModelName(),
            embeddingClient.getProviderName()
        );

        DocumentEmbedding savedEmbedding = embeddingRepository.save(documentEmbedding);
        log.info("Document stored successfully. ID: {}, external_id: {}",
            savedDocument.getId(), savedDocument.getExternalId());

        return savedEmbedding.getId();
    }

    @Override
    @Transactional
    public int storeBatch(List<DocumentWithEmbedding> documentsWithEmbeddings) {
        log.info("Storing batch of {} documents", documentsWithEmbeddings.size());

        int successCount = 0;
        EmbeddingClient embeddingClient = embeddingClientFactory.getClient();

        for (DocumentWithEmbedding item : documentsWithEmbeddings) {
            try {
                LegalDocument savedDocument = documentRepository.save(item.document());

                // Serializar embedding a formato de string para pgvector
                String embeddingStr = serializeEmbedding(item.embedding());

                // Usar native query con CAST para insertar en pgvector
                embeddingRepository.insertEmbeddingWithCast(
                    savedDocument.getId(),
                    embeddingStr,
                    embeddingClient.getModelName(),
                    embeddingClient.getProviderName(),
                    1  // embedding_version
                );

                successCount++;

            } catch (Exception e) {
                log.error("Failed to store document: {}", item.document().getExternalId(), e);
            }
        }

        log.info("Batch storage complete. Success: {}/{}", successCount, documentsWithEmbeddings.size());
        return successCount;
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long documentId) {
        log.debug("Deleting document with ID: {}", documentId);

        if (documentRepository.existsById(documentId)) {
            documentRepository.deleteById(documentId);
            log.info("Document deleted successfully. ID: {}", documentId);
            return true;
        }

        log.warn("Document not found for deletion. ID: {}", documentId);
        return false;
    }

    @Override
    @Transactional
    public int deleteBySource(String sourceName) {
        log.info("Deleting all documents from source: {}", sourceName);

        long countBefore = documentRepository.countBySource(sourceName);
        documentRepository.deleteBySource(sourceName);

        log.info("Deleted {} documents from source: {}", countBefore, sourceName);
        return (int) countBefore;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentEmbedding> findSimilar(float[] queryEmbedding, int topK) {
        return findSimilar(queryEmbedding, topK, SearchFilters.empty());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentEmbedding> findSimilar(float[] queryEmbedding, int topK, SearchFilters filters) {
        log.debug("Searching for {} similar documents with filters: {}", topK, filters.hasFilters());

        String queryVector = serializeEmbedding(queryEmbedding);

        List<DocumentEmbedding> results;

        if (!filters.hasFilters()) {
            // Búsqueda simple sin filtros
            results = embeddingRepository.findTopKSimilar(queryVector, topK);
        } else if (filters.getMateria() != null && filters.getMinSimilarityScore() == null) {
            // Búsqueda con filtro de materia
            results = embeddingRepository.findTopKSimilarByMateria(queryVector, filters.getMateria(), topK);
        } else if (filters.getMinSimilarityScore() != null) {
            // Búsqueda con score mínimo
            results = embeddingRepository.findSimilarWithMinScore(
                queryVector,
                filters.getMinSimilarityScore(),
                topK
            );
        } else {
            // Búsqueda simple como fallback
            results = embeddingRepository.findTopKSimilar(queryVector, topK);
        }

        log.info("Found {} similar documents", results.size());
        return results;
    }

    @Override
    public long countDocuments() {
        return documentRepository.count();
    }

    @Override
    public long countBySource(String sourceName) {
        return documentRepository.countBySource(sourceName);
    }

    /**
     * Serializa un array de floats al formato esperado por pgvector.
     * Formato: "[0.1, 0.2, 0.3, ...]"
     */
    private String serializeEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
