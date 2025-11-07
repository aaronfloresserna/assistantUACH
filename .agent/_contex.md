Contexto del Proyecto: Asistente Jurídico UACH (RAG con Barcenas-Jurídico-Mexicano)
1. Visión
Construir un asistente jurídico académico para estudiantes de la Licenciatura en Derecho de la UACH.
El asistente:
Responde dudas sobre derecho mexicano usando fuentes reales (leyes, criterios, material del dataset).
Explica como profesor paciente:
lenguaje claro,
ejemplos prácticos,
enfoque a planes de estudio típicos (constitucional, civil, penal, mercantil, amparo, etc.).
Siempre:
cita fundamento (ley/artículo/criterio),
diferencia entre norma, criterio y opinión,
evita inventar artículos o tesis.
Primera versión: no producción masiva, MVP funcional, mantenible, listo para demos con la facultad.
2. Base de Conocimiento
Fuente inicial:
Dataset: Danielbrdz/Barcenas-Juridico-Mexicano-Dataset en Hugging Face. 
Hugging Face
Formato QA con foco en leyes mexicanas y documentos de la SCJN.
Licencia CC BY-NC 4.0 → uso académico, requiere atribución, no comercial. 
Hugging Face
Lineamientos:
Todo el pipeline debe estar preparado para agregar:
nuevas leyes,
PDFs oficiales,
resúmenes docentes,
material UACH específico.
El Asistente NUNCA debe responder “de memoria del modelo” si no hay contexto jurídico disponible:
si el contexto no sustenta, debe decirlo explícito.
3. Arquitectura Técnica (RAG – propuesta)
Stack propuesto (ajustable, pero usar esto como default):
Backend RAG API
Java 21 + Spring Boot 3
Endpoints REST:
POST /ask
POST /ingest
GET /health
Vector Store
PostgreSQL + pgvector
Embeddings
OpenAI text-embedding o equivalente compatible (dejar interfaz para cambiar proveedor).
LLM
Provider-agnostic:
Compatible con OpenAI (ChatGPT) y Anthropic (Claude).
El RAG API no se acopla a uno solo: interfaz LLMClient.
Frontend
React / Next.js: interfaz simple para estudiantes.
Infra
Docker + docker-compose para ambiente local.
Variables de entorno para llaves (no hardcode).
4. Flujo RAG Esperado
Ingesta
Descargar/cargar dataset de Hugging Face.
Normalizar a una estructura interna:
{
  "id": "...",
  "question": "...",
  "answer": "...",
  "source": "Barcenas-Juridico-Mexicano-Dataset",
  "law_reference": "Ley/Artículo/Tesis si existe",
  "tags": ["materia", "tema", "nivel"]
}
Aplicar:
limpieza de texto,
opcional: chunking (si respuestas largas),
cálculo de embeddings,
persistencia en PostgreSQL + pgvector.
Consulta (/ask)
Input:
question
opcional: materia (constitucional, civil, penal, etc.)
opcional: semester_level (1–10)
Pipeline:
Generar embedding de la pregunta.
Buscar en vector store (k=5–10, filtrando por tags si vienen).
Construir prompt al LLM:
Rol:
“Eres un asistente jurídico académico para estudiantes de Derecho de la UACH.”
Instrucciones clave:
Usa EXCLUSIVAMENTE el CONTEXTO para fundamentar.
Cita leyes/artículos/criterios cuando existan en contexto.
Si el contexto no es suficiente:
dilo: “Con la información disponible no puedo fundamentar con precisión jurídica.”
Explica de forma pedagógica.
Ofrece:
resumen corto,
explicación extendida,
ejemplo práctico.
Adjuntar fragmentos recuperados.
Respuesta:
answer: texto amigable.
sources: listado de fragmentos usados, con referencia.
Control de Alucinaciones
Prohibido:
inventar artículos, números de tesis o jurisprudencias.
Si el modelo inventa, el backend puede:
agregar instrucciones fuertes en el prompt,
opcional: validación básica (regex) para artículos vs. fuentes conocidas.
5. Reglas Funcionales del Asistente
Claude Code debe asumir esto como no-negociable:
Dominio
Enfocado en derecho mexicano.
Público objetivo: estudiantes de la UACH (18–25 años aprox).
Estilo
Claro, directo, respetuoso.
Nada de lenguaje vendedor ni “coach”.
Puede simplificar, pero siempre indicar fundamento cuando exista.
Seguridad / Descargo
Siempre incluir cláusula:
“Esto es material académico y no constituye asesoría jurídica profesional.”
Mantenibilidad
Código modular:
ingestion
vector_store
llm_client
rag_service
web_api
Fácil de cambiar proveedor de LLM o embeddings.
6. Tareas para Claude Code (Planificación Inicial)
Claude Code debe empezar por:
Definir estructura del repo (monorepo simple):
/backend (Spring Boot)
/frontend (React/Next.js)
/infra (docker-compose, db init)
/docs (arquitectura, prompts, decisiones técnicas)
Diseñar el modelo de datos en /backend:
Tabla documents o qa_entries
Tabla embeddings
Uso de pgvector
Campos para:
texto,
referencia legal,
materia,
tags,
fuente.
Especificar endpoints del backend (sin implementarlos todavía):
POST /ask
POST /ingest/hf-barcenas
GET /sources/{id}
GET /health
Diseñar módulo de ingesta:
Explicar cómo:
leer el dataset de Hugging Face,
mapearlo al modelo,
generar embeddings,
guardar en BD.
Diseñar interfaz LLMClient e EmbeddingClient:
Para poder:
enchufar OpenAI o Anthropic sin tocar el core.
Definir prompt base del asistente:
Guardarlo en /docs/prompts/assistant_juridico_uach.md
Incluyendo:
rol,
reglas,
estilo,
manejo de “no sé”,
formato de citas.