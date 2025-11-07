package mx.uach.luisamigo.repository;

import mx.uach.luisamigo.domain.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para embeddings vectoriales.
 * Incluye operaciones de búsqueda por similitud usando pgvector.
 */
@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    /**
     * Busca el embedding asociado a un documento específico.
     */
    Optional<DocumentEmbedding> findByDocumentId(Long documentId);

    /**
     * Busca los K embeddings más similares usando cosine similarity.
     *
     * Nota: La sintaxis específica depende de cómo se configure pgvector.
     * Esta es una versión conceptual. En implementación real usar operador <=> de pgvector.
     *
     * Ejemplo real con pgvector:
     * SELECT * FROM document_embeddings
     * ORDER BY embedding <=> :queryVector::vector
     * LIMIT :limit
     */
    @Query(value = "SELECT e.* FROM document_embeddings e " +
                   "ORDER BY e.embedding <=> CAST(:queryVector AS vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DocumentEmbedding> findTopKSimilar(
        @Param("queryVector") String queryVector,  // Array serializado como string
        @Param("limit") int limit
    );

    /**
     * Busca embeddings similares con filtro por materia.
     */
    @Query(value = "SELECT e.* FROM document_embeddings e " +
                   "JOIN legal_documents d ON e.document_id = d.id " +
                   "WHERE d.materia = :materia " +
                   "ORDER BY e.embedding <=> CAST(:queryVector AS vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DocumentEmbedding> findTopKSimilarByMateria(
        @Param("queryVector") String queryVector,
        @Param("materia") String materia,
        @Param("limit") int limit
    );

    /**
     * Busca embeddings similares con score mínimo de similitud.
     *
     * Cosine similarity devuelve valores entre -1 y 1 (mayor = más similar).
     * El operador <=> de pgvector retorna "distance" (menor = más similar).
     */
    @Query(value = "SELECT e.*, " +
                   "(1 - (e.embedding <=> CAST(:queryVector AS vector))) as similarity_score " +
                   "FROM document_embeddings e " +
                   "WHERE (1 - (e.embedding <=> CAST(:queryVector AS vector))) >= :minScore " +
                   "ORDER BY e.embedding <=> CAST(:queryVector AS vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<DocumentEmbedding> findSimilarWithMinScore(
        @Param("queryVector") String queryVector,
        @Param("minScore") double minScore,
        @Param("limit") int limit
    );

    /**
     * Elimina embeddings de documentos de una fuente específica.
     */
    @Query("DELETE FROM DocumentEmbedding e WHERE e.document.source = :source")
    void deleteByDocumentSource(@Param("source") String source);

    /**
     * Cuenta el número total de embeddings.
     */
    @Query("SELECT COUNT(e) FROM DocumentEmbedding e")
    long countEmbeddings();

    /**
     * Inserta un embedding usando native query con CAST para pgvector.
     * Retorna el ID del embedding insertado.
     */
    @Query(value = "INSERT INTO document_embeddings " +
                   "(document_id, embedding, model_name, model_provider, embedding_version, created_at) " +
                   "VALUES (:documentId, CAST(:embeddingVector AS vector), :modelName, :modelProvider, :version, CURRENT_TIMESTAMP) " +
                   "RETURNING id",
           nativeQuery = true)
    Long insertEmbeddingWithCast(
        @Param("documentId") Long documentId,
        @Param("embeddingVector") String embeddingVector,
        @Param("modelName") String modelName,
        @Param("modelProvider") String modelProvider,
        @Param("version") Integer version
    );
}
