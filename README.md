# Vitruvius Server

Spring Boot backend for VSUM management, async change propagation, and the Inconsistency Hub.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker

## Databases

| Database | Purpose |
|----------|---------|
| `vitruvius-server` | Runtime (`local.env` + `application.yaml`) |
| `vitruvius-server-test` | Tests (`application-test.yaml`) |

Create the test database with `scripts/ensure-test-db.ps1` or `scripts/ensure-test-db.sh` after Postgres is up.  
`services/VitruviusServer-local/init-db/01-create-test-db.sql` also creates it when the Postgres volume is initialized for the first time.

## Start

```powershell
cd ..\VsumProvider
mvn install -DskipTests
cd ..\VitruviusServer

docker compose -f services/VitruviusServer-local/docker-compose.yml up -d
.\scripts\ensure-test-db.ps1
mvn clean package
# set SPRING_DATASOURCE_* and LOGS_PATH from local.env
java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar
```

```bash
cd ../VsumProvider && mvn install -DskipTests
cd ../VitruviusServer
docker compose -f services/VitruviusServer-local/docker-compose.yml up -d
bash scripts/ensure-test-db.sh
mvn clean package
# export vars from local.env
java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar
```

API base: `http://localhost:8000/api`

## Data

- Postgres: VSUM metadata, Hub rows, comments (`services/VitruviusServer-local/data/…`)
- Disk: model files under `vsum-storage/`
- Prefer `docker compose stop` / `up -d` — avoid `down -v` (wipes the DB volume)
- Backup: `.\scripts\backup-vsum-data.ps1`

Providers live in `vsum-providers/` (JAR + `lib/` + `ecore-models/`).  
Tests use `./remote/src/test/resources/vsum-providers/`.

## Tests

```powershell
cd ..\VsumProvider
mvn install -DskipTests
cd ..\VitruviusServer

docker compose -f services/VitruviusServer-local/docker-compose.yml up -d
.\scripts\ensure-test-db.ps1
mvn test
```

```powershell
mvn test "-Dtest=SystemRoot*InteractionPropagationTest"
mvn test "-Dtest=InconsistencyParkResolveIntegrationTest,InconsistencyCatalogIntegrationTest,AsyncTaskStatusInteractionTest"
```

Provider tests:

```powershell
cd ..\vsum-provider-build\vsum-provider\SystemRootVsumProvider
mvn test
```

## How To Use

**Create a new VsumProvider from an existing one as a Template:**

- [ ] Ensure you have the dependency `de.niklaskerkhoff.vsumprovider:VsumProvider:1.1-SNAPSHOT` installed locally via `mvn install`. It is available in my GitLab repository in `niklaskerkhoff/VsumProvider`.
- [ ] Create a new Vsum-Vitruv-Server instance using the Vitruv-CLI
- [ ] Create a new module `dist`
- [ ] Copy the root pom `/pom.xml` to the new project
- [ ] Copy the vsum-module pom `/vsum/pom.xml` to the new project
- [ ] Copy the dist-module pom `/dist/pom.xml` to the new project
- [ ] Change the artifactId in `/pom.xml`, `/vsum/pom.xml` and `/dist/pom.xml`
- [ ] Adjust the code marked with the comment `!!!CHANGE!!!` in `/dist/pom.xml`
- [ ] Replace the main class in `/vsum/src/main/java` with the VsumProvider class
- [ ]  Adjust the package name, class name and imports of the VsumProviderClass
- [ ] Adjust the code marked with the comment `!!!CHANGE!!!` in the VsumProviderClass
- [ ] Copy the file `/vsum/src/main/resources/META-INF/services/de.niklaskerkhoff.vsumprovider.VsumProvider` to the new project and adjust the content according to the package and classname of the VsumProvider class
- [ ] Run `mvn clean package`
- [ ] The new VsumProvider is located in `/dist/target`


## Docs

- OpenAPI: `VitruviusServer-openapi.yaml`
- Async propagation: [ASYNC-PROPAGATION.md](ASYNC-PROPAGATION.md)
- Hub demo: [HUB-E2E-DEMO.md](HUB-E2E-DEMO.md)
- AmaltheaAscet: [`../amalthea-acset-integration/README.md`](../amalthea-acset-integration/README.md)
