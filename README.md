# HAGAP — Hummingbird AI Gateway & Assistant Platform

A Java Spring Boot backend wrapping OpenCode CLI with a React frontend, PostgreSQL, Qdrant vector DB — all containerized via Docker Compose.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────┐
│   React UI  │────▶│  Spring Boot │────▶│ OpenCode │
│  (Vite/TS)  │     │   Gateway    │     │   CLI    │
└─────────────┘     └──────┬───────┘     └──────────┘
                           │
                    ┌──────┴───────┐
                    │              │
              ┌─────┴─────┐ ┌─────┴─────┐
              │ PostgreSQL │ │  Qdrant   │
              │  (data)    │ │ (vectors) │
              └───────────┘ └───────────┘
```

### Clean Architecture Layers

- **Domain** — Entities, repositories, exceptions
- **Application** — DTOs, services, business logic
- **Infrastructure** — CLI execution, vector search, parsing, embedding
- **Presentation** — REST controllers, error handling

## Features

- Chat with AI via OpenCode CLI (sync + SSE streaming)
- Workspace isolation with path traversal protection
- Knowledge file upload with document parsing and chunking
- RAG pipeline: embedding → Qdrant vector search → prompt assembly → LLM
- Hallucination control with confidence scoring (0.75 threshold)
- Conversation history with session management

## Quick Start

```bash
docker compose up --build
```

Services:
- **Frontend**: http://localhost
- **Gateway API**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Qdrant**: localhost:6333

## Development

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Tests

```bash
cd backend
./mvnw test
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Synchronous chat |
| POST | `/api/chat/stream` | SSE streaming chat |
| POST | `/api/workspaces` | Create workspace |
| GET | `/api/workspaces` | List workspaces |
| GET | `/api/workspaces/{id}` | Get workspace |
| DELETE | `/api/workspaces/{id}` | Delete workspace |
| POST | `/api/workspaces/{id}/knowledge` | Upload knowledge file |
| GET | `/api/workspaces/{id}/sessions` | List sessions |
| GET | `/api/sessions/{id}/messages` | Get session messages |
| GET | `/api/health` | Health check |

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4, Maven, Flyway, Lombok
- **Frontend**: React, TypeScript, Vite
- **Database**: PostgreSQL 17
- **Vector DB**: Qdrant 1.13
- **Container**: Docker Compose
