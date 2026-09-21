# Vitruvius Server

## Setup

- Use Java 17 (tested with Temurin 17)
- Run `mvn install` in `VsumProvider` so the interface is available locally
- Start Postgres: `VitruviusServer/services/VitruviusServer-local/docker-compose.yml`
- Start `VitruviusServer` (see `VitruviusServer/README.md`)
- Start `vitruvapp-web` (see `vitruvapp-web/README.md`)

Quick local check after everything is up:

- Frontend: http://localhost:5173
- API: http://localhost:8000/api/v1/vsums
- Login: `demo` / `demo`

## Start the Server

- Run `docker compose -f services/VitruviusServer-local/docker-compose.yml up -d`
- Run `mvn clean package`
- Set environment variables from `local.env` (e.g. `export $(grep -v '^#' local.env | xargs)` on unix)
- Run `java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar`

## Persist VSUMs (do not wipe)

VSUM **names/metadata** live in Postgres (`services/VitruviusServer-local/data/...`).  
VSUM **model files** live in `vsum-storage/`.

- Prefer `docker compose stop` / `up -d` — do **not** delete `services/**/data` and avoid `docker compose down -v`.
- If a reused folder contains JSON instead of XMI (`Content is not allowed in prolog`), the server moves it to `vsum-storage/.corrupt/` and recreates a clean folder. The last Postgres view snapshot is replayed when possible.
- Automated tests use a **separate** database `vitruvius-server-test` (`create-drop`) so `mvn test` does not wipe your real VSUMs.
- Backup: `.\scripts\backup-vsum-data.ps1`
- Recover list from orphan folders: `.\scripts\recover-vsums-from-storage.ps1`

## Documentation

- The design is described in the report of the practical course
- API documentation: `VitruviusServer-openapi.yaml` (sync and async propagation, inconsistencies, comments, model snapshot, resolution logging)
- **Async change propagation:** [ASYNC-PROPAGATION.md](ASYNC-PROPAGATION.md)
- **Inconsistency Hub demo steps:** [HUB-E2E-DEMO.md](HUB-E2E-DEMO.md)
- **AmaltheaAscet provider:** [`../amalthea-acset-integration/README.md`](../amalthea-acset-integration/README.md)

Failed interactive updates can leave the in-memory change recorder stuck. The server then evicts that VSUM (`VsumManager.evictVsum`); reopen the VSUM in the UI after such an error.

## Automated tests

Start Postgres first (`docker compose -f services/VitruviusServer-local/docker-compose.yml up -d`).

```powershell
# All server tests
mvn test

# Epic 1 SystemRootVsum async interaction tests
mvn test "-Dtest=SystemRoot*InteractionPropagationTest"

# Epic 2 Hub park / catalog / comments
mvn test "-Dtest=InconsistencyParkResolveIntegrationTest,InconsistencyCatalogIntegrationTest,AsyncTaskStatusInteractionTest"
```

Provider unit tests (includes LinkInteractionTest for Points 2 & 3):

```powershell
cd ..\vsum-provider-build\vsum-provider\SystemRootVsumProvider
mvn test
```

AmaltheaAscet unit tests (create dialogs + delete propagation):

```powershell
cd ..\amalthea-acset-integration
mvn test -pl vsum -am
```



- Metamodels are made available as VsumProvider in the `vsum-providers` directory
- The directory `all-vsum-providers` contains all VsumProviders created during the practical course
- For creating a new VsumProvider see the README of the `vsum-providers-build` project
- In addition to the `dist` of a VsumProvider built, the ecore-models must be added to the VsumProvider (see the example VsumProviders in `vsum-providers`)


- To use a Vitruv-Server, `vsum-providers` must contain a directory `VitruvServerVsum` that only contains the ecore-models of the running Vitruv-Server. The web-frontend will automatically detect if the server is not running and will not show it in this case. Currently, the Methodologist-Template ecore-models are present. The Methodologist-Template-Vitruv-Server is added to the repository.
