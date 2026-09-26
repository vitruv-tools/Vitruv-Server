# Async Change Propagation (VitruviusServer)

This document describes the async change propagation feature integrated into VitruviusServer, based on David Kowal's Vitruv-Server async pattern, adapted to Tejas Redkar's Spring Boot stack.

## Summary

Clients can propagate committed view changes **asynchronously** instead of blocking on `POST /apply-update`. The server returns a `taskId` immediately; the client polls task status until propagation completes.

**Important:** The core propagation logic is unchanged. Async work runs the same `ViewService.update()` as the synchronous endpoint. Postgres schema was **not** modified.

| | Sync (existing) | Async (new) |
|---|----------------|-------------|
| Start propagation | `POST /v1/views/{viewId}/apply-update` | `POST /v1/views/{viewId}/apply-update/async` |
| HTTP response | `200` + full result (blocks) | `202` + `{ "taskId": "..." }` |
| Get result | Same response body | Poll `GET /v1/tasks/{taskId}` |
| Persistence | Postgres `view_updates` + `./vsum-storage/` | **Same** + OPEN inconsistencies in Postgres table `open-inconsistencies` |
| Task state | N/A | In-memory only (`PropagationTaskRegistry`) |

## Prerequisites

1. **PostgreSQL** — start via Docker:

   ```bash
   docker compose -f services/VitruviusServer-local/docker-compose.yml up -d
   ```

2. **Build**

   ```bash
   mvn clean package
   ```

3. **Run server** (separate terminal)

   **Linux / macOS:**

   ```bash
   export $(grep -v '^#' local.env | xargs)
   java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar
   ```

   **Windows PowerShell:**

   ```powershell
   $env:SPRING_DATASOURCE_USERNAME = "postgres"
   $env:SPRING_DATASOURCE_PASSWORD = "password"
   $env:SPRING_DATASOURCE_HOST = "localhost:5432"
   $env:LOGS_PATH = "./logs/vitruvius-server.log"
   java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar
   ```

   Server base URL: `http://localhost:8000/api`

## API documentation

Async endpoints are documented in [`VitruviusServer-openapi.yaml`](VitruviusServer-openapi.yaml):

- `POST /v1/views/{viewId}/apply-update/async` → `202` + `StartAsyncUpdateResponse`
- `GET /v1/tasks/{taskId}` → `PropagationTaskStatusResponse`
- `POST /v1/tasks/{taskId}/interaction` → `204`
- `POST /v1/tasks/{taskId}/inconsistent` → park as open inconsistency (worker still waiting)
- `GET /v1/inconsistencies` → open inconsistencies only
- `GET /v1/inconsistencies/{id}` → one inconsistency

The synchronous `POST /v1/views/{viewId}/apply-update` remains available.

## Step 4: VsumProvider interaction wiring

When VitruviusServer loads a VSUM, `VsumManager` passes `VitruviusInteractionResultProvider` into
`VsumProvider.getInitializer(storagePath, interactionResultProvider)`. That provider:

- **Async propagation** (worker thread with task id): delegates to `ServerInteractionResultProvider` → REST polling + `POST /v1/tasks/{taskId}/interaction`
- **Sync propagation** (request thread, no task id): auto-confirms via `TestUserInteraction` (unchanged behaviour)

**Files:**

- `VsumProvider` API: optional `getInitializer(Path, InteractionResultProvider)` overload
- `VitruviusServer`: `VitruviusInteractionResultProvider.java`, `VsumManager.java`
- `vsum-provider-build`: all 8 provider main classes updated to use the injected provider

After changing providers, rebuild and redeploy JARs to `VitruviusServer/vsum-providers/`:

```bash
cd VsumProvider && mvn install
cd vsum-provider-build/vsum-provider/SystemRootVsumProvider && mvn clean package
# copy dist/target/* to VitruviusServer/vsum-providers/SystemRootVsum/
```

## Step 5: Frontend polling UI (`vitruvapp-web`)

