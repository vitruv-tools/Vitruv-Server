# Vitruv-Server
Vitruv Server

## Model endpoints

Besides views, the models of the virtual model can be read directly, e.g. for monitoring or to locate violating elements of constraints:

| Method | Path | Description |
|---|---|---|
| GET | `/vsum/models` | Lists the models: `id`, `metamodels` (`name`, `nsUri`), `rootCount` |
| GET | `/vsum/models/{id}` | Content of one model as JSON (`{ "uri", "content" }`), or as XMI with `Accept: application/xmi+xml` |
| POST | `/vsum/models/batch` | Content of several models from the same state, body `{ "ids": [...] }` (all if omitted) |
| GET | `/vsum/metamodels` | Lists the metamodels of the models: `name`, `nsUri` |
| GET | `/vsum/metamodels/{nsUri}` | One metamodel as `.ecore` XMI |

- the `id` of a model is its path relative to the folder of the virtual model, e.g. `/model/items.shop`, and equals the `resource` of constraint violations; in paths, ids and namespace URIs are URL-encoded, e.g. `/vsum/models/%2Fmodel%2Fitems.shop`
- every element carries its UUID in the virtual model, as `_id` in JSON and as `xmi:id` in XMI, so the `uuid` of a constraint violation identifies the element in the returned model
- references to elements of models that are not returned are written relative to the referencing model with the UUID as fragment, e.g. `other.shop#<uuid>`; references to metamodel elements use the namespace URI of the metamodel, so XMI models can be loaded with plain EMF together with the `.ecore` files
- the JSON content and the batch have the format of the `models` of a constraint validation
- unknown models or metamodels result in `404`

The Java client offers `getModelDescriptors()`, `getModelContents(ids)` (stored in the temporary folder of the client like the models of views), `getModelXmi(id)`, `getMetamodelDescriptors()` and `getMetamodelXmi(nsUri)`.

## Constraint endpoints

Constraints are registered at the virtual model via `VirtualModelBuilder.withConstraintChecker(...)`, e.g. with the `VitruvOclConstraintChecker` of VitruvOCL. The server exposes them independently of the constraint language.

