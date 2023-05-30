---
title: Schema specification
---

# Parameter Schema Specification

The Nextflow schema file contains information about pipeline configuration parameters.
The file is typically saved in the workflow root directory and called `nextflow_schema.json`.

The Nextflow schema syntax is based on the JSON schema standard, with some key differences.
You can find more information about JSON Schema here:

- Official docs: <https://json-schema.org>
- Excellent "Understanding JSON Schema" docs: <https://json-schema.org/understanding-json-schema>

!!! note

    This file is a reference specification, _not_ documentation about how to write a schema manually.

    Please see [Creating schema files](create_schema.md) for instructions on how to create these files
    (and don't be tempted to do it manually in a code editor!)

## Definitions

A slightly strange use of a JSON schema standard that we use for Nextflow schema is `definitions`.

JSON schema can group variables together in an `object`, but then the validation expects this structure to exist in the data that it is validating.
In reality, we have a very long "flat" list of parameters, all at the top level of `params.foo`.

In order to give some structure to log outputs, documentation and so on, we group parameters into `definitions`.
Each `definition` is an object with a title, description and so on.
However, as they are under `definitions` scope they are effectively ignored by the validation and so their nested nature is not a problem.
We then bring the contents of each definition object back to the "flat" top level for validation using a series of `allOf` statements at the end of the schema,
which reference the specific definition keys.

<!-- prettier-ignore-start -->
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "object",
  // Definition groups
  "definitions": { // (1)!
    "my_group_of_params": { // (2)!
      "title": "A virtual grouping used for docs and pretty-printing",
      "type": "object",
      "required": ["foo", "bar"], // (3)!
      "properties": { // (4)!
        "foo": { // (5)!
          "type": "string"
        },
        "bar": {
          "type": "string"
        }
      }
    }
  },
  // Contents of each definition group brought into main schema for validation
  "allOf": [
    { "$ref": "#/definitions/my_group_of_params" } // (6)!
  ]
}
```
<!-- prettier-ignore-end -->

1. An arbitrary number of definition groups can go in here - these are ignored by main schema validation.
2. This ID is used later in the `allOf` block to reference the definition.
3. Note that any required properties need to be listed within this object scope.
4. Actual parameter specifications go in here.
5. Shortened here for the example, see below for full parameter specification.
6. A `$ref` line like this needs to be added for every definition group

Parameters can be described outside of the `definitions` scope, in the regular JSON Schema top-level `properties` scope.
However, they will be displayed as ungrouped in tools working off the schema.

## Parameters

### Nested parameters

!!! warning "Warning (TLDR;)"

    Although the JSON Schema allows schema objects (eg.` params.foo.bar = "baz"`) or arrays, this is not supported by this plugin.

Nextflow config allows parameters to be nested as objects, for example:

```groovy
params {
    foo {
        bar = "baz"
    }
}
```

or on the CLI:

```bash
nextflow run <pipeline> --foo.bar "baz"
```

But - the current implementations of the Nextflow _schema_ do not (for now).

This was done as a conscious decision when the code was first written, to try to reduce complexity.

It would be great to implement this at some point - there is a GitHub issue to track the feature request here:
[`nf-core/tools#1554`](https://github.com/nf-core/tools/issues/1554). Contributions welcome!

!!! tip

    In the example schema file, we use some extra JSON keys not available in the standard JSON Schema set: `help_text`, `hidden` and `fa_icon`.

    You can find an interactive [schema builder tool](https://nf-co.re/pipeline_schema_builder) in the nf-core website, and more information about the extra keys under the `Help` section.

### Object key

The `properties` object _key_ must correspond to the parameter variable name in the Nextflow config.

For example, for `params.foo`, the schema should look like this:

```json
// ..
"type": "object",
"properties": {
    "foo": {
        "type": "string",
        // ..
    }
}
// ..
```

### `description`

A short description of what the parameter does, written in markdown.
Printed in docs and terminal help text.
Should be maximum one short sentence.

### `help_text`

A longer text with usage help for the parameter, written in markdown.
Can include newlines with multiple paragraphs and more complex markdown structures.

Typically hidden by default in documentation and interfaces, unless explicitly clicked / requested.

### `fa_icon`

A text identifier corresponding to an icon from [Font Awesome](https://fontawesome.com/).
Used for easier visual navigation of documentation and pipeline interfaces.

Should be the font-awesome class names, for example:

```json
"fa_icon": "fas fa-file-csv"
```

### `type`

Variable type, taken from the [JSON schema keyword vocabulary](https://json-schema.org/understanding-json-schema/reference/type.html):

- `string`
- `number` (float)
- `integer`
- `boolean` (true / false)
- `null`

Two JSON schema types are _not_ supported (see [Nested paramters](#nested-parameters)):

- `object`
- `array`

### `format`

Formats can be used to check `string` values for certain properties.

You can use the formats like this:

```json
"field": {
    "type":"string",
    "format":"file-path"
}
```

The following list describes additional supported formats.
These formats will be converted to the correct type.

`file-path`
: States if the provided value is a file. Does not check its existence.

`file-path-exists`
: Automatically checks if the file exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input.

`directory-path`
: States if the provided value is a directory. Does not check its existence.

`directory-path-exists`
: Automatically checks if the directory exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input.
This is currently synonymous for `file-path-exists`.

`path`
: States if the provided value is a path (file or directory). Does not check its existence.

`path-exists`
: Automatically checks if the path (file or directory) exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input.

See [here](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/schema_input.json#L33-41) for an example in the samplesheet.

## Example Schema

You can see an example JSON Schema for a Nextflow pipeline [`nextflow_schema.json` file](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/nextflow_schema.json):

??? info "Click to expand: `nextflow_schema.json`"

    ```json
    --8<-- "plugins/nf-validation/src/testResources/nextflow_schema.json"
    ```

!!! note

    More examples can be found in the plugin [`testResources` directory](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/).
