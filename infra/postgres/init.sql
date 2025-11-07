-- Inicialización de base de datos para Luis Amigo
-- PostgreSQL + pgvector

-- Habilitar extensión pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla de documentos jurídicos
CREATE TABLE IF NOT EXISTS legal_documents (
    id BIGSERIAL PRIMARY KEY,

    -- Identificador del dataset original
    external_id VARCHAR(255) UNIQUE NOT NULL,

    -- Contenido Q&A
    question TEXT NOT NULL,
    answer TEXT NOT NULL,

    -- Metadatos jurídicos
    law_reference VARCHAR(500),
    materia VARCHAR(100),
    semester_level INT,

    -- Información de fuente
    source VARCHAR(255) NOT NULL,
    source_url TEXT,

    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraint
    CONSTRAINT chk_semester_level CHECK (semester_level IS NULL OR (semester_level >= 1 AND semester_level <= 10))
);

-- Tabla para tags (relación many-to-many simple)
CREATE TABLE IF NOT EXISTS legal_document_tags (
    document_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (document_id, tag),
    FOREIGN KEY (document_id) REFERENCES legal_documents(id) ON DELETE CASCADE
);

-- Tabla de embeddings
CREATE TABLE IF NOT EXISTS document_embeddings (
    id BIGSERIAL PRIMARY KEY,

    -- Relación con documento
    document_id BIGINT NOT NULL UNIQUE,

    -- Vector embedding (1536 dimensiones para text-embedding-3-small)
    embedding vector(1536) NOT NULL,

    -- Metadata del embedding
    model_name VARCHAR(100) NOT NULL,
    model_provider VARCHAR(50) NOT NULL,
    embedding_version INT NOT NULL DEFAULT 1,

    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign key
    CONSTRAINT fk_document FOREIGN KEY (document_id)
        REFERENCES legal_documents(id)
        ON DELETE CASCADE
);

-- Índices para legal_documents
CREATE INDEX IF NOT EXISTS idx_legal_documents_materia ON legal_documents(materia);
CREATE INDEX IF NOT EXISTS idx_legal_documents_source ON legal_documents(source);
CREATE INDEX IF NOT EXISTS idx_legal_documents_external_id ON legal_documents(external_id);
CREATE INDEX IF NOT EXISTS idx_legal_document_tags_tag ON legal_document_tags(tag);

-- Índice vectorial para búsqueda por similitud
-- Usamos HNSW para mejor performance en datasets pequeños/medianos
CREATE INDEX IF NOT EXISTS idx_document_embeddings_vector ON document_embeddings
USING hnsw (embedding vector_cosine_ops);

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_legal_documents_updated_at
    BEFORE UPDATE ON legal_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Datos de ejemplo (opcional, para testing)
INSERT INTO legal_documents (external_id, question, answer, law_reference, materia, source, semester_level)
VALUES (
    'example_001',
    '¿Qué es el derecho al trabajo según la Constitución Mexicana?',
    'El artículo 123 de la Constitución Política de los Estados Unidos Mexicanos establece que toda persona tiene derecho al trabajo digno y socialmente útil. Este artículo regula las relaciones laborales entre patrones y trabajadores, estableciendo condiciones mínimas de trabajo como jornada máxima, salario mínimo, descansos, y protección de la maternidad.',
    'Artículo 123 CPEUM',
    'constitucional',
    'example-dataset',
    3
) ON CONFLICT (external_id) DO NOTHING;

INSERT INTO legal_document_tags (document_id, tag)
SELECT id, 'derecho_laboral' FROM legal_documents WHERE external_id = 'example_001'
ON CONFLICT DO NOTHING;

INSERT INTO legal_document_tags (document_id, tag)
SELECT id, 'derechos_sociales' FROM legal_documents WHERE external_id = 'example_001'
ON CONFLICT DO NOTHING;

-- Mensaje de éxito
DO $$
BEGIN
    RAISE NOTICE 'Base de datos Luis Amigo inicializada correctamente con pgvector';
END $$;