| Method | Path | Description |
|---|---|---|
| GET | `/vsum/constraints` | Lists all constraints (`id`, `name`, `contextType`, `kind`, `severity`, `language`, `source`, `metamodels`) |
| GET | `/vsum/constraints/{id}` | Gets one constraint (`404` if unknown) |
| POST | `/vsum/constraints` | Creates a constraint, see [Managing constraints](#managing-constraints) |
| PUT | `/vsum/constraints/{id}` | Replaces the source of a constraint |
| DELETE | `/vsum/constraints/{id}` | Deletes a constraint (`204`) |
| GET | `/vsum/constraints/revision` | Gets the revision of the constraints |
| POST | `/vsum/constraints/validate` | Validates constraints, see below |
| GET | `/vsum/constraints/status` | Validates constraints and returns only their status, see [Status](#status) |
| GET | `/vsum/constraints/events` | Subscribes to validation results as Server-Sent Events, see [Events](#events) |

`metamodels` lists the names of the metamodels a constraint reads, the metamodel of its context type first, e.g. `["shop", "billing"]` for a constraint on `shop::Item` that navigates to `billing::Invoice`. Constraints only read models, so these are all metamodels whose models a constraint touches. The list is empty if the checker does not know them.

### Validation

The body of `POST /vsum/constraints/validate` is optional:

```json
{ "constraintIds": ["shop::Item::nonNegativePrice"], "includeModels": true }
```

- `constraintIds`: the constraints to validate; omitted or `null` validates all constraints, an empty list none
- `includeModels`: also return the models containing violating elements (default `false`); can also be given as query parameter, `POST /vsum/constraints/validate?includeModels` or `?includeModels=true|false`, which takes precedence over the body

Response:

```json
{
  "satisfied": false,
  "results": [
    {
      "constraintId": "shop::Item::nonNegativePrice",
      "name": "nonNegativePrice",
      "status": "VIOLATED",
      "severity": "CRITICAL",
      "violations": [
        {
          "uuid": "…",
          "eClass": "shop::Item",
          "resource": "/model/items.shop",
          "severity": "CRITICAL",
          "message": "Item \"coupon\" has a negative price"
        }
      ],
      "errors": []
    }
  ],
  "models": [ { "uri": "/model/items.shop", "content": { "…": "every element has its VSUM UUID as _id" } } ]
}
```

- `status` is `SATISFIED`, `VIOLATED` or `ERROR` (the constraint could not be checked, see `errors`)
- `uuid` is the UUID of the violating element in the virtual model; in `models`, every element carries its UUID as `_id`, and `resource` equals the `uri` of the model
- references to elements of models that are not returned are written relative to the referencing model with the UUID as fragment, e.g. `other.ecore#<uuid>`
- validation and the copy of the models are taken from the same state of the virtual model
- an unknown constraint id results in `404`, an empty id or a malformed body in `400`

The Java client provides the same operations via `VitruvClient.getConstraints()` and `VitruvClient.validateConstraints(...)`. Like the models of views, returned models are stored in the temporary folder of the client.

### Status

`GET /vsum/constraints/status` validates the constraints like `validate`, but returns only the status per constraint, so clients can cheaply poll the consistency of the virtual model:

```json
{
  "revision": "\"…\"",
  "satisfied": false,
  "constraints": {
    "shop::Item::nonNegativePrice": "VIOLATED",
    "shop::Item::cheap": "SATISFIED"
  }
}
```

- the optional, repeatable query parameter `id` restricts the validation, e.g. `?id=shop%3A%3AItem%3A%3Acheap`; unknown ids result in `404`, empty ones in `400`
- the constraints keep the order of the virtual model; `revision` is the revision of the constraints (also sent as `ETag`), which changes when constraints are created, updated or deleted, not when models change
- the Java client offers `VitruvClient.getConstraintStatus(ids)` (`null` for all constraints)

### Events

Instead of polling, clients can subscribe to the results of constraint validations as [Server-Sent Events](https://html.spec.whatwg.org/multipage/server-sent-events.html):

```bash
curl -N http://localhost:8080/vsum/constraints/events
```

```
event: validation
id: 1
data: { "revision" : "\"…\"", "satisfied" : false, "constraints" : { "shop::Item::cheap" : "VIOLATED" } }

event: constraints
id: 2
data: { "revision" : "\"…\"" }
```

- `validation` carries the same body as `GET /vsum/constraints/status`; it is sent right after subscribing, after every propagated change (e.g. a committed view), after every change of a constraint and whenever all constraints are validated on request
- `constraints` is sent when a constraint was created, updated or deleted, before the `validation` of the changed constraints
- while clients are subscribed, the virtual model validates its constraints after every propagated change; afterwards, its previous setting is restored (see `VirtualModelBuilder.withConstraintValidationAfterPropagation`)
- a comment is sent every 15 seconds to keep idle connections open; clients that cannot be reached are dropped, and stopping the server ends all subscriptions
- the Java client offers `VitruvClient.subscribeConstraintEvents(status -> …)`, which returns a `Subscription` to close

### Managing constraints

Constraints can be created, updated and deleted while the server is running. Changes take effect on the next validation and are stored in the VSUM folder, so they survive a restart. Constraints the VSUM was built with can be updated and deleted like any other constraint. Creating needs a checker that supports runtime changes for the language (an `EditableConstraintChecker`, e.g. the `VitruvOclConstraintChecker`).

```http
POST /vsum/constraints
{ "language": "vitruvocl", "source": "context shop::Item inv cheap: self.price < 1000" }
```

answers `201` with the created constraint and its path in the `Location` header, e.g. `/vsum/constraints/shop%3A%3AItem%3A%3Acheap`. The source must define exactly one constraint; its id is derived from the source.

```http
PUT /vsum/constraints/shop%3A%3AItem%3A%3Acheap
{ "source": "context shop::Item inv cheap: self.price < 500" }
```

answers `200` with the updated constraint. The new source must define a constraint with the same id.

- ids in paths are URL-encoded; `validate`, `revision`, `status` and `events` are reserved and cannot be addressed as ids
- errors have the body `{ "error": "…", "diagnostics": [] }`:
  - `400`: invalid source, with `diagnostics` like `{ "line": 1, "column": 34, "message": "…" }` (line from 1, column from 0), or a missing/malformed body
  - `404`: unknown id
  - `409`: a constraint with the id already exists, or the checker of the constraint does not support changes
  - `422`: no checker supports the language
- an invalid request never changes the constraints

Every response of `/vsum/constraints*` (except `validate`) carries the current revision of the constraints as `ETag` header. It changes whenever a constraint is created, updated or deleted, and only then, also across restarts. Clients can poll `GET /vsum/constraints/revision` (`{ "revision": "\"…\"" }`) to detect changes made by others. `PUT` and `DELETE` accept an `If-Match` header with a revision and answer `412` if the constraints changed since.

The Java client offers `getConstraint`, `createConstraint`, `updateConstraint`, `deleteConstraint` and `getConstraintRevision`; errors are thrown as `BadServerResponseException` with the status code.

## Postman

[`postman/Vitruv-Server.postman_collection.json`](postman/Vitruv-Server.postman_collection.json) contains a request for every model and constraint endpoint. Set the collection variable `baseUrl` to the address of the server; `constraintId`, `modelId` and `nsUri` are encoded for the paths automatically.
