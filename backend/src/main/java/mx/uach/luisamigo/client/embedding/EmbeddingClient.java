package mx.uach.luisamigo.client.embedding;

import java.util.List;

/**
 * Abstracción de clientes de embeddings.
 * Permite intercambiar proveedores de embeddings sin modificar lógica de negocio.
 */
public interface EmbeddingClient {

    /**
     * Genera un vector embedding para un texto dado.
     *
     * @param text Texto a convertir en embedding
     * @return Array de floats representando el vector
     * @throws EmbeddingException si hay error al generar el embedding
     */
    float[] generateEmbedding(String text);

    /**
     * Genera embeddings para múltiples textos en batch.
     * Más eficiente que llamar generateEmbedding() múltiples veces.
     *
     * @param texts Lista de textos a convertir
     * @return Lista de arrays de floats en el mismo orden
     * @throws EmbeddingException si hay error al generar embeddings
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Retorna la dimensionalidad de los embeddings generados.
     *
     * @return Número de dimensiones (e.g., 1536 para text-embedding-3-small)
     */
    int getDimensions();

    /**
     * Retorna el nombre del modelo de embedding usado.
     *
     * @return Nombre del modelo (e.g., "text-embedding-3-small")
     */
    String getModelName();

    /**
     * Retorna el nombre del proveedor.
     *
     * @return Nombre del proveedor (e.g., "OpenAI")
     */
    String getProviderName();

    /**
     * Verifica si el cliente está disponible y configurado correctamente.
     *
     * @return true si el cliente puede generar embeddings
     */
    boolean isAvailable();

    /**
     * Retorna el tamaño máximo de texto soportado (en tokens aproximados).
     *
     * @return Límite de tokens
     */
    int getMaxTokens();
}
