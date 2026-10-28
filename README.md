# Vitruv-Server
Vitruv Server

## Constraint endpoints

Constraints are registered at the virtual model via `VirtualModelBuilder.withConstraintChecker(...)`, e.g. with the `VitruvOclConstraintChecker` of VitruvOCL. The server exposes them independently of the constraint language.

| Method | Path | Description |
|---|---|---|
| GET | `/vsum/constraints` | Lists all constraints (`id`, `name`, `contextType`, `kind`, `severity`, `language`, `source`) |
| GET | `/vsum/constraints/{id}` | Gets one constraint (`404` if unknown) |
| POST | `/vsum/constraints` | Creates a constraint, see [Managing constraints](#managing-constraints) |
| PUT | `/vsum/constraints/{id}` | Replaces the source of a constraint |
| DELETE | `/vsum/constraints/{id}` | Deletes a constraint (`204`) |
| GET | `/vsum/constraints/revision` | Gets the revision of the constraints |
| POST | `/vsum/constraints/validate` | Validates constraints, see below |

The body of `POST /vsum/constraints/validate` is optional:

```json
{ "constraintIds": ["shop::Item::nonNegativePrice"], "includeModels": true }
```

- `constraintIds`: the constraints to validate; omitted or `null` validates all constraints, an empty list none
- `includeModels`: also return the models containing violating elements (default `false`)

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

- ids in paths are URL-encoded; `validate` and `revision` are reserved and cannot be addressed as ids
- errors have the body `{ "error": "…", "diagnostics": [] }`:
  - `400`: invalid source, with `diagnostics` like `{ "line": 1, "column": 34, "message": "…" }` (line from 1, column from 0), or a missing/malformed body
  - `404`: unknown id
  - `409`: a constraint with the id already exists, or the checker of the constraint does not support changes
  - `422`: no checker supports the language
- an invalid request never changes the constraints

Every response of `/vsum/constraints*` (except `validate`) carries the current revision of the constraints as `ETag` header. It changes whenever a constraint is created, updated or deleted, and only then, also across restarts. Clients can poll `GET /vsum/constraints/revision` (`{ "revision": "\"…\"" }`) to detect changes made by others. `PUT` and `DELETE` accept an `If-Match` header with a revision and answer `412` if the constraints changed since.

The Java client offers `getConstraint`, `createConstraint`, `updateConstraint`, `deleteConstraint` and `getConstraintRevision`; errors are thrown as `BadServerResponseException` with the status code.
