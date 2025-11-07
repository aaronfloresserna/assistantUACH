# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AILuisAmigo** is an academic legal assistant (RAG system) for law students at Universidad Autónoma de Chihuahua (UACH). The assistant answers questions about Mexican law using the Barcenas-Juridico-Mexicano dataset from Hugging Face, providing cited, pedagogical responses while avoiding hallucinations.

**Key Constraints:**
- Academic use only (CC BY-NC 4.0 license from dataset)
- MVP focused on functionality and maintainability for faculty demos
- Must NEVER invent legal references, articles, or jurisprudence
- All responses must be grounded in retrieved context or explicitly state insufficient information

## Architecture

### Proposed Tech Stack

**Backend (RAG API):**
- Java 21 + Spring Boot 3
- PostgreSQL + pgvector for vector storage
- Provider-agnostic LLM integration (OpenAI & Anthropic Claude)
- Embeddings: OpenAI text-embedding or compatible provider

**Frontend:**
- React / Next.js for student interface

**Infrastructure:**
- Docker + docker-compose for local development
- Environment variables for API keys (never hardcoded)

### Expected Directory Structure

```
/backend          # Spring Boot RAG API
/frontend         # React/Next.js UI
/infra            # docker-compose, database init scripts
/docs             # Architecture docs, prompts, technical decisions
```

## Core RAG Pipeline

### 1. Ingestion Pipeline (`POST /ingest`)

Source: `Danielbrdz/Barcenas-Juridico-Mexicano-Dataset` on Hugging Face

Expected data model:
```json
{
  "id": "...",
  "question": "...",
  "answer": "...",
  "source": "Barcenas-Juridico-Mexicano-Dataset",
  "law_reference": "Ley/Artículo/Tesis",
  "tags": ["materia", "tema", "nivel"]
}
```

Pipeline steps:
1. Load dataset from Hugging Face
2. Normalize to internal structure
3. Text cleaning and optional chunking for long answers
4. Generate embeddings
5. Persist to PostgreSQL with pgvector

Design must allow future addition of:
- New laws
- Official PDFs
- UACH-specific teaching materials

### 2. Query Pipeline (`POST /ask`)

Input parameters:
- `question` (required)
- `materia` (optional): constitucional, civil, penal, mercantil, amparo, etc.
- `semester_level` (optional): 1-10

Pipeline flow:
1. Generate embedding for question
2. Vector similarity search (k=5-10, filter by tags if provided)
3. Construct LLM prompt with retrieved context
4. Return structured response with sources

Response format:
```json
{
  "answer": "pedagogical explanation",
  "sources": [
    {
      "text": "fragment used",
      "reference": "Ley/Artículo/Tesis",
      "source": "dataset name"
    }
  ]
}
```

## Assistant Behavior Rules

### System Prompt Guidelines

The assistant prompt (store in `/docs/prompts/assistant_juridico_uach.md`) must enforce:

**Role:** Academic legal assistant for UACH law students

**Core Instructions:**
- Use EXCLUSIVELY the provided CONTEXT for answers
- Always cite laws/articles/criteria when present in context
- If context insufficient: explicitly state "Con la información disponible no puedo fundamentar con precisión jurídica."
- Provide pedagogical explanations: summary + extended explanation + practical example
- Clear, direct, respectful language (target audience: 18-25 year old students)
- No sales language or "coach" tone
- Always include disclaimer: "Esto es material académico y no constituye asesoría jurídica profesional."

**Strictly Forbidden:**
- Inventing article numbers, thesis numbers, or jurisprudence
- Responding from model's memory without grounding in context
- Distinguishing between norm, criteria, and opinion

## Backend API Endpoints

### Core Endpoints

- `POST /ask` - Query the legal assistant
- `POST /ingest/hf-barcenas` - Ingest Barcenas dataset
- `GET /sources/{id}` - Retrieve specific source document
- `GET /health` - Health check

## Code Architecture Principles

### Modularity

Backend should be organized in clear modules:
- `ingestion` - Dataset loading and processing
- `vector_store` - PostgreSQL + pgvector operations
- `llm_client` - LLM provider abstraction (interface-based)
- `embedding_client` - Embedding provider abstraction
- `rag_service` - Core RAG orchestration logic
- `web_api` - REST controllers

### Provider Abstraction

**Critical:** Use interfaces for external dependencies:
- `LLMClient` interface - implementations for OpenAI, Anthropic
- `EmbeddingClient` interface - swappable embedding providers

This ensures the system is not tightly coupled to any single provider.

### Database Schema (PostgreSQL)

Key tables needed:
- `qa_entries` or `documents` - stores question/answer pairs
- `embeddings` - vector embeddings with pgvector

Required fields:
- Text content
- Legal reference (law/article/thesis)
- Subject matter (materia)
- Tags (for filtering)
- Source attribution

## Development Context

### Language
Project documentation in Spanish. Code should use English for identifiers and comments, Spanish for user-facing content and legal domain terminology.

### Focus Areas
- Constitutional law (constitucional)
- Civil law (civil)
- Criminal law (penal)
- Commercial law (mercantil)
- Amparo (constitutional remedy)
- Other typical law school subjects

### Anti-Hallucination Strategy

1. Strong prompt engineering (see rules above)
2. Strict context-only responses
3. Optional: regex validation for legal references against known sources
4. Explicit "I don't know" responses when context insufficient

## Initial Setup Tasks

When starting development:
1. Initialize monorepo structure (backend/frontend/infra/docs)
2. Design PostgreSQL schema with pgvector support
3. Implement data models for QA entries and embeddings
4. Create LLMClient and EmbeddingClient interfaces
5. Build ingestion module for Hugging Face dataset
6. Define base system prompt in `/docs/prompts/assistant_juridico_uach.md`
7. Set up Docker Compose with PostgreSQL + pgvector
8. Implement REST endpoints
9. Create simple frontend for testing

## Important References

- Dataset: https://huggingface.co/datasets/Danielbrdz/Barcenas-Juridico-Mexicano-Dataset
- License: CC BY-NC 4.0 (academic use, attribution required, non-commercial)
- Project context: See `.agent/_contex.md` for detailed Spanish specifications
