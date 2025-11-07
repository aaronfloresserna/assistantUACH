package mx.uach.luisamigo.client.llm;

/**
 * Abstracción de clientes LLM (Large Language Model).
 * Permite intercambiar proveedores (OpenAI, Anthropic, etc.) sin modificar lógica de negocio.
 */
public interface LLMClient {

    /**
     * Genera una respuesta usando el modelo LLM configurado.
     *
     * @param prompt El prompt completo a enviar al modelo
     * @param config Configuración específica para esta llamada
     * @return La respuesta generada por el modelo
     * @throws LLMProviderException si hay error al comunicarse con el proveedor
     */
    String generateResponse(String prompt, LLMConfig config);

    /**
     * Genera una respuesta usando la configuración por defecto.
     *
     * @param prompt El prompt completo a enviar al modelo
     * @return La respuesta generada por el modelo
     * @throws LLMProviderException si hay error al comunicarse con el proveedor
     */
    default String generateResponse(String prompt) {
        return generateResponse(prompt, LLMConfig.defaultConfig());
    }

    /**
     * Verifica si el cliente está disponible y configurado correctamente.
     *
     * @return true si el cliente puede hacer llamadas al proveedor
     */
    boolean isAvailable();

    /**
     * Retorna el nombre del proveedor (e.g., "OpenAI", "Anthropic").
     *
     * @return Nombre del proveedor
     */
    String getProviderName();

    /**
     * Retorna el modelo específico usado (e.g., "gpt-4", "claude-3-sonnet").
     *
     * @return Nombre del modelo
     */
    String getModelName();

    /**
     * Estima el costo aproximado de una llamada basado en tokens.
     *
     * @param estimatedTokens Número estimado de tokens (input + output)
     * @return Costo estimado en USD
     */
    double estimateCost(int estimatedTokens);
}
