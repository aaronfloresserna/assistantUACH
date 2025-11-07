package mx.uach.luisamigo.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidad JPA para embeddings vectoriales.
 * Usa el tipo 'vector' de pgvector para almacenar arrays de floats.
 */
@Entity
@Table(name = "document_embeddings")
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private LegalDocument document;

    // pgvector almacena como tipo 'vector'
    // En JPA lo manejamos como String y convertimos según sea necesario
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding;  // Serializado como "[0.1, 0.2, ...]"

    // Transient field para trabajar con el array en memoria
    @Transient
    private float[] embeddingArray;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_provider", nullable = false, length = 50)
    private String modelProvider;

    @Column(name = "embedding_version", nullable = false)
    private Integer embeddingVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        // Convertir array a string si es necesario
        if (embeddingArray != null && embedding == null) {
            embedding = serializeEmbedding(embeddingArray);
        }
    }

    @PostLoad
    protected void onLoad() {
        // Convertir string a array después de cargar
        if (embedding != null && embeddingArray == null) {
            embeddingArray = deserializeEmbedding(embedding);
        }
    }

    // Constructors
    protected DocumentEmbedding() {
        // JPA requires a no-arg constructor
    }

    public DocumentEmbedding(LegalDocument document, float[] embeddingArray,
                             String modelName, String modelProvider) {
        this.document = document;
        this.embeddingArray = embeddingArray;
        this.embedding = serializeEmbedding(embeddingArray);
        this.modelName = modelName;
        this.modelProvider = modelProvider;
    }

    // Serialización / Deserialización de embeddings
    private String serializeEmbedding(float[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] deserializeEmbedding(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        // Remover corchetes y dividir por comas
        String cleaned = str.substring(1, str.length() - 1);
        String[] parts = cleaned.split(",");
        float[] array = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            array[i] = Float.parseFloat(parts[i].trim());
        }
        return array;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public LegalDocument getDocument() { return document; }
    public void setDocument(LegalDocument document) { this.document = document; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) {
        this.embedding = embedding;
        this.embeddingArray = null;  // Invalidar cache
    }

    public float[] getEmbeddingArray() {
        if (embeddingArray == null && embedding != null) {
            embeddingArray = deserializeEmbedding(embedding);
        }
        return embeddingArray;
    }

    public void setEmbeddingArray(float[] embeddingArray) {
        this.embeddingArray = embeddingArray;
        this.embedding = serializeEmbedding(embeddingArray);
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public Integer getEmbeddingVersion() { return embeddingVersion; }
    public void setEmbeddingVersion(Integer embeddingVersion) { this.embeddingVersion = embeddingVersion; }

    public Instant getCreatedAt() { return createdAt; }

    public int getDimensions() {
        float[] array = getEmbeddingArray();
        return array != null ? array.length : 0;
    }

    @Override
    public String toString() {
        return "DocumentEmbedding{" +
               "id=" + id +
               ", documentId=" + (document != null ? document.getId() : null) +
               ", dimensions=" + getDimensions() +
               ", model='" + modelName + '\'' +
               '}';
    }
}
