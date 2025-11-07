# Estructura de Paquetes - Backend RAG API

## Visión General

El backend está organizado en capas siguiendo principios de Clean Architecture y Domain-Driven Design, con separación clara entre dominio, servicios, clientes externos y API REST.

## Estructura de Paquetes

```
mx.uach.luisamigo/
├── config/                    # Configuración de Spring Boot
│   ├── DatabaseConfig.java
│   ├── VectorStoreConfig.java
│   └── SecurityConfig.java
│
├── domain/                    # Entidades de dominio (JPA)
│   ├── LegalDocument.java
│   ├── DocumentEmbedding.java
│   └── QueryResult.java
│
├── repository/                # Repositorios JPA + pgvector
│   ├── LegalDocumentRepository.java
│   └── DocumentEmbeddingRepository.java
│
├── service/                   # Lógica de negocio
│   ├── ingestion/            # Pipeline de ingesta
│   │   ├── IngestionService.java
│   │   ├── DatasetLoader.java
│   │   ├── TextNormalizer.java
│   │   └── ChunkingService.java
│   │
│   ├── rag/                  # Core RAG pipeline
│   │   ├── RAGService.java
│   │   ├── PromptBuilder.java
│   │   ├── ResponseFormatter.java
│   │   └── HallucinationValidator.java
│   │
│   └── vectorstore/          # Operaciones de vector store
│       ├── VectorStoreService.java
│       └── SimilaritySearchService.java
│
├── client/                    # Clientes externos (abstracción)
│   ├── llm/                  # Clientes LLM
│   │   ├── LLMClient.java (interface)
│   │   ├── OpenAIClient.java
│   │   ├── AnthropicClient.java
│   │   └── LLMClientFactory.java
│   │
│   └── embedding/            # Clientes de embeddings
│       ├── EmbeddingClient.java (interface)
│       ├── OpenAIEmbeddingClient.java
│       └── EmbeddingClientFactory.java
│
├── controller/                # REST Controllers
│   ├── AskController.java
│   ├── IngestionController.java
│   ├── SourceController.java
│   └── HealthController.java
│
├── dto/                       # Data Transfer Objects
│   ├── request/
│   │   ├── AskRequest.java
│   │   └── IngestionRequest.java
│   └── response/
│       ├── AskResponse.java
│       ├── SourceReference.java
│       └── ErrorResponse.java
│
├── exception/                 # Manejo de excepciones
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientContextException.java
│   ├── LLMProviderException.java
│   └── EmbeddingException.java
│
└── util/                      # Utilidades
    ├── TextCleaner.java
    └── LegalReferenceParser.java
```

## Responsabilidades por Capa

### 1. Domain (`domain/`)
**Responsabilidad:** Entidades JPA que representan el modelo de datos persistente.

- `LegalDocument`: Documento jurídico (pregunta/respuesta del dataset)
- `DocumentEmbedding`: Vector embedding asociado a un documento
- `QueryResult`: Resultado interno de búsqueda vectorial

**Principios:**
- Entidades ricas con lógica de dominio mínima
- Anotaciones JPA para persistencia
- Inmutabilidad donde sea posible

### 2. Repository (`repository/`)
**Responsabilidad:** Acceso a datos con Spring Data JPA y operaciones pgvector.

- `LegalDocumentRepository`: CRUD + búsquedas por materia, tags
- `DocumentEmbeddingRepository`: Búsqueda por similitud vectorial (cosine similarity)

**Principios:**
- Interfaces que extienden JpaRepository
- Queries personalizadas con @Query para operaciones pgvector
- Métodos nombrados semánticamente

### 3. Service Layer (`service/`)

#### 3.1 Ingestion (`service/ingestion/`)
**Responsabilidad:** Pipeline completo de ingesta de datos.

- `IngestionService`: Orquestador principal de ingesta
- `DatasetLoader`: Carga dataset desde Hugging Face
- `TextNormalizer`: Limpieza y normalización de texto
- `ChunkingService`: División de textos largos en chunks

**Flujo:**
DatasetLoader → TextNormalizer → ChunkingService → EmbeddingClient → Repository

#### 3.2 RAG (`service/rag/`)
**Responsabilidad:** Pipeline RAG completo (retrieval + generation).

- `RAGService`: Orquestador principal del flujo RAG
- `PromptBuilder`: Construcción del prompt con contexto recuperado
- `ResponseFormatter`: Formateo de respuesta con citas
- `HallucinationValidator`: Validación básica de referencias legales

**Flujo:**
Query → EmbeddingClient → VectorStoreService → PromptBuilder → LLMClient → ResponseFormatter

#### 3.3 Vector Store (`service/vectorstore/`)
**Responsabilidad:** Operaciones sobre vector store.

- `VectorStoreService`: Operaciones CRUD en embeddings
- `SimilaritySearchService`: Búsqueda por similitud con filtros

**Operaciones clave:**
- Búsqueda k-NN con cosine similarity
- Filtrado por materia, tags, semester_level
- Ranking y deduplicación de resultados

