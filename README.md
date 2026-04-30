# java-buscacep-api

REST API for Brazilian CEP lookup using Spring Boot 4, Java 21, Maven, PostgreSQL, and Docker Compose.

## 1) Project Overview

Goal: expose a CEP lookup API with deterministic local behavior for technical interviews/challenges.

Provider strategy (planned contract):
1. Query WireMock first (local mocked source).
2. If not found, fallback to ViaCEP.
3. If not found in either provider, return `404`.

Flyway migrations run outside the application via Docker Compose.

## 2) Solution Architecture

High-level flow:
1. `GET /api/v1/ceps/{cep}`
2. Application use case invokes provider strategy/fallback chain
3. `WireMockCepProvider` (primary)
4. `ViaCepProvider` (secondary fallback)
5. Optional persistence in PostgreSQL

Architecture notes:
- Strategy/fallback mechanism isolates provider selection from controller/use case.
- Providers are separated behind ports/interfaces.
- Domain/application code does not depend directly on provider-specific HTTP details.

Intended package boundaries:
- `entrypoint` (controllers, request/response DTOs)
- `application` (use cases, ports, fallback orchestration)
- `domain` (entities/value objects/rules)
- `infrastructure` (JPA, HTTP clients, provider adapters)

## 3) Technologies Used

- Java 21
- Spring Boot 4
- Maven
- PostgreSQL (`kbdemiranda/postgres:multiarch`)
- Flyway container (`flyway/flyway:11-alpine`)
- WireMock (`wiremock/wiremock:3.13.1`)
- Docker Compose

## 4) Requirements

- Docker Desktop + Compose v2
- Java 21 (only if running app outside container)
- Maven 3.9+ (or `./mvnw`)

## 5) Run With Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Services started with one command:
- `postgres` (database)
- `flyway` (migration runner)
- `wiremock` (primary CEP mock source, exposed at `http://localhost:8081`)
- `app` (API at `http://localhost:8080`)

Stop:

```bash
docker compose down
```

Remove DB volume:

```bash
docker compose down -v
```

## 6) Run Locally (App outside Docker)

Start infra:

```bash
docker compose up -d postgres wiremock
docker compose run --rm flyway
```

Run app:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/buscacep \
SPRING_DATASOURCE_USERNAME=buscacep \
SPRING_DATASOURCE_PASSWORD=buscacep \
SPRING_FLYWAY_ENABLED=false \
CEP_CLIENT_WIREMOCK_URL=http://localhost:8081 \
CEP_CLIENT_VIACEP_URL=https://viacep.com.br/ws \
./mvnw spring-boot:run
```

## 7) Environment Variables

| Variable | Description | Example |
|---|---|---|
| `POSTGRES_DB` | PostgreSQL database name | `buscacep` |
| `POSTGRES_USER` | PostgreSQL username | `buscacep` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `buscacep` |
| `POSTGRES_PORT` | Exposed PostgreSQL port | `5432` |
| `SPRING_DATASOURCE_URL` | JDBC URL for app/Flyway | `jdbc:postgresql://postgres:5432/buscacep` |
| `SPRING_DATASOURCE_USERNAME` | JDBC username | `buscacep` |
| `SPRING_DATASOURCE_PASSWORD` | JDBC password | `buscacep` |
| `SPRING_FLYWAY_ENABLED` | Keep `false` (Flyway runs externally) | `false` |
| `CEP_CLIENT_WIREMOCK_URL` | Primary CEP provider base URL | `http://wiremock:8080` |
| `CEP_CLIENT_VIACEP_URL` | Secondary CEP provider base URL | `https://viacep.com.br/ws` |
| `APP_PORT` | API port mapping | `8080` |

## 8) WireMock Mappings

Mappings folder:

`wiremock/mappings`

Included example mapping:
- `GET /cep/12345678` returns `200` with CEP JSON.

To add a new mocked CEP:
1. Create a new JSON file under `wiremock/mappings`.
2. Define request path (`/cep/{cep}`) and response body/status.
3. Restart wiremock service (or full stack):

```bash
docker compose restart wiremock
```

## 9) API Examples (curl)

Health:

```bash
curl -i http://localhost:8080/actuator/health
```

CEP found in WireMock (primary):

```bash
curl -i http://localhost:8080/api/v1/ceps/12345678
```

CEP not found in WireMock, found in ViaCEP (fallback):

```bash
curl -i http://localhost:8080/api/v1/ceps/01001000
```

CEP not found anywhere (expected `404`):

```bash
curl -i http://localhost:8080/api/v1/ceps/00000000
```

Note: Java fallback business logic is not implemented yet; examples describe target behavior.

## 10) Database Migrations

Migration scripts directory:

`src/main/resources/db/migration`

Execution model:
- Flyway runs in dedicated container (`flyway` service).
- App starts only after Flyway migration succeeds.
- In-app Flyway auto-execution remains disabled (`SPRING_FLYWAY_ENABLED=false`).

## 11) Project Structure

```text
.
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── wiremock
│   └── mappings
│       └── cep-12345678.json
├── src
│   └── main
│       └── resources
│           └── db/migration
└── README.md
```

## 12) Suggested 15-Minute Interview Flow

1. Problem and scope (2 min)
2. Dual-provider strategy (WireMock first, ViaCEP fallback) (4 min)
3. Infra demo with single `docker compose up` (3 min)
4. API contracts and expected outcomes (3 min)
5. SOLID/clean architecture decisions and next steps (3 min)

## 13) SOLID and Clean Architecture Decisions

- `S`: provider adapters only handle provider integration concerns.
- `O`: new provider can be added without changing use-case contracts.
- `L`: all provider implementations respect the same lookup interface.
- `I`: focused ports (lookup/read/write separated).
- `D`: use cases depend on interfaces, not framework/provider classes.

Practical direction:
- Keep Spring and HTTP details in infrastructure layer.
- Keep fallback orchestration in application layer.
- Keep domain independent from external providers and persistence details.
