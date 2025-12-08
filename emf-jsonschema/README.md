# Metamodeling with JSON

Result of Master's Thesis by Adrian Freund

## Structure

The source code is located in the `src/` folder.
The `main/kotlin` folder contains the main code while the `test/kotlin` folder contains the tests.
The `main/resources/schema` directory contains the adjusted meta-schema and extension vocabularies.

All code is contained in the `io.freund.adrian.emfjsonschema` package.
The `transform` subpackage contains code related to Ecore to JSON-Schema and JSON-Schema to Ecore transformations.
The `generate` subpackage contains code related to code generation.

In the `transform` subpackage the Ecore to JSON-Schema transformation is in `EcoreTransformer.kt`. The JSON-Schema to 
Ecore transformation in `JsonSchemaTransformer.kt`.
`Transformations.kt` contains a previous attempt of specifying both transformation at the same time in a hybrid
declaration-procedural transformation DSL. While this worked great for simple transformations it became to complicated for
more complex transformations, which is why the two transformation directions were seperated again.

## Usage

This project uses the pixi package manager to make sure your Java and Gradle version are up to date
and other dependencies are installed.

After cloning run `pixi run pre-commit-install` to configure pre-commit.
To run the project run `pixi run start`. You should then see the output in the `out/` directory.
Run tests with `pixi run test`
and linters with `pixi run lint` (`pixi run lint:fix` to auto-fix fixable errors)

If you don't want to install `pixi` and have a working java installation you can also directly interact with gradle instead.
