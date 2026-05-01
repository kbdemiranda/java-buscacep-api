# Busca CEP API

Aplicação para consulta de CEP com Spring Boot 4 e Java 21, composta por API REST e interface web com Thymeleaf. O fluxo prioriza uma fonte local mockada (WireMock), usa ViaCEP como fallback e registra todas as consultas para auditoria.

## Visão geral

O projeto resolve um cenário comum de integração: manter previsibilidade em ambiente local sem abrir mão de uma fonte externa real. Para isso:

- consulta primeiro o WireMock;
- se não encontrar, tenta o ViaCEP;
- persiste o resultado da consulta (sucesso, não encontrado ou erro) no PostgreSQL.

## Demonstração

### Busca de CEP
<!-- TODO: Adicionar GIF da tela de busca aqui -->

### Histórico de Consultas
<!-- TODO: Adicionar GIF da tela de histórico aqui -->

## Diagramas

### Arquitetura geral
<!-- TODO: Adicionar diagrama Mermaid da arquitetura geral aqui -->

### Fluxo de consulta
<!-- TODO: Adicionar diagrama Mermaid de sequência aqui -->

## Tecnologias utilizadas

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker / Docker Compose
- WireMock
- Thymeleaf
- Tailwind CSS
- springdoc-openapi (Swagger)
- JUnit e Mockito (via `spring-boot-starter-test`)

## Funcionalidades implementadas

- Consulta de CEP: `GET /api/v1/zip-codes/{cep}`
- Histórico com paginação e filtros: `GET /api/v1/zip-code-queries`
- Registro de todas as consultas em banco
- Filtros por `cep`, `status`, `provider`, `dateFrom`, `dateTo`
- Interface web para busca e histórico
- Ações de cópia na interface:
  - cópia de campo individual
  - cópia do endereço consolidado
  - cópia do JSON no modal de detalhes do histórico
- Documentação Swagger/OpenAPI em `/swagger-ui.html`

Observação: o frontend atual está em tema claro; alternância dark/light não está implementada no código neste momento.

## Endpoints da API

Base local: `http://localhost:8080`

### `GET /api/v1/zip-codes/{cep}`

Consulta um CEP no formato `99999999` ou `99999-999`.

Parâmetro de rota:
- `cep` (obrigatório): CEP com 8 dígitos, com ou sem máscara.

Exemplos:

```bash
curl -i http://localhost:8080/api/v1/zip-codes/04364030
curl -i http://localhost:8080/api/v1/zip-codes/04364-030
```

Respostas esperadas:
- `200 OK`: CEP encontrado
- `400 Bad Request`: CEP inválido
- `404 Not Found`: CEP não encontrado
- `502 Bad Gateway`: falha em integração externa

### `GET /api/v1/zip-code-queries`

Lista histórico de consultas com paginação e filtros.

Parâmetros de query:
- `page` (opcional, padrão `0`)
- `size` (opcional, padrão `10`)
- `cep` (opcional)
- `status` (opcional): `SUCCESS`, `NOT_FOUND`, `ERROR`
- `provider` (opcional): `WIREMOCK`, `VIACEP`
- `dateFrom` (opcional, ISO-8601): ex. `2026-04-01T00:00:00`
- `dateTo` (opcional, ISO-8601): ex. `2026-04-30T23:59:59`

Exemplos:

```bash
curl -G "http://localhost:8080/api/v1/zip-code-queries"

curl -G "http://localhost:8080/api/v1/zip-code-queries" \
  --data-urlencode "page=0" \
  --data-urlencode "size=5" \
  --data-urlencode "provider=WIREMOCK" \
  --data-urlencode "status=SUCCESS"

curl -G "http://localhost:8080/api/v1/zip-code-queries" \
  --data-urlencode "cep=04364030" \
  --data-urlencode "dateFrom=2026-04-01T00:00:00" \
  --data-urlencode "dateTo=2026-04-30T23:59:59"
```

## Execução

Subir tudo com Docker Compose:

```bash
cp .env.example .env
docker compose up --build
```

Serviços:
- aplicação: `http://localhost:8080`
- PostgreSQL: `localhost:45432`
- WireMock: `http://localhost:8081`

Parar:

```bash
docker compose down
```

Parar e remover volume do banco:

```bash
docker compose down -v
```

## Variáveis de ambiente

| Variável | Descrição | Exemplo |
|---|---|---|
| `POSTGRES_DB` | Nome do banco PostgreSQL | `buscacep` |
| `POSTGRES_USER` | Usuário do PostgreSQL | `buscacep` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `buscacep` |
| `SPRING_DATASOURCE_URL` | URL JDBC da aplicação | `jdbc:postgresql://postgres:5432/buscacep` |
| `SPRING_DATASOURCE_USERNAME` | Usuário JDBC | `buscacep` |
| `SPRING_DATASOURCE_PASSWORD` | Senha JDBC | `buscacep` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Estratégia de schema do Hibernate | `validate` |
| `SPRING_FLYWAY_ENABLED` | Habilita Flyway no startup | `true` |
| `CEP_CLIENT_WIREMOCK_URL` | URL base do WireMock | `http://wiremock:8080` |
| `CEP_CLIENT_VIACEP_URL` | URL base do ViaCEP | `https://viacep.com.br/ws` |
| `APP_PORT` | Porta publicada da aplicação | `8080` |

## Decisões técnicas

- **WireMock como provedor primário:** garante previsibilidade e controle em ambiente local e testes de desafio técnico.
- **ViaCEP como fallback:** mantém cobertura para CEPs não mapeados no mock, sem interromper a experiência da API.
- **`BIGSERIAL` interno + `external_id` UUID:** `id` numérico facilita indexação e ordenação interna; `external_id` expõe identificador estável e seguro para consumo externo.
- **`JSONB` em `response_body`:** permite armazenar o payload retornado de forma estruturada e flexível.
- **`PageResponse` próprio:** evita expor `PageImpl` diretamente, desacopla contrato HTTP de detalhes internos do Spring Data e mantém resposta estável.

## Princípios SOLID aplicados

- **SRP (Single Responsibility Principle):**
  - controllers tratam entrada/saída HTTP;
  - service concentra regras de consulta, fallback e persistência;
  - clients encapsulam integração com cada provedor.
- **DIP (Dependency Inversion Principle):**
  - `CepService` depende de abstrações de cliente (`CepClient`) e não de implementação concreta de HTTP.
- **OCP (Open/Closed Principle):**
  - a estratégia de provedores permite extensão com novos clients sem alterar o contrato público da API.

## Estrutura do projeto

```text
.
├── Dockerfile
├── docker-compose.yml
├── docker-compose.mock.yml
├── .env.example
├── wiremock/
│   └── mappings/
├── src/
│   ├── main/
│   │   ├── java/io/github/kbdemiranda/buscacep/
│   │   │   ├── client/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/migration/
│   │       └── templates/
│   └── test/
│       └── java/io/github/kbdemiranda/buscacep/
└── pom.xml
```

## URLs úteis

- Busca: `http://localhost:8080/`
- Histórico: `http://localhost:8080/history`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testes

```bash
./mvnw test
```
