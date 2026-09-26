# Inconsistency Hub — demo walkthrough

## Prerequisites

| Service | How |
|---------|-----|
| Postgres | `docker compose -f services/VitruviusServer-local/docker-compose.yml up -d` |
| VitruviusServer | rebuild jar, run on `:8000` |
| Frontend | `npm run dev` in `vitruvapp-web` → `:5173` |

Accounts:

| User | Password | Notes |
|------|----------|--------|
| `demo` | `demo` | Built-in |
| `tejas` | `Tejas@123!` | Sign up once if you want a second user |

---

## 1. Quick API check (optional)

```powershell
Invoke-RestMethod http://localhost:8000/api/v1/vsums
Invoke-RestMethod http://localhost:8000/api/v1/inconsistencies
Invoke-WebRequest http://localhost:5173/ -UseBasicParsing | Select-Object StatusCode
```

---

## Hub visualization & resolution

- `GET /v1/inconsistencies/{id}/model` — model snapshot for Hub visualization (prefers committed resource set; enriches missing entity from prompt)
- `GET /v1/inconsistencies/{id}/context` — elements and correspondences for the visualization panel
- `POST /v1/inconsistencies/{id}/resolution` — record resolver, choice, optional comment, and resolved timestamp

**Visualization UI behaviour:**

- Compact panels: model overview | Mermaid diagram | correspondences
- **Red** highlight = inconsistency; **blue** = click selection
- Correspondences: prefer live context edges; if empty (common for OPEN), infer from the snapshot by name/type
- If the prompt names `entity 'null'` and several unnamed Entities exist, all of them are highlighted in red
- **+ choice** is on the Commits / Visualization tab bar for OPEN items

---

## 2. Park → Hub → resolve (System Root)

1. Sign in as `demo` / `demo`.
2. Open **System Root Demo** (or create a new `SystemRootVsum`).
3. Add **Entity** under **Root** → **Update**.
4. When the component-type dialog appears, close it with **X** (do not answer).
5. Open **HUB** — the item should appear under **Open**.
6. Select it → open **Visualization** — the inconsistent entity should appear in the diagram with a **red** highlight. Click an element for **blue** selection and linked correspondences in the right panel.
7. **+ choice** (tab bar or detail) → pick e.g. **Device** → optional resolution comment → wait until it succeeds.
8. Check **Description** — resolution block shows date/time, resolver, choice, and comment.
9. **Open** should be empty; the item is under **Resolved**.
10. Go back to **Editor**, reopen the same VSUM, and check that the System-side component exists.

---

## 3. Comments from another user

1. Browser A (`demo`): park an item → Hub → **Comments** → add a comment.
2. Browser B (incognito): sign in as `tejas` / `Tejas@123!` (sign up first if needed).
3. Open the same item in Hub → **Comments** — demo’s comment should be there.
4. Reply as Tejas, switch back to demo, refresh — Tejas’s reply should show up.

Comments are stored on the server (Postgres), not only in the browser.

---

## 4. One open inconsistency per VSUM

1. Park one interaction on a VSUM (dialog **X**).
2. Reopen that VSUM and try **Update** again — it should be blocked until Hub resolve.
3. Resolve in Hub — Update works again.

---

## 5. Server restart caveat

1. Park an item and leave it unresolved.
2. Restart VitruviusServer.
3. Hub may still list OPEN rows, but **+ choice** can fail with `task not found`.

That is expected for now: propagation tasks are in memory only. For demos, resolve open items (or avoid restarting) before showing resolve.

---

## Automated tests

```powershell
cd VitruviusServer
mvn test "-Dtest=InconsistencyParkResolveIntegrationTest,InconsistencyCatalogIntegrationTest,AsyncTaskStatusInteractionTest"

cd ..\vitruvapp-web
npm run test:run -- src/modules/inconsistency-hub/
```

Also see [ASYNC-PROPAGATION.md](ASYNC-PROPAGATION.md).
