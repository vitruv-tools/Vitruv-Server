# Amalthea → ASCET (Vitruvius web stack)

Case study from [UserInteractionDemo / amalthea-acset-integration](https://github.com/boss-miriam/UserInteractionDemo/tree/main/amalthea-acset-integration)
([CoCoPath](http://www.jot.fm/contents/issue_2026_03/a9.html): concolic exploration of consistency-preserving paths with awaited user input).

Consistency is **one-way**: reactions respond to changes in **Amalthea** and update **ASCET**. There are no ASCET→Amalthea reverse reactions (same as upstream).

Metamodel name in the UI: **`AmaltheaAscet`**

---

## Modules

| Module | Role |
|--------|------|
| `model` | EMF metamodels `amalthea.ecore` / `ascet.ecore` + generated Java |
| `consistency` | Reactions DSL + generated routines (`Amalthea2ascetChangePropagationSpecification`) |
| `vsum` | `AmaltheaAscetVsumProvider`, CLI harness, unit tests |
| `dist` | Packages drop-in folder for `VitruviusServer/vsum-providers/AmaltheaAscet/` |

---

## Build & deploy

```powershell
cd VsumProvider
mvn install -DskipTests

cd ..\amalthea-acset-integration
mvn clean package

# Deploy into VitruviusServer
Remove-Item -Recurse -Force ..\VitruviusServer\vsum-providers\AmaltheaAscet -ErrorAction SilentlyContinue
Copy-Item -Recurse dist\target\AmaltheaAscet ..\VitruviusServer\vsum-providers\AmaltheaAscet
```

Deployed layout:

```
VitruviusServer/vsum-providers/AmaltheaAscet/
  vsum-provider.jar
  lib/amalthea-acset-model-*.jar
  lib/amalthea-acset-consistency-*.jar
  ecore-models/amalthea.ecore
  ecore-models/ascet.ecore
```

Restart VitruviusServer after redeploying so the new JAR is loaded. Prefer a **new** VSUM after provider or reaction changes; stale VSUMs may keep old correspondences or misplaced model files.

---

## Run Vitruvius (web)

Requires the **async-propagation** branch (async Update + user-interaction modal). See also [`VitruviusServer/ASYNC-PROPAGATION.md`](../VitruviusServer/ASYNC-PROPAGATION.md).

1. Postgres: `docker compose -f VitruviusServer/services/VitruviusServer-local/docker-compose.yml up -d`
2. Server (from `VitruviusServer/`):
   - Set env from `local.env` (`SPRING_DATASOURCE_*`, `LOGS_PATH`)
   - `mvn clean package -DskipTests`
   - `java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar`
3. Frontend: `cd vitruvapp-web && npm install && npm run dev`

Create a VSUM with metamodel **AmaltheaAscet**, open a view selecting **ComponentContainer** and **AscetModule**, edit, **Update**.

---

## Consistency behaviour

### Create (Amalthea → ASCET)

| Trigger | Effect |
|---------|--------|
| `ComponentContainer` inserted as root | Create `AscetModule`, persist as `example.model2`, bidirectional correspondence with the container |
| Amalthea `Task` inserted under `ComponentContainer.tasks` | User dialogs (see below), then create typed ASCET task under `AscetModule` |

**Correspondences (create routines):** ASCET task ↔ `ComponentContainer` (upstream pairing). Root: `ComponentContainer` ↔ `AscetModule` (both directions).

### Delete

| Trigger | Effect |
|---------|--------|
| Amalthea `Task` removed from `ComponentContainer.tasks` | Find `AscetModule` corresponding to the container; remove ASCET task with the **same name**; drop correspondence |
| Delete only inside `AscetModule` | ASCET task removed locally; **no** reverse update to Amalthea |

Give Amalthea tasks a non-null **name** in the UI so delete matching works reliably.

### User interaction (async Update)

VitruviusServer injects `VitruviusInteractionResultProvider` into `AmaltheaAscetVsumProvider.getInitializer(Path, InteractionResultProvider)`. Creating an Amalthea `Task` pauses for:

1. Confirmation — create corresponding ASCET task? (Yes / No)
2. Single choice — InitTask / PeriodicTask / SoftwareTask / TimeTableTask / Decide Later
3. If PeriodicTask — period and delay text inputs (`PositiveDoubleValidator`, ≥ 0)

Sync / no-arg initializer still auto-answers: yes → SoftwareTask → `"1.0"`.

CLI: `Test` + `CliInteractionResultProviderImpl`, or `mvn test -pl vsum -am`.

---

## Integration changes vs upstream UserInteractionDemo

Adaptations for this Vitruvius web stack (not present or incomplete upstream):

| Area | Change |
|------|--------|
| Packaging | `dist` module + `AmaltheaAscetVsumProvider` SPI (`META-INF/services/...VsumProvider`) |
| Vitruvius / SPI | Vitruvius 3.2.x; `getInitializer(Path, InteractionResultProvider)` for async web dialogs |
| Create UX | Confirmation (Yes/No) before task-type selection; InitTask option (upstream DSL lists InterruptTask) |
| Delete | Trigger on remove from `ComponentContainer.tasks`; resolve ASCET task via container + name (upstream delete looked up by Amalthea Task but create linked ASCET task to container) |
| Roots | Bidirectional `ComponentContainer` ↔ `AscetModule` correspondence |
| Storage | `ProjectMarker` + `fixAscetModuleInStorage` / `ensureContainerModuleCorrespondence` (same class of issue as SystemRoot `persistProjectRelative` writing next to the server cwd) |
| Server URI | Known model files `example.model` / `example.model2` handled in `VitruviusServer` `IdTransformation` (file-URI double-wrap fix) |

Reaction source: `consistency/.../amalthea2acset.reactions`. Runtime uses generated Java under `edu.neu.ccs.prl.galette.reactions.mir…`. Keep DSL and generated Java in sync when editing reactions.

---

## Tests

```powershell
cd amalthea-acset-integration
mvn test -pl vsum -am
```

`UserInputPropagationTest` covers:

- Confirmation **No** → no ASCET task
- Confirmation **Yes** + SoftwareTask
- Confirmation **Yes** + PeriodicTask with period/delay
- Delete Amalthea Task → corresponding ASCET task removed

VitruviusServer async integration tests for AmaltheaAscet (HTTP poll / interaction POST) are not included yet; SystemRoot has the pattern under `SystemRoot*InteractionPropagationTest`.

---

## Manual check (browser)

1. New VSUM, metamodel **AmaltheaAscet**
2. Open view with **ComponentContainer** and **AscetModule**
3. Add Amalthea Task, set **name**, Update → Yes → SoftwareTask (or Periodic) → both roots remain
4. Delete that Task from the container, Update → ASCET task with same name is gone
5. Delete a task only under AscetModule → Amalthea Task unchanged
