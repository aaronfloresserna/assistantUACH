# Esquema de Base de Datos - PostgreSQL + pgvector

## Visión General

El sistema usa PostgreSQL 15+ con la extensión **pgvector** para almacenar y buscar embeddings vectoriales de documentos jurídicos.

## Extensión pgvector

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

La extensión pgvector añade:
- Tipo de dato `vector(n)` para arrays de floats de dimensión fija
- Operadores de similitud:
  - `<->` : Distancia Euclidiana (L2)
  - `<#>` : Producto interno negativo
  - `<=>` : Distancia coseno (1 - similitud coseno)
- Índices: `ivfflat` y `hnsw` para búsqueda eficiente

## Tablas

### 1. legal_documents

Almacena documentos jurídicos (preguntas/respuestas del dataset).

```sql
CREATE TABLE legal_documents (
    id BIGSERIAL PRIMARY KEY,

    -- Identificador del dataset original
    external_id VARCHAR(255) UNIQUE NOT NULL,

    -- Contenido Q&A
    question TEXT NOT NULL,
    answer TEXT NOT NULL,

    -- Metadatos jurídicos
    law_reference VARCHAR(500),  -- e.g., "Artículo 123 CPEUM"
    materia VARCHAR(100),        -- e.g., "constitucional", "civil", "penal"
    tags TEXT[],                 -- Array de tags: ["derecho_laboral", "salario"]
    semester_level INT,          -- Nivel sugerido (1-10), NULL si aplica a todos

    -- Información de fuente
    source VARCHAR(255) NOT NULL, -- e.g., "Barcenas-Juridico-Mexicano-Dataset"
    source_url TEXT,

    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Índices
    CONSTRAINT chk_semester_level CHECK (semester_level IS NULL OR (semester_level >= 1 AND semester_level <= 10))
);

-- Índices para búsquedas frecuentes
CREATE INDEX idx_legal_documents_materia ON legal_documents(materia);
CREATE INDEX idx_legal_documents_source ON legal_documents(source);
CREATE INDEX idx_legal_documents_external_id ON legal_documents(external_id);
CREATE INDEX idx_legal_documents_tags ON legal_documents USING GIN(tags);
```

### 2. document_embeddings

Almacena embeddings vectoriales de los documentos.

```sql
CREATE TABLE document_embeddings (
    id BIGSERIAL PRIMARY KEY,

    -- Relación con documento
    document_id BIGINT NOT NULL UNIQUE,

    -- Vector embedding
    embedding vector(1536) NOT NULL,  -- Dimensión depende del modelo (1536 para text-embedding-3-small)

    -- Metadata del embedding
    model_name VARCHAR(100) NOT NULL,      -- e.g., "text-embedding-3-small"
    model_provider VARCHAR(50) NOT NULL,   -- e.g., "OpenAI"
    embedding_version INT NOT NULL DEFAULT 1,

    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign key
    CONSTRAINT fk_document FOREIGN KEY (document_id)
        REFERENCES legal_documents(id)
        ON DELETE CASCADE,

    -- Constraint para verificar dimensiones
    CONSTRAINT chk_embedding_dimensions CHECK (vector_dims(embedding) = 1536)
);

-- Índice para búsqueda vectorial (HNSW es más rápido que IVFFlat pero usa más memoria)
CREATE INDEX idx_document_embeddings_vector ON document_embeddings
USING hnsw (embedding vector_cosine_ops);

-- Índice alternativo con IVFFlat (más eficiente en memoria)
-- CREATE INDEX idx_document_embeddings_vector ON document_embeddings
-- USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

## Relaciones

```
legal_documents (1) -----> (1) document_embeddings
```

Relación uno-a-uno: cada documento tiene exactamente un embedding.

## Queries Principales

### Búsqueda por Similitud (Top-K)

```sql
-- Búsqueda simple top-5
SELECT
    ld.id,
    ld.question,
    ld.answer,
    ld.law_reference,
    (1 - (de.embedding <=> :query_vector)) as similarity_score
FROM document_embeddings de
JOIN legal_documents ld ON de.document_id = ld.id
ORDER BY de.embedding <=> :query_vector
LIMIT 5;
```

### Búsqueda con Filtro por Materia

```sql
SELECT
    ld.id,
    ld.question,
    ld.answer,
    ld.law_reference,
    (1 - (de.embedding <=> :query_vector)) as similarity_score
FROM document_embeddings de
JOIN legal_documents ld ON de.document_id = ld.id
WHERE ld.materia = :materia
ORDER BY de.embedding <=> :query_vector
LIMIT 5;
```

### Búsqueda con Múltiples Filtros

```sql
SELECT
    ld.id,
    ld.question,
    ld.answer,
    ld.law_reference,
    ld.materia,
    (1 - (de.embedding <=> :query_vector)) as similarity_score
