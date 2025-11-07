# Índice de Archivos del Proyecto

Este documento lista todos los archivos creados en el diseño técnico del proyecto Luis Amigo.

## 📋 Archivos de Documentación

| Archivo | Propósito |
|---------|-----------|
| `README.md` | Documentación principal del proyecto |
| `CLAUDE.md` | Guía para Claude Code - contexto del proyecto |
| `docs/TECHNICAL_DESIGN_SUMMARY.md` | Resumen ejecutivo del diseño técnico completo |
| `docs/architecture/PACKAGE_STRUCTURE.md` | Estructura de paquetes Java detallada con responsabilidades |
| `docs/architecture/DATABASE_SCHEMA.md` | Esquema PostgreSQL + pgvector, queries y optimizaciones |
| `docs/prompts/assistant_juridico_uach.md` | Prompt base del asistente con reglas anti-hallucination |
| `docs/FILE_INDEX.md` | Este archivo - índice de todos los archivos |

## ⚙️ Archivos de Configuración

### Docker e Infraestructura

| Archivo | Propósito |
|---------|-----------|
| `docker-compose.yml` | Orquestación de servicios (PostgreSQL, Backend, Frontend) |
| `.env.example` | Template de variables de entorno |
| `.gitignore` | Archivos excluidos de Git |
| `backend/Dockerfile` | Imagen Docker multi-stage para Spring Boot |
| `frontend/Dockerfile` | Imagen Docker multi-stage para Next.js |
| `infra/postgres/init.sql` | Script de inicialización de PostgreSQL + pgvector |

### Backend (Spring Boot)

| Archivo | Propósito |
|---------|-----------|
| `backend/pom.xml` | Dependencias Maven y configuración de build |
| `backend/src/main/resources/application.yml` | Configuración Spring Boot (perfiles dev/prod) |

### Frontend (Next.js)

| Archivo | Propósito |
|---------|-----------|
| `frontend/package.json` | Dependencias NPM y scripts |

## 🔧 Código Backend (Java)

### Interfaces de Clientes (Abstracción de Proveedores)

| Archivo | Propósito |
|---------|-----------|
| `client/llm/LLMClient.java` | Interface para clientes LLM (OpenAI, Anthropic, etc.) |
| `client/llm/LLMConfig.java` | Configuración de llamadas LLM (temperatura, tokens, etc.) |
| `client/embedding/EmbeddingClient.java` | Interface para clientes de embeddings |

**Pendiente de implementar:**
- `OpenAIClient.java` - Implementación para GPT
- `AnthropicClient.java` - Implementación para Claude
- `OpenAIEmbeddingClient.java` - Implementación de embeddings
- `LLMClientFactory.java` - Factory para seleccionar implementación
- `EmbeddingClientFactory.java` - Factory para embeddings

### Entidades de Dominio (JPA)

| Archivo | Propósito |
|---------|-----------|
| `domain/LegalDocument.java` | Entidad JPA para documentos jurídicos (Q&A) |
| `domain/DocumentEmbedding.java` | Entidad JPA para embeddings vectoriales |

### Repositorios (JPA + pgvector)

| Archivo | Propósito |
|---------|-----------|
| `repository/LegalDocumentRepository.java` | Repositorio JPA con queries por materia, tags, etc. |
| `repository/DocumentEmbeddingRepository.java` | Repositorio con búsqueda vectorial (pgvector) |

### Servicios (Lógica de Negocio)

#### RAG Service

| Archivo | Propósito |
|---------|-----------|
| `service/rag/RAGService.java` | Interface del orquestador principal RAG |

**Pendiente de implementar:**
- `RAGServiceImpl.java` - Implementación del pipeline RAG completo
- `PromptBuilder.java` - Construcción de prompts con contexto
- `ResponseFormatter.java` - Formateo de respuestas con citas
- `HallucinationValidator.java` - Validación de referencias legales

#### Vector Store Service

| Archivo | Propósito |
|---------|-----------|
| `service/vectorstore/VectorStoreService.java` | Interface para operaciones vectoriales |
| `service/vectorstore/SearchFilters.java` | Clase de filtros para búsqueda vectorial |

**Pendiente de implementar:**
- `VectorStoreServiceImpl.java` - Implementación con pgvector
- `SimilaritySearchService.java` - Búsqueda por similitud optimizada

#### Ingestion Service

| Archivo | Propósito |
|---------|-----------|
| `service/ingestion/IngestionService.java` | Interface del pipeline de ingesta |
| `service/ingestion/IngestionConfig.java` | Configuración de ingesta (batch size, chunking) |
| `service/ingestion/IngestionResult.java` | Record con resultado de ingesta |
| `service/ingestion/ValidationResult.java` | Record con resultado de validación |
| `service/ingestion/IngestionEstimate.java` | Record con estimación de tiempo/costo |

