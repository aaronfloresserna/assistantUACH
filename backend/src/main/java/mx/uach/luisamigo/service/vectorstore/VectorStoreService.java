package mx.uach.luisamigo.service.vectorstore;

import mx.uach.luisamigo.domain.DocumentEmbedding;
import mx.uach.luisamigo.domain.LegalDocument;

import java.util.List;

/**
 * Servicio para operaciones CRUD en el vector store (PostgreSQL + pgvector).
 */
public interface VectorStoreService {

    /**
     * Persiste un documento con su embedding en el vector store.
     *
     * @param document Documento jurídico
     * @param embedding Vector embedding del documento
     * @return ID del embedding persistido
     */
    Long storeDocument(LegalDocument document, float[] embedding);

    /**
     * Persiste múltiples documentos con sus embeddings en batch.
     * Más eficiente que llamar storeDocument() múltiples veces.
     *
     * @param documentsWithEmbeddings Lista de pares (documento, embedding)
     * @return Número de documentos persistidos exitosamente
     */
    int storeBatch(List<DocumentWithEmbedding> documentsWithEmbeddings);

    /**
     * Elimina un documento y su embedding del vector store.
     *
     * @param documentId ID del documento a eliminar
     * @return true si se eliminó exitosamente
     */
    boolean deleteDocument(Long documentId);

    /**
     * Elimina todos los documentos de una fuente específica.
     * Útil para re-ingesta completa de un dataset.
     *
     * @param sourceName Nombre de la fuente (e.g., "Barcenas-Juridico-Mexicano-Dataset")
     * @return Número de documentos eliminados
     */
    int deleteBySource(String sourceName);

    /**
     * Busca documentos similares usando búsqueda vectorial (cosine similarity).
     *
     * @param queryEmbedding Vector embedding de la consulta
     * @param topK Número de resultados a retornar
     * @return Lista de embeddings ordenados por similitud (mayor a menor)
     */
    List<DocumentEmbedding> findSimilar(float[] queryEmbedding, int topK);

    /**
     * Busca documentos similares con filtros adicionales.
     *
     * @param queryEmbedding Vector embedding de la consulta
     * @param topK Número de resultados a retornar
     * @param filters Filtros opcionales (materia, tags, etc.)
     * @return Lista de embeddings filtrados y ordenados por similitud
     */
    List<DocumentEmbedding> findSimilar(float[] queryEmbedding, int topK, SearchFilters filters);

    /**
     * Retorna el número total de documentos en el vector store.
     *
     * @return Número de documentos
     */
    long countDocuments();

    /**
     * Retorna el número de documentos por fuente.
     *
     * @param sourceName Nombre de la fuente
     * @return Número de documentos de esa fuente
     */
    long countBySource(String sourceName);

    /**
     * Record helper para pasar documento + embedding juntos.
     */
    record DocumentWithEmbedding(LegalDocument document, float[] embedding) {}
}
