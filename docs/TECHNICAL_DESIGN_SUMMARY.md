# Resumen del Diseño Técnico - Luis Amigo

## Estado del Proyecto

✅ **Diseño Técnico Completo**

El diseño técnico detallado del proyecto Luis Amigo está completo. Este documento resume la estructura, componentes y próximos pasos para implementación.

## Estructura del Repositorio

```
AILuisAmigo/
├── backend/                      # Spring Boot RAG API
│   ├── src/main/java/mx/uach/luisamigo/
│   │   ├── config/              # Configuración Spring
│   │   ├── domain/              # Entidades JPA
│   │   │   ├── LegalDocument.java
│   │   │   └── DocumentEmbedding.java
│   │   ├── repository/          # Repositorios JPA + pgvector
│   │   │   ├── LegalDocumentRepository.java
│   │   │   └── DocumentEmbeddingRepository.java
│   │   ├── service/             # Lógica de negocio
│   │   │   ├── ingestion/      # Pipeline de ingesta
│   │   │   ├── rag/            # Core RAG
│   │   │   └── vectorstore/    # Operaciones vectoriales
│   │   ├── client/              # Clientes externos (abstracción)
│   │   │   ├── llm/            # LLMClient interface + implementaciones
│   │   │   └── embedding/      # EmbeddingClient interface + implementaciones
│   │   ├── controller/          # REST endpoints
│   │   ├── dto/                 # Request/Response DTOs
│   │   ├── exception/           # Manejo de errores
│   │   └── util/                # Utilidades
│   ├── src/main/resources/
│   │   └── application.yml      # Configuración Spring Boot
│   ├── pom.xml                  # Maven dependencies
│   └── Dockerfile
│
├── frontend/                     # Next.js UI
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── infra/                        # Infraestructura
│   └── postgres/
│       └── init.sql             # Schema PostgreSQL + pgvector
│
├── docs/                         # Documentación
│   ├── architecture/
│   │   ├── PACKAGE_STRUCTURE.md      # Estructura de paquetes detallada
│   │   └── DATABASE_SCHEMA.md        # Esquema DB + queries
│   └── prompts/
│       └── assistant_juridico_uach.md  # Prompt base del asistente
│
├── .agent/
│   └── _contex.md               # Contexto original del proyecto
├── CLAUDE.md                     # Guía para Claude Code
├── docker-compose.yml            # Orquestación Docker
├── .env.example                  # Ejemplo de variables de entorno
└── .gitignore
```

## Componentes Clave

### 1. Interfaces Principales (Contratos)

#### LLMClient (`client/llm/LLMClient.java`)
```java
String generateResponse(String prompt, LLMConfig config);
boolean isAvailable();
String getProviderName();
```

**Implementaciones a desarrollar:**
- `OpenAIClient` - GPT-4, GPT-3.5
- `AnthropicClient` - Claude 3
- `LLMClientFactory` - Selector de implementación

#### EmbeddingClient (`client/embedding/EmbeddingClient.java`)
```java
float[] generateEmbedding(String text);
List<float[]> generateEmbeddings(List<String> texts);
int getDimensions();
```

**Implementaciones a desarrollar:**
- `OpenAIEmbeddingClient` - text-embedding-3-small
- `EmbeddingClientFactory` - Selector de implementación

### 2. Servicios Core

#### RAGService (`service/rag/RAGService.java`)
Orquestador principal del pipeline RAG:
1. Genera embedding de pregunta
2. Busca documentos similares
3. Construye prompt con contexto
4. Llama LLM
5. Formatea respuesta con citas

#### VectorStoreService (`service/vectorstore/VectorStoreService.java`)
Operaciones sobre PostgreSQL + pgvector:
- Búsqueda por similitud (top-K)
- Filtros por materia, tags, semester_level
- CRUD de embeddings

#### IngestionService (`service/ingestion/IngestionService.java`)
Pipeline de ingesta de datasets:
1. Carga desde Hugging Face
2. Normalización de texto
3. Chunking (opcional)
4. Generación de embeddings
5. Persistencia en DB

### 3. Modelo de Datos

#### legal_documents
```sql
- id (PK)
- external_id (unique)
- question, answer (TEXT)
- law_reference, materia
- tags (array)
- semester_level (1-10)
- source, source_url
```

#### document_embeddings
```sql
- id (PK)
- document_id (FK, unique)
- embedding (vector(1536))  -- pgvector
- model_name, model_provider
- embedding_version
```

**Índice:** HNSW para búsqueda vectorial eficiente

### 4. API REST Endpoints

```
POST   /api/ask                    # Consulta al asistente
POST   /api/ingest/hf-barcenas     # Ingesta dataset Barcenas
GET    /api/sources/{id}           # Obtener documento fuente
GET    /api/health                 # Health check
```

### 5. Configuración

**application.yml:**
- Datasource PostgreSQL
- Configuración LLM (OpenAI, Anthropic)
- Configuración Embeddings
- Parámetros RAG (top-k, min-similarity)
- Parámetros Ingestion (batch-size, chunking)