The web app uses async propagation when the user clicks **Update**:

1. `POST /v1/views/{viewId}/apply-update/async` with the resource-set JSON body — commits and propagates on the worker thread (single call)
2. Poll `GET /v1/tasks/{taskId}` until `COMPLETED` or `FAILED`
3. If `WAITING_USER_INTERACTION` — show modal, then `POST /v1/tasks/{taskId}/interaction`

**Why commit runs inside async:** Vitruv reactions (including user prompts) fire when changes are committed to the VSUM, not only on `view.update()`. A separate synchronous `PUT` commit would hit `TestUserInteraction` before any task exists. Passing the resource set in the async POST ensures commit and update share the propagation worker thread and task id.

The async endpoint still accepts an empty body when changes were already committed via `PUT`.

**Files:**

- `vitruvapp-web/src/modules/model-editor/view/hooks/useAsyncPropagation.ts`
- `vitruvapp-web/src/modules/model-editor/view/hooks/useViewLogic.ts` — calls async propagation
- `vitruvapp-web/src/modules/model-editor/view/components/management/PropagationProgressOverlay.tsx`
- `vitruvapp-web/src/modules/model-editor/view/components/management/UserInteractionModal.tsx`
- `vitruvapp-web/src/modules/model-editor/types/api-propagation-task.ts`
- `vitruvapp-web/src/modules/model-editor/helpers/build-interaction-response.ts`

Run frontend: `cd vitruvapp-web && npm run dev` (set `VITE_VITRUVIUS_SERVER_BASE_URL=http://localhost:8000/api`).

## Manual user-interaction demo (InteractionDemoVsum)

For a **live browser demo** of `WAITING_USER_INTERACTION`, use the dedicated demo VSUM (same models as SystemRootVsum, but asks for confirmation when mirroring a new component):

- Build: `vsum-provider-build/vsum-provider/InteractionDemoVsumProvider` → `mvn clean package`
- Deploy `dist/target/InteractionDemoVsum/` to `VitruviusServer/vsum-providers/InteractionDemoVsum/` (+ `ecore-models/` from SystemRootVsum)
- Create VSUM with metamodel **`InteractionDemoVsum`**, add a component, click **Update**

See [`InteractionDemoVsumProvider/README.md`](../vsum-provider-build/vsum-provider/InteractionDemoVsumProvider/README.md) for step-by-step instructions and expected network calls.

## SystemRootVsum — user interactions

Metamodel **`SystemRootVsum`** supports three async user-interaction flows:

| Point | Trigger | Dialog |
|------:|---------|--------|
| 1 | Entity under **Root** (model2) | Single choice: Plain Component / Server / Device |
| 2 | Link under **Root** (model2) | Protocol multi-select or new protocol text input |
| 3 | Link under **System** (model) | Text input: speed in MBits/s (validated positive integer; invalid input shows error and allows retry) |

**Correct test paths:** Entity/Link under Root, or Link under System → async **Update**. Reverse reactions (e.g. Component under System) do not show dialogs.

**Text-input validation:** When reactions attach an `InputValidator`, `ServerInteractionResultProvider` rejects invalid input, exposes `validationError` on the task interaction payload, and re-prompts (`WAITING_USER_INTERACTION`) so the user can try again without failing the task.

Provider reactions:
- Point 1 & 2: `…/model2ToModelReactions.reactions`
- Point 3: `…/templateReactions.reactions` (`createAndInsertLink`)

### Automated tests (VitruviusServer)

Requires Postgres (`docker compose -f services/VitruviusServer-local/docker-compose.yml up -d`):

```powershell
cd VitruviusServer
mvn test "-Dtest=SystemRoot*InteractionPropagationTest"
```

This runs three integration tests (Entity, protocol text, link speed) tagged `systemroot-epic1`. Protocol multi-select and mode choice are covered by provider `LinkInteractionTest`.

Full documentation, known bugs, build/deploy, and provider unit tests:

