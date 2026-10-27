# Vitruv-Server
Vitruv Server

## Constraint endpoints

Constraints are registered at the virtual model via `VirtualModelBuilder.withConstraintChecker(...)`, e.g. with the `VitruvOclConstraintChecker` of VitruvOCL. The server exposes them independently of the constraint language.

| Method | Path | Description |
|---|---|---|
| GET | `/vsum/constraints` | Lists all constraints (`id`, `name`, `contextType`, `kind`, `severity`, `language`, `source`) |
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