### 4. Client Layer (`client/`)

#### 4.1 LLM Clients (`client/llm/`)
**Responsabilidad:** Abstracción de proveedores de LLM.

**Interface `LLMClient`:**
```java
String generateResponse(String prompt, LLMConfig config);
boolean isAvailable();
String getProviderName();
```

**Implementaciones:**
- `OpenAIClient`: Integración con OpenAI API (GPT-4, GPT-3.5)
- `AnthropicClient`: Integración con Anthropic API (Claude)
- `LLMClientFactory`: Factory para seleccionar implementación según configuración

**Principios:**
- Completamente intercambiables
- Manejo de rate limits y reintentos
- Logging de tokens y costos

#### 4.2 Embedding Clients (`client/embedding/`)
**Responsabilidad:** Abstracción de proveedores de embeddings.

**Interface `EmbeddingClient`:**
```java
float[] generateEmbedding(String text);
List<float[]> generateEmbeddings(List<String> texts);
int getDimensions();
String getModelName();
```

**Implementaciones:**
- `OpenAIEmbeddingClient`: text-embedding-3-small o similar
- `EmbeddingClientFactory`: Factory para seleccionar implementación

### 5. Controller Layer (`controller/`)
**Responsabilidad:** Endpoints REST y validación de entrada.

- `AskController`: POST /ask - Consulta al asistente
- `IngestionController`: POST /ingest/* - Ingesta de datos
- `SourceController`: GET /sources/{id} - Recuperar documento fuente
- `HealthController`: GET /health - Health check

**Principios:**
- Validación con Bean Validation (@Valid)
- Documentación con OpenAPI/Swagger
- Manejo de errores delegado a GlobalExceptionHandler

### 6. DTO Layer (`dto/`)
**Responsabilidad:** Contratos de API (request/response).

**Request DTOs:**
- `AskRequest`: question, materia (opcional), semester_level (opcional)
- `IngestionRequest`: dataset_name, batch_size, overwrite

**Response DTOs:**
- `AskResponse`: answer, sources[], metadata
- `SourceReference`: text, reference, source, law_reference
- `ErrorResponse`: error, message, timestamp

**Principios:**
- Separación clara entre DTOs y entidades de dominio
- Validaciones con anotaciones javax.validation
- Jackson para serialización JSON

### 7. Exception Layer (`exception/`)
**Responsabilidad:** Manejo centralizado de excepciones.

- `GlobalExceptionHandler`: @ControllerAdvice para manejo global
- `InsufficientContextException`: Contexto insuficiente para responder
- `LLMProviderException`: Error al llamar proveedor LLM
- `EmbeddingException`: Error al generar embeddings

**Principios:**
- Mapeo de excepciones a códigos HTTP apropiados
- Mensajes de error informativos pero seguros
- Logging de errores para debugging

## Flujos Principales

### Flujo de Ingesta
```
[Hugging Face]
    ↓
DatasetLoader (descarga dataset)
    ↓
TextNormalizer (limpia texto)
    ↓
ChunkingService (divide si es necesario)
    ↓
EmbeddingClient (genera vectors)
    ↓
VectorStoreService (persiste en DB)
```

### Flujo de Consulta (RAG)
```
[Usuario] → AskRequest
    ↓
RAGService.ask()
    ↓
EmbeddingClient (convierte pregunta a vector)
    ↓
SimilaritySearchService (busca top-k similares)
    ↓
PromptBuilder (construye prompt con contexto)
    ↓
LLMClient (genera respuesta)
    ↓
ResponseFormatter (formatea con citas)
    ↓
HallucinationValidator (valida referencias)
    ↓
[Usuario] ← AskResponse
```

## Decisiones de Diseño

### 1. Abstracción de Proveedores
**Decisión:** Interfaces para LLMClient y EmbeddingClient
**Razón:** Permite cambiar de proveedor sin modificar lógica de negocio
**Trade-off:** Más código inicial, pero flexibilidad a largo plazo

### 2. Separación RAG Service vs Vector Store Service
**Decisión:** Servicios separados para lógica RAG y operaciones vectoriales
**Razón:** Single Responsibility Principle, testabilidad
**Trade-off:** Más clases, pero mayor claridad

### 3. DTOs Separados de Entidades
**Decisión:** DTOs diferentes para API vs entidades JPA
**Razón:** Desacoplamiento entre capa web y persistencia
**Trade-off:** Mapeo manual/MapStruct, pero mayor control

### 4. Validador de Alucinaciones
**Decisión:** Componente específico para validar referencias legales
**Razón:** Anti-hallucination es requisito crítico del proyecto
**Trade-off:** Puede dar falsos positivos, pero reduce riesgo

## Próximos Pasos

1. Definir interfaces completas (LLMClient, EmbeddingClient)
2. Diseñar modelo de datos JPA y esquema PostgreSQL
3. Documentar contratos de API (OpenAPI)
4. Implementar por capas (domain → repository → service → controller)
