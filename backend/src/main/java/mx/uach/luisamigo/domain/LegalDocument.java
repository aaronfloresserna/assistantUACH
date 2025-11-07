package mx.uach.luisamigo.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para documentos jurídicos.
 * Representa un par pregunta/respuesta del dataset con metadatos jurídicos.
 */
@Entity
@Table(name = "legal_documents", indexes = {
    @Index(name = "idx_legal_documents_materia", columnList = "materia"),
    @Index(name = "idx_legal_documents_source", columnList = "source"),
    @Index(name = "idx_legal_documents_external_id", columnList = "external_id")
})
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "law_reference", length = 500)
    private String lawReference;

    @Column(name = "materia", length = 100)
    private String materia;

    @ElementCollection
    @CollectionTable(name = "legal_document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Column(name = "semester_level")
    private Integer semesterLevel;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relación uno-a-uno con embedding
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private DocumentEmbedding embedding;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors
    protected LegalDocument() {
        // JPA requires a no-arg constructor
    }

    public LegalDocument(String externalId, String question, String answer, String source) {
        this.externalId = externalId;
        this.question = question;
        this.answer = answer;
        this.source = source;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public Long getId() { return id; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getLawReference() { return lawReference; }
    public void setLawReference(String lawReference) { this.lawReference = lawReference; }

    public String getMateria() { return materia; }
    public void setMateria(String materia) { this.materia = materia; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public void addTag(String tag) { this.tags.add(tag); }

    public Integer getSemesterLevel() { return semesterLevel; }
    public void setSemesterLevel(Integer semesterLevel) { this.semesterLevel = semesterLevel; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public DocumentEmbedding getEmbedding() { return embedding; }
    public void setEmbedding(DocumentEmbedding embedding) {
        this.embedding = embedding;
        if (embedding != null) {
            embedding.setDocument(this);
        }
    }

    // Builder
    public static class Builder {
        private final LegalDocument document = new LegalDocument();

        public Builder externalId(String externalId) {
            document.externalId = externalId;
            return this;
        }

        public Builder question(String question) {
            document.question = question;
            return this;
        }

        public Builder answer(String answer) {
            document.answer = answer;
            return this;
        }

        public Builder lawReference(String lawReference) {
            document.lawReference = lawReference;
            return this;
        }

        public Builder materia(String materia) {
            document.materia = materia;
            return this;
        }

        public Builder tags(Set<String> tags) {
            document.tags = new HashSet<>(tags);
            return this;
        }

        public Builder addTag(String tag) {
            document.tags.add(tag);
            return this;
        }

        public Builder semesterLevel(Integer semesterLevel) {
            document.semesterLevel = semesterLevel;
            return this;
        }

        public Builder source(String source) {
            document.source = source;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            document.sourceUrl = sourceUrl;
            return this;
        }

        public LegalDocument build() {
            if (document.externalId == null || document.question == null ||
                document.answer == null || document.source == null) {
                throw new IllegalStateException(
                    "externalId, question, answer, and source are required"
                );
            }
            return document;
        }
    }

    @Override
    public String toString() {
        return "LegalDocument{" +
               "id=" + id +
               ", externalId='" + externalId + '\'' +
               ", materia='" + materia + '\'' +
               ", source='" + source + '\'' +
               '}';
    }
}