**Pendiente de implementar:**
- `IngestionServiceImpl.java` - Orquestador de ingesta
- `DatasetLoader.java` - Carga desde Hugging Face
- `TextNormalizer.java` - Normalización de textos
- `ChunkingService.java` - División de textos largos

### DTOs (Data Transfer Objects)

#### Request DTOs

| Archivo | Propósito |
|---------|-----------|
| `dto/request/AskRequest.java` | Request para consultas al asistente |

**Pendiente de implementar:**
- `dto/request/IngestionRequest.java` - Request para ingesta

#### Response DTOs

| Archivo | Propósito |
|---------|-----------|
| `dto/response/AskResponse.java` | Response con respuesta del asistente |
| `dto/response/SourceReference.java` | Record con referencia a fuente citada |
| `dto/response/ErrorResponse.java` | Response estándar de error |

### Controllers (REST API)

**Pendiente de implementar:**
- `controller/AskController.java` - POST /api/ask
- `controller/IngestionController.java` - POST /api/ingest/*
- `controller/SourceController.java` - GET /api/sources/{id}
- `controller/HealthController.java` - GET /api/health

### Exception Handling

**Pendiente de implementar:**
- `exception/GlobalExceptionHandler.java` - @ControllerAdvice
- `exception/InsufficientContextException.java` - Contexto insuficiente
- `exception/LLMProviderException.java` - Error de proveedor LLM
- `exception/EmbeddingException.java` - Error al generar embeddings

### Utilidades

**Pendiente de implementar:**
- `util/TextCleaner.java` - Limpieza de textos
- `util/LegalReferenceParser.java` - Parseo de referencias legales

### Configuración Spring

**Pendiente de implementar:**
- `config/DatabaseConfig.java` - Configuración de datasource
- `config/VectorStoreConfig.java` - Configuración pgvector
- `config/SecurityConfig.java` - Configuración de seguridad

## 📊 Estadísticas

### Archivos Creados

- **Total de archivos**: 33
- **Archivos de documentación**: 7
- **Archivos de configuración**: 9
- **Interfaces Java**: 3
- **Entidades JPA**: 2
- **Repositorios**: 2
- **DTOs**: 4
- **Records/Config classes**: 5
- **Scripts SQL**: 1

### Archivos Pendientes de Implementación

**Estimación**: ~35 archivos adicionales de implementación

**Categorías:**
- 9 clientes (LLM, Embeddings + factories)
- 8 servicios (implementaciones + helpers)
- 4 controllers
- 4 exception handlers
- 3 configs
- 3 utilidades
- ~4 tests por cada componente crítico

## 🔄 Próximos Archivos a Crear (Orden Sugerido)

### Fase 1: Infraestructura Base
1. Validar que PostgreSQL levanta correctamente
2. Crear tests de conexión

### Fase 2: Clientes Externos
1. `client/llm/OpenAIClient.java`
2. `client/llm/AnthropicClient.java`
3. `client/llm/LLMClientFactory.java`
4. `client/embedding/OpenAIEmbeddingClient.java`
5. `client/embedding/EmbeddingClientFactory.java`
6. Tests unitarios para cada cliente

### Fase 3: Servicios Core
1. `service/vectorstore/VectorStoreServiceImpl.java`
2. `service/rag/PromptBuilder.java`
3. `service/rag/RAGServiceImpl.java`
4. `service/rag/ResponseFormatter.java`
5. Tests de integración

### Fase 4: Ingesta
1. `service/ingestion/DatasetLoader.java`
2. `service/ingestion/TextNormalizer.java`
3. `service/ingestion/ChunkingService.java`
4. `service/ingestion/IngestionServiceImpl.java`

### Fase 5: API REST
1. `controller/AskController.java`
2. `controller/IngestionController.java`
3. `exception/GlobalExceptionHandler.java`
4. Tests de API

### Fase 6: Frontend
1. Estructura básica Next.js
2. Componentes de chat
3. Integración con backend

## 📝 Notas

- Todos los archivos Java siguen convenciones de Spring Boot
- Estructura de paquetes sigue principios de Clean Architecture
- Separación clara entre interfaces y implementaciones
- DTOs separados de entidades de dominio
- Configuración por perfiles (dev/prod)

## 🔗 Referencias Cruzadas

- Ver `PACKAGE_STRUCTURE.md` para detalles de arquitectura
- Ver `DATABASE_SCHEMA.md` para esquema completo de DB
- Ver `TECHNICAL_DESIGN_SUMMARY.md` para roadmap de implementación
