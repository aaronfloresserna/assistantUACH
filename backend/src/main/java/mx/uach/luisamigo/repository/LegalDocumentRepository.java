package mx.uach.luisamigo.repository;

import mx.uach.luisamigo.domain.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para documentos jurídicos.
 * Operaciones CRUD y búsquedas por metadatos.
 */
@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    /**
     * Busca documentos por materia.
     */
    List<LegalDocument> findByMateria(String materia);

    /**
     * Busca documentos por fuente (dataset).
     */
    List<LegalDocument> findBySource(String source);

    /**
     * Busca documentos que contengan un tag específico.
     */
    @Query("SELECT d FROM LegalDocument d WHERE :tag MEMBER OF d.tags")
    List<LegalDocument> findByTag(@Param("tag") String tag);

    /**
     * Busca documento por ID externo del dataset.
     */
    Optional<LegalDocument> findByExternalId(String externalId);

    /**
     * Verifica si existe un documento con el ID externo dado.
     */
    boolean existsByExternalId(String externalId);

    /**
     * Cuenta documentos por fuente.
     */
    long countBySource(String source);

    /**
     * Elimina todos los documentos de una fuente.
     */
    void deleteBySource(String source);

    /**
     * Busca documentos por materia y nivel de semestre.
     */
    @Query("SELECT d FROM LegalDocument d WHERE d.materia = :materia " +
           "AND (:semesterLevel IS NULL OR d.semesterLevel <= :semesterLevel)")
    List<LegalDocument> findByMateriaAndSemesterLevel(
        @Param("materia") String materia,
        @Param("semesterLevel") Integer semesterLevel
    );
}