[`SystemRootVsumProvider/README.md`](../vsum-provider-build/vsum-provider/SystemRootVsumProvider/README.md)

## Inconsistency Hub — park dismiss → resolve

Dismissing the interaction dialog (X) **parks** the waiting task; it does **not** abort the worker.

| Step | What happens |
|------|----------------|
| 1 | Editor shows `WAITING_USER_INTERACTION` |
| 2 | User closes dialog with X → `POST /v1/tasks/{taskId}/inconsistent` |
| 3 | Server creates an OPEN row in `open-inconsistencies`; worker stays waiting |
| 4 | Client unloads the editor view **without** closing the server view |
| 5 | Hub lists OPEN items via `GET /v1/inconsistencies` |
| 6 | User answers via `POST /v1/tasks/{taskId}/interaction` (same as editor) |
| 7 | Multi-step prompts (e.g. AmaltheaAscet) continue until `COMPLETED` |
| 8 | Server marks the row RESOLVED; item leaves the open list; VSUM is updated |

**Rules**

- At most **one OPEN** inconsistency per VSUM; new Update returns `409` while one is open.
- Tasks live **in memory** — a server restart drops parked workers (Hub rows may remain OPEN but are no longer resolvable).

### Manual E2E checklist

Full walkthrough (including comments from a second user and the restart caveat): **[HUB-E2E-DEMO.md](HUB-E2E-DEMO.md)**

Short version:

1. Postgres + server on `:8000` + frontend on `:5173`; sign in with `demo` / `demo`.
2. SystemRoot: Entity under Root → Update → close the dialog with **X** → **HUB** → **+ choice** → item becomes Resolved.
3. Optional: second browser user (sign up e.g. `tejas` / `Tejas@123!`) can see the same Hub comments.

Related tests:

```powershell
cd VitruviusServer
mvn test "-Dtest=InconsistencyParkResolveIntegrationTest,InconsistencyCatalogIntegrationTest,AsyncTaskStatusInteractionTest"
cd ..\vitruvapp-web
npm run test:run -- src/modules/model-editor/view/hooks/useAsyncPropagation.test.ts src/modules/inconsistency-hub/api/inconsistency-api.test.ts
```

### Hub catalog APIs (history, comments, commits)

| API | Purpose |
|-----|---------|
| `GET /v1/inconsistencies?state=OPEN\|RESOLVED\|FAILED\|ALL` | History filters (default OPEN) |
| `GET/POST /v1/inconsistencies/{id}/comments` | Conversation thread |
| `GET /v1/inconsistencies/{id}/view-updates` | Commits tab (VSUM view-update summaries) |

Hub UI: Open/Resolved/Failed/All chips, Conversation / Commits / Files tabs, **Answer / Resolve** for OPEN items (same parked-task flow).

## AmaltheaAscet — interactive consistency (Amalthea → ASCET)

Metamodel **`AmaltheaAscet`** is the CoCoPath / [UserInteractionDemo](https://github.com/boss-miriam/UserInteractionDemo/tree/main/amalthea-acset-integration) case study, packaged as a VitruviusServer VsumProvider.

| Trigger | Dialogs / effect |
|---------|------------------|
| Amalthea `Task` under `ComponentContainer` | Confirmation → ASCET task kind → (Periodic) period & delay |
| Amalthea `Task` deleted from container | Matching ASCET task removed (by name under corresponding `AscetModule`) |
| Edit only under `AscetModule` | No reverse propagation to Amalthea (one-way CPRs) |

Provider wiring matches other async-capable providers: `getInitializer(Path, InteractionResultProvider)` receives `VitruviusInteractionResultProvider` so async Update can pause on `WAITING_USER_INTERACTION`.

Build, deploy, and unit tests:

[`amalthea-acset-integration/README.md`](../amalthea-acset-integration/README.md)

```powershell
cd amalthea-acset-integration
mvn clean package
# copy dist/target/AmaltheaAscet → VitruviusServer/vsum-providers/AmaltheaAscet/
mvn test -pl vsum -am
```
