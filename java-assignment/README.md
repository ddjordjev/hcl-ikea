# Java Code Assignment — Warehouse Colocation Management

A simplified Warehouse colocation management system built with **Quarkus 3.13**, **Java 17**, **Hibernate ORM with Panache**, and **PostgreSQL 15**.

## About the assignment

Tasks and requirements are described in [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md).
Answers to the code-related questions are in [QUESTIONS.md](QUESTIONS.md).
The case study scenarios are discussed in [../case-study/CASE_STUDY.md](../case-study/CASE_STUDY.md).

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17+ | Ensure `JAVA_HOME` is set and `java` is on `PATH` |
| Maven | 3.6+ | Used to compile, test, and package the application |
| Docker | 20+ | Required for the PostgreSQL database |
| Docker Compose | v2+ | Ships with Docker Desktop; or install standalone |

## Quick start

### 1. Start the database

From the **repository root** (one level above this directory):

```sh
docker-compose up -d
```

This starts a PostgreSQL 15 container on **port 15432** with:
- Database: `quarkus_test`
- User: `quarkus_test`
- Password: `quarkus_test`

Alternatively, use the included setup script:

```sh
bash setup.sh
```

Verify the database is ready:

```sh
docker exec hcl-ikea-postgres pg_isready -U quarkus_test
```

### 2. Build the application

From the `java-assignment/` directory:

```sh
mvn compile
```

### 3. Run in dev mode (live reload)

```sh
mvn quarkus:dev
```

The app starts at [http://localhost:8080](http://localhost:8080). Code changes are hot-reloaded automatically, including JPA entity changes.

### 4. Run tests

```sh
# Unit tests only
mvn test

# Unit tests + integration tests + JaCoCo coverage check
mvn verify
```

The JaCoCo coverage report is generated at `target/site/jacoco/index.html`.
Coverage enforcement is set to **80% minimum** on the `location` and `warehouses.domain.usecases` packages.

### 5. Package and run in JVM mode

```sh
mvn package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

## API endpoints

Once the application is running, the following endpoints are available:

### Warehouse (`/warehouse`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/warehouse` | List all warehouses |
| `POST` | `/warehouse` | Create a new warehouse |
| `GET` | `/warehouse/{id}` | Get a warehouse by business unit code |
| `DELETE` | `/warehouse/{id}` | Archive a warehouse |
| `POST` | `/warehouse/{businessUnitCode}/replacement` | Replace a warehouse |

### Product (`/product`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/product` | List all products |
| `POST` | `/product` | Create a product |
| `GET` | `/product/{id}` | Get a product by ID |
| `PUT` | `/product/{id}` | Update a product |
| `DELETE` | `/product/{id}` | Delete a product |

### Store (`/store`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/store` | List all stores |
| `POST` | `/store` | Create a store |
| `GET` | `/store/{id}` | Get a store by ID |
| `PUT` | `/store/{id}` | Update a store |
| `PATCH` | `/store/{id}` | Partially update a store |
| `DELETE` | `/store/{id}` | Delete a store |

### Fulfillment assignments (`/fulfillment`)

Associates Warehouses as fulfillment units for Products to Stores.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/fulfillment` | List all assignments |
| `POST` | `/fulfillment` | Create an assignment |
| `DELETE` | `/fulfillment/{id}` | Delete an assignment |
| `GET` | `/fulfillment/store/{storeId}` | List assignments by store |
| `GET` | `/fulfillment/warehouse/{businessUnitCode}` | List assignments by warehouse |
| `GET` | `/fulfillment/product/{productId}` | List assignments by product |

**Business constraints:**
1. Each Product can be fulfilled by a maximum of **2** different Warehouses per Store
2. Each Store can be fulfilled by a maximum of **3** different Warehouses
3. Each Warehouse can store a maximum of **5** types of Products

### Health checks (`/q/health`)

| Path | Description |
|------|-------------|
| `/q/health` | Full health status |
| `/q/health/live` | Liveness probe |
| `/q/health/ready` | Readiness probe |

## Project structure

```
java-assignment/
├── src/main/java/com/fulfilment/application/monolith/
│   ├── location/          # LocationGateway — resolves valid warehouse locations
│   ├── stores/            # Store entity, REST resource, legacy system integration
│   ├── products/          # Product entity, repository, REST resource
│   ├── warehouses/        # Hexagonal architecture (ports, adapters, use cases)
│   │   ├── domain/
│   │   │   ├── models/    # Warehouse, Location domain models
│   │   │   ├── ports/     # Interfaces (WarehouseStore, LocationResolver, operations)
│   │   │   └── usecases/  # CreateWarehouse, ReplaceWarehouse, ArchiveWarehouse
│   │   └── adapters/
│   │       ├── database/  # DbWarehouse JPA entity, WarehouseRepository
│   │       └── restapi/   # WarehouseResourceImpl (OpenAPI-generated interface)
│   └── fulfillment/       # Fulfillment assignment entity, repository, REST resource
├── src/main/resources/
│   ├── application.properties   # Datasource and Hibernate configuration
│   ├── import.sql               # Seed data (3 stores, 3 products, 3 warehouses)
│   └── openapi/                 # Warehouse API OpenAPI spec (code generation source)
├── src/test/java/               # Unit tests and integration tests
└── pom.xml                      # Maven build with JaCoCo, Surefire, Failsafe
```

## Database configuration

The application connects to PostgreSQL at `localhost:15432/quarkus_test` by default. This is configured in `src/main/resources/application.properties`. The `docker-compose.yml` in the repository root matches these settings.

Hibernate is configured with `drop-and-create` — the schema is recreated on every startup and seed data from `import.sql` is loaded.

## CI/CD

A GitHub Actions pipeline is defined in `.github/workflows/ci.yml`. It runs on every push and pull request:
1. **Build & Test** — compiles, runs unit + integration tests, enforces JaCoCo coverage
2. **Health Check** — packages the app, starts it, and verifies all health and API endpoints respond

## Troubleshooting

- **`./mvnw` not found**: Use `mvn` directly (requires Maven installed), or install the Maven wrapper with `mvn wrapper:wrapper`.
- **Database connection refused**: Ensure the PostgreSQL container is running: `docker-compose up -d` from the repository root.
- **Port 15432 in use**: Stop the conflicting process or change the port in both `docker-compose.yml` and `application.properties`.
- **IntelliJ — generated code not recognized**: Add `target/generated-sources/src/main/java` as a "Generated Sources Root" in Project Structure.
