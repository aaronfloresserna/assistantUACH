# Luis Amigo - Asistente Jurídico Académico UACH

![Status](https://img.shields.io/badge/status-diseño_técnico_completo-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+pgvector-blue)

Asistente jurídico académico basado en RAG (Retrieval-Augmented Generation) para estudiantes de la Licenciatura en Derecho de la Universidad Autónoma de Chihuahua.

## 🎯 Objetivo

Proporcionar un asistente que:
- Responde dudas sobre derecho mexicano con fuentes reales
- Explica conceptos jurídicos de forma pedagógica
- Siempre cita fundamentos legales (leyes, artículos, criterios)
- **NUNCA inventa referencias legales** (anti-hallucination)

## 📚 Base de Conocimiento

**Dataset**: [Barcenas-Juridico-Mexicano-Dataset](https://huggingface.co/datasets/Danielbrdz/Barcenas-Juridico-Mexicano-Dataset)
- Licencia: CC BY-NC 4.0 (uso académico)
- Contenido: Preguntas/respuestas sobre leyes mexicanas y documentos SCJN

## 🏗️ Arquitectura

### Stack Tecnológico

- **Backend**: Java 21 + Spring Boot 3
- **Base de Datos**: PostgreSQL 16 + pgvector
- **LLM**: OpenAI (GPT-4) / Anthropic (Claude) - Provider-agnostic
- **Embeddings**: OpenAI text-embedding-3-small
- **Frontend**: Next.js 14 + React 18
- **Infraestructura**: Docker + docker-compose

### Componentes Principales

```
┌─────────────┐
│   Frontend  │ (Next.js)
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│     Backend RAG API (Spring Boot)    │
│  ┌─────────────────────────────────┐ │
│  │  RAG Service (Orquestador)      │ │
│  └────┬──────────────┬──────────────┘ │
│       │              │                 │
│  ┌────▼─────┐  ┌────▼──────────┐     │
│  │ LLMClient│  │ EmbeddingClient│     │
│  │(abstrac.)│  │  (abstrac.)    │     │
│  └──────────┘  └────────────────┘     │
│       │              │                 │
│  ┌────▼──────────────▼──────────┐     │
│  │   VectorStoreService         │     │
│  └──────────┬───────────────────┘     │
└─────────────┼──────────────────────────┘
              │
        ┌─────▼─────┐
        │PostgreSQL │
        │+ pgvector │
        └───────────┘
```

## 📁 Estructura del Proyecto

```
AILuisAmigo/
├── backend/              # Spring Boot RAG API
│   ├── src/main/java/mx/uach/luisamigo/
│   │   ├── client/      # LLM y Embedding clients (abstracción)
│   │   ├── service/     # Lógica de negocio (RAG, VectorStore, Ingestion)
│   │   ├── domain/      # Entidades JPA
│   │   ├── repository/  # Repositorios JPA + pgvector
│   │   ├── controller/  # REST endpoints
│   │   └── dto/         # Request/Response DTOs
│   └── pom.xml
├── frontend/            # Next.js UI
├── infra/               # Docker y DB init scripts
├── docs/                # Documentación técnica
└── docker-compose.yml
```

## 🚀 Quick Start

### Prerrequisitos

- Docker y Docker Compose
- Java 21 (para desarrollo local)
- Node.js 18+ (para desarrollo local)
- API Keys: OpenAI y/o Anthropic

### 1. Configurar Variables de Entorno

```bash
cp .env.example .env
# Editar .env con tus API keys
```

### 2. Levantar Infraestructura

```bash
# Levantar PostgreSQL + pgvector
docker-compose up postgres

# O levantar todo el stack
docker-compose up --build
```

### 3. Acceder

- **Backend API**: http://localhost:8080/api
- **Frontend**: http://localhost:3000
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## 📖 Documentación

### Documentos Principales

- **[CLAUDE.md](./CLAUDE.md)** - Guía para Claude Code
- **[Diseño Técnico Completo](./docs/TECHNICAL_DESIGN_SUMMARY.md)** - Resumen de arquitectura y próximos pasos
- **[Estructura de Paquetes](./docs/architecture/PACKAGE_STRUCTURE.md)** - Arquitectura detallada del backend
- **[Esquema de Base de Datos](./docs/architecture/DATABASE_SCHEMA.md)** - Schema PostgreSQL + queries
- **[Prompt del Asistente](./docs/prompts/assistant_juridico_uach.md)** - Prompt base y reglas

### API Endpoints

```
POST   /api/ask                    # Consultar al asistente
POST   /api/ingest/hf-barcenas     # Ingestar dataset Barcenas
GET    /api/sources/{id}           # Obtener documento fuente
GET    /api/health                 # Health check
```

#### Ejemplo de Request

```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "¿Qué es el derecho al trabajo según la Constitución?",
    "materia": "constitucional",
    "topK": 5
  }'
```

## 🧪 Desarrollo

### Backend (Spring Boot)

```bash
cd backend

# Compilar
mvn clean install

# Ejecutar
mvn spring-boot:run

# Tests
mvn test
```

### Frontend (Next.js)

```bash
cd frontend

# Instalar dependencias
npm install

# Desarrollo
npm run dev

# Build producción
npm run build
```

## 📋 Estado del Proyecto

### ✅ Completado (Diseño Técnico)

- [x] Estructura de directorios
- [x] Interfaces principales (LLMClient, EmbeddingClient)
- [x] Entidades JPA y repositorios
- [x] Esquema de base de datos (PostgreSQL + pgvector)
- [x] Configuración Docker y Spring Boot
- [x] Definición de DTOs y contratos de API
- [x] Prompt base del asistente
- [x] Documentación de arquitectura

### 🚧 Próximas Fases de Implementación

**Fase 1**: Infraestructura Base
- Levantar PostgreSQL con pgvector
- Verificar conectividad

**Fase 2**: Clientes Externos
- Implementar OpenAIClient
- Implementar AnthropicClient
- Implementar OpenAIEmbeddingClient

**Fase 3**: Servicios Core
- Implementar VectorStoreService
- Implementar RAGService
- Implementar PromptBuilder

**Fase 4**: Ingesta de Datos
- Implementar DatasetLoader (Hugging Face)
- Implementar IngestionService
- Ingestar dataset Barcenas

**Fase 5**: API REST
- Implementar controllers
- Implementar exception handlers
- Documentación OpenAPI

**Fase 6**: Frontend
- Interfaz de chat
- Visualización de fuentes

## 🔒 Seguridad y Licencias

### Licencia del Dataset
- **Dataset Barcenas**: CC BY-NC 4.0
- Uso académico exclusivamente
- Requiere atribución
- No comercial

### Variables de Entorno
- **NUNCA** commits API keys al repositorio
- Usar `.env` (incluido en `.gitignore`)
- En producción: usar secrets management

## 🎓 Uso Académico

**Descargo de Responsabilidad**: Este sistema proporciona material académico y **NO constituye asesoría jurídica profesional**. Las respuestas son únicamente para fines educativos.

## 🤝 Contribución

Este es un proyecto académico para la UACH. Para contribuir:
1. Fork el repositorio
2. Crear rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## 📞 Contacto

- **Proyecto**: Luis Amigo - Asistente Jurídico UACH
- **Universidad**: Universidad Autónoma de Chihuahua
- **Facultad**: Licenciatura en Derecho

---

**Nota**: Este proyecto está en fase de diseño técnico. El diseño completo está listo para comenzar implementación por fases.
