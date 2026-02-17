# HCL IKEA Test — Warehouse Colocation Management

## Repository structure

| Directory | Description |
|-----------|-------------|
| `java-assignment/` | Main application — Quarkus + Java 17 + PostgreSQL. See [java-assignment/README.md](java-assignment/README.md) for full setup and run instructions. |
| `case-study/` | Case study scenarios and domain briefing. See [case-study/CASE_STUDY.md](case-study/CASE_STUDY.md). |
| `.github/workflows/` | CI/CD pipeline (GitHub Actions). |

## Quick start

```sh
# 1. Start the PostgreSQL database
docker-compose up -d

# 2. Run the application in dev mode
cd java-assignment
mvn quarkus:dev
```

The app is available at [http://localhost:8080](http://localhost:8080).

For detailed instructions (building, testing, API reference), see **[java-assignment/README.md](java-assignment/README.md)**.
