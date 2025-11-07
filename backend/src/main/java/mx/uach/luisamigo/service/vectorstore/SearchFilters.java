package mx.uach.luisamigo.service.vectorstore;

import java.util.Set;

/**
 * Filtros opcionales para búsqueda vectorial.
 * Permite refinar resultados por materia, tags, nivel, etc.
 */
public class SearchFilters {

    private final String materia;
    private final Set<String> tags;
    private final Integer semesterLevel;
    private final String sourceName;
    private final Double minSimilarityScore;

    private SearchFilters(Builder builder) {
        this.materia = builder.materia;
        this.tags = builder.tags;
        this.semesterLevel = builder.semesterLevel;
        this.sourceName = builder.sourceName;
        this.minSimilarityScore = builder.minSimilarityScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SearchFilters empty() {
        return new Builder().build();
    }

    // Getters
    public String getMateria() { return materia; }
    public Set<String> getTags() { return tags; }
    public Integer getSemesterLevel() { return semesterLevel; }
    public String getSourceName() { return sourceName; }
    public Double getMinSimilarityScore() { return minSimilarityScore; }

    public boolean hasFilters() {
        return materia != null ||
               (tags != null && !tags.isEmpty()) ||
               semesterLevel != null ||
               sourceName != null ||
               minSimilarityScore != null;
    }

    public static class Builder {
        private String materia;
        private Set<String> tags;
        private Integer semesterLevel;
        private String sourceName;
        private Double minSimilarityScore;

        public Builder materia(String materia) {
            this.materia = materia;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder semesterLevel(Integer semesterLevel) {
            this.semesterLevel = semesterLevel;
            return this;
        }

        public Builder sourceName(String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        public Builder minSimilarityScore(Double minSimilarityScore) {
            this.minSimilarityScore = minSimilarityScore;
            return this;
        }

        public SearchFilters build() {
            return new SearchFilters(this);
        }
    }
}