FROM document_embeddings de
JOIN legal_documents ld ON de.document_id = ld.id
WHERE
    (:materia IS NULL OR ld.materia = :materia)
    AND (:semester_level IS NULL OR ld.semester_level IS NULL OR ld.semester_level <= :semester_level)
    AND (1 - (de.embedding <=> :query_vector)) >= :min_similarity
ORDER BY de.embedding <=> :query_vector
LIMIT :top_k;
```

### Búsqueda por Tags

```sql
SELECT
    ld.id,
    ld.question,
    ld.answer,
    (1 - (de.embedding <=> :query_vector)) as similarity_score
FROM document_embeddings de
JOIN legal_documents ld ON de.document_id = ld.id
WHERE :tag = ANY(ld.tags)
ORDER BY de.embedding <=> :query_vector
LIMIT 5;
```

## Optimización de Índices

### Consideraciones para HNSW vs IVFFlat

**HNSW (Hierarchical Navigable Small World):**
- Pros: Búsqueda más rápida, mejor recall
- Cons: Usa más memoria, construcción más lenta
- Recomendado para: Producción con < 1M vectores

**IVFFlat (Inverted File with Flat Compression):**
- Pros: Menos memoria, construcción más rápida
- Cons: Búsqueda más lenta, requiere tunear parámetro `lists`
- Recomendado para: Datasets grandes (> 1M vectores)

### Parámetros de Tunning

```sql
-- Para HNSW
-- m: número de conexiones por capa (default: 16, mayor = más precisión pero más lento)
-- ef_construction: tamaño de búsqueda durante construcción (default: 64)
CREATE INDEX idx_embeddings_hnsw ON document_embeddings
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Para IVFFlat
-- lists: número de clusters (recomendado: rows / 1000)
-- Con 10,000 documentos → lists = 100
CREATE INDEX idx_embeddings_ivfflat ON document_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Ajustar probes en tiempo de query para IVFFlat
SET ivfflat.probes = 10;  -- Mayor = más preciso pero más lento
```

## Estadísticas Útiles

```sql
-- Número de documentos por materia
SELECT materia, COUNT(*) as count
FROM legal_documents
GROUP BY materia
ORDER BY count DESC;

-- Número de documentos por fuente
SELECT source, COUNT(*) as count
FROM legal_documents
GROUP BY source;

-- Documentos sin referencias legales
SELECT COUNT(*)
FROM legal_documents
WHERE law_reference IS NULL;

-- Tags más comunes
SELECT tag, COUNT(*) as frequency
FROM legal_documents, UNNEST(tags) as tag
GROUP BY tag
ORDER BY frequency DESC
LIMIT 20;

-- Tamaño de la base de datos
SELECT
    pg_size_pretty(pg_total_relation_size('legal_documents')) as documents_size,
    pg_size_pretty(pg_total_relation_size('document_embeddings')) as embeddings_size;
```

## Migraciones

### Script de Inicialización

Ver: `/infra/postgres/init.sql`

### Orden de Ejecución

1. Crear extensión pgvector
2. Crear tabla legal_documents
3. Crear tabla document_embeddings
4. Crear índices
5. (Opcional) Cargar datos de prueba

## Consideraciones de Escalabilidad

### < 100K documentos
- HNSW con configuración default
- Sin particionamiento

### 100K - 1M documentos
- HNSW con m=16, ef_construction=128
- Considerar particionar por materia

### > 1M documentos
- IVFFlat con lists apropiado
- Particionamiento por materia o año
- Considerar múltiples réplicas de lectura

## Backups y Mantenimiento

```sql
-- Vacuum periódico (importante para pgvector)
VACUUM ANALYZE legal_documents;
VACUUM ANALYZE document_embeddings;

-- Reindex si performance degrada
REINDEX INDEX idx_document_embeddings_vector;

-- Backup
pg_dump -Fc -f luisamigo_backup.dump -d luisamigo
```

## Valores de Ejemplo

```sql
INSERT INTO legal_documents (
    external_id, question, answer, law_reference,
    materia, tags, semester_level, source
) VALUES (
    'barcenas_001',
    '¿Qué es el derecho al trabajo según la Constitución?',
    'El artículo 123 de la Constitución Política establece que toda persona tiene derecho al trabajo digno y socialmente útil...',
    'Artículo 123 CPEUM',
    'constitucional',
    ARRAY['derecho_laboral', 'derechos_sociales'],
    3,
    'Barcenas-Juridico-Mexicano-Dataset'
);

-- El embedding se insertará después vía la aplicación
```
