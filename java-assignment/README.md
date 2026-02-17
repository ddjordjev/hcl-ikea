# Java Code Assignment — Warehouse Colocation Management

Quarkus 3.13, Java 17, Hibernate ORM with Panache, PostgreSQL 15.

Tasks and requirements: [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md)
Questions answers: [QUESTIONS.md](QUESTIONS.md)
Case study: [../case-study/CASE_STUDY.md](../case-study/CASE_STUDY.md)

## Prerequisites

- JDK 17+
- Maven 3.6+
- Docker (for PostgreSQL)

## Running locally

Start the database from the **repository root**:

```sh
docker-compose up -d
```

Then from `java-assignment/`:

```sh
# Dev mode with live reload
mvn quarkus:dev

# Or compile + run as jar
mvn package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

App runs at http://localhost:8080

## Running tests

```sh
mvn test           # unit tests only
mvn verify         # unit + integration tests + JaCoCo coverage check
```

Coverage report: `target/site/jacoco/index.html`
Coverage threshold: 80% on `location` and `warehouses.domain.usecases` packages.

## API overview

| Resource | Base path | Key operations |
|----------|-----------|----------------|
| Warehouse | `/warehouse` | CRUD + archive + replace (OpenAPI-generated) |
| Product | `/product` | CRUD |
| Store | `/store` | CRUD + partial update |
| Fulfillment | `/fulfillment` | Associate warehouses to products for stores |
| Health | `/q/health` | Liveness + readiness probes |

Fulfillment constraints: max 2 warehouses per product per store, max 3 warehouses per store, max 5 product types per warehouse.

## Database

PostgreSQL on port 15432 (`quarkus_test` / `quarkus_test` / `quarkus_test`).
Schema is `drop-and-create` — recreated on every startup with seed data from `import.sql`.

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`): compile, test, coverage check, health check.