**docker-compose.yml:**
- PostgreSQL con pgvector
- Backend Spring Boot
- Frontend Next.js

## Flujos Principales

### Flujo de Ingesta
```
Hugging Face Dataset
  ↓
DatasetLoader
  ↓
TextNormalizer
  ↓
ChunkingService (si es necesario)
  ↓
EmbeddingClient (genera vectores)
  ↓
VectorStoreService (persiste en PostgreSQL)
```

### Flujo de Consulta (RAG)
```
Usuario → AskRequest
  ↓
RAGService
  ↓
EmbeddingClient (convierte pregunta a vector)
  ↓
VectorStoreService (búsqueda top-k similares)
  ↓
PromptBuilder (construye prompt con contexto)
  ↓
LLMClient (genera respuesta)
  ↓
ResponseFormatter (formatea con citas)
  ↓
HallucinationValidator (valida referencias)
  ↓
Usuario ← AskResponse
```

## Decisiones de Diseño Importantes

1. **Abstracción de Proveedores**: Interfaces para LLM y Embeddings permiten cambiar proveedor sin tocar lógica de negocio

2. **Separación de Responsabilidades**: Cada servicio tiene una responsabilidad única (SRP)

3. **DTOs vs Entidades**: Separación clara entre capa web y persistencia

4. **Validador de Alucinaciones**: Componente específico para validar referencias legales (requisito crítico)

5. **pgvector con HNSW**: Búsqueda vectorial eficiente para datasets pequeños/medianos (< 1M docs)

## Próximos Pasos para Implementación

### Fase 1: Infraestructura Base
1. ✅ Estructura de directorios creada
2. ✅ Interfaces definidas
3. ✅ Entidades JPA creadas
4. ✅ Repositorios definidos
5. ✅ Configuración Docker/Spring lista

**Siguiente:** Levantar infraestructura local
```bash
docker-compose up postgres
```

### Fase 2: Clientes Externos (LLM y Embeddings)
1. Implementar `OpenAIClient`
2. Implementar `AnthropicClient`
3. Implementar `OpenAIEmbeddingClient`
4. Implementar factories
5. Tests unitarios

### Fase 3: Servicios Core
1. Implementar `VectorStoreService`
2. Implementar `RAGService`
3. Implementar `PromptBuilder`
4. Implementar `ResponseFormatter`
5. Tests de integración

### Fase 4: Ingesta de Datos
1. Implementar `DatasetLoader` (Hugging Face API)
2. Implementar `TextNormalizer`
3. Implementar `ChunkingService`
4. Implementar `IngestionService`
5. Ingestar dataset Barcenas

### Fase 5: API REST
1. Implementar `AskController`
2. Implementar `IngestionController`
3. Implementar `GlobalExceptionHandler`
4. Documentación OpenAPI/Swagger
5. Tests de API

### Fase 6: Frontend
1. Crear interfaz básica de chat
2. Integrar con backend
3. Mostrar fuentes citadas
4. Diseño responsive

### Fase 7: Testing y Refinamiento
1. Tests end-to-end
2. Ajustar prompt para reducir alucinaciones
3. Optimizar búsqueda vectorial
4. Métricas de calidad de respuestas

## Comandos Útiles

### Desarrollo Local

```bash
# Iniciar PostgreSQL con pgvector
docker-compose up postgres

# Compilar backend
cd backend
mvn clean install

# Ejecutar backend
mvn spring-boot:run

# Instalar frontend
cd frontend
npm install

# Ejecutar frontend
npm run dev
```

### Testing

```bash
# Tests backend
mvn test

# Tests frontend
npm test
```

### Docker Completo

```bash
# Construir y levantar todo
docker-compose up --build

# Solo backend y DB
docker-compose up postgres backend
```

## Archivos de Referencia

- **Arquitectura de Paquetes**: `docs/architecture/PACKAGE_STRUCTURE.md`
- **Esquema de Base de Datos**: `docs/architecture/DATABASE_SCHEMA.md`
- **Prompt del Asistente**: `docs/prompts/assistant_juridico_uach.md`
- **Configuración Spring**: `backend/src/main/resources/application.yml`
- **Contexto Original**: `.agent/_contex.md`

## Estimaciones

- **Fase 1-2**: 1-2 días
- **Fase 3**: 2-3 días
- **Fase 4**: 2-3 días
- **Fase 5**: 1-2 días
- **Fase 6**: 2-3 días
- **Fase 7**: 2-3 días

**Total estimado**: 10-15 días de desarrollo para MVP funcional

## Preguntas Abiertas

1. ¿Cómo obtendremos acceso al dataset de Hugging Face? (API key, descarga manual, etc.)
2. ¿Qué modelo LLM usaremos por defecto? (GPT-4 vs Claude 3)
3. ¿Necesitamos autenticación de usuarios en el MVP?
4. ¿Qué métricas queremos trackear? (queries/día, costos API, calidad respuestas)

## Contacto y Soporte

Este diseño está listo para comenzar implementación. Cada fase puede desarrollarse incrementalmente con tests y demos continuas.
