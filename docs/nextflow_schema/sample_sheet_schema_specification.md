---
description: Schema specification for sample sheet validation
---

# Sample sheet schema specification

Sample sheet schema files are used by the nf-validation plugin for validation of sample sheet contents and type conversion / channel generation.

The Nextflow schema syntax is based on the JSON schema standard.
You can find more information about JSON Schema here:

- Official docs: <https://json-schema.org>
- Excellent "Understanding JSON Schema" docs: <https://json-schema.org/understanding-json-schema>

## Schema structure

Validation by the plugin works by parsing the supplied file contents into a groovy object,
then passing this to the JSON schema validation library.
As such, the structure of the schema must match the structure of the parsed file.

Typically, sample sheets are CSV files, with fields represented as columns and samples as rows. TSV and simple unnested YAML files are also supported by the plugin.

!!! warning

    Nested YAML files can be validated with the `validateParameters()` function, but cannot be converted to a channel with `.fromSamplesheet()`.

In this case, the parsed object will be an `array` (see [JSON schema docs](https://json-schema.org/understanding-json-schema/reference/array.html#items)).
The array type is associated with an `items` key which in our case contains a single `object`.
The object has `properties`, where the keys must match the headers of the CSV file.

So, for CSV sample sheets, the top-level schema should look something like this:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "field_1": { "type": "string" },
      "field_2": { "type": "string" }
    }
  }
}
```

If your sample sheet has a different format (for example, a simple YAML file),
you will need to build your schema to match the parsed structure.

## Properties

Every array object will contain keys for each field.
Each field should be described as an element in the object `properties` section.

The keys of each property must match the header text used in the sample sheet.

Fields that are present in the sample sheet, but not in the schema will be ignored and produce a warning.

!!! tip

    The order of columns in the _sample sheet_ is not relevant, as long as the header text matches.

!!! warning

    The order of properties in the _schema_ **is** important.
    This order defines the order of output channel properties when using the `fromSamplesheet` channel factory.

## Common keys

The majority of schema keys for sample sheet schema validation are identical to the Nextflow schema.
For example: `type`, `pattern`, `format`, `errorMessage`, `exists` and so on.

Please refer to the [Nextflow schema specification](../nextflow_schema/nextflow_schema_specification.md) docs for details.

!!! tip

    Sample sheets are commonly used to define input file paths.
    Be sure to set `"type": "string"` and `"format": "file-path"` for these properties,
    so that nf-validation correctly returns this sample sheet field as a `Nextflow.file` object.

When using the `file-path-pattern` format for a globbing pattern, a list will be created with all files found by the globbing pattern. See [here](../nextflow_schema/nextflow_schema_specification.md#file-path-pattern) for more information.

## Sample sheet keys

Below are the properties that are specific to sample sheet schema.
These exist in addition to those described in the [Nextflow schema specification](../nextflow_schema/nextflow_schema_specification.md).

### `meta`

Type: `List`

The current field will be considered a meta value when this parameter is present. This parameter should contain a list of the meta fields to assign this value to. The default is no meta for each field.

For example:

```json
{
  "meta": ["id", "sample"]
}
```

will convert the `field` value to a meta value, resulting in the channel `[[id:value, sample:value]...]`
See [here](https://github.com/nextflow-io/nf-validation/blob/ce3aef60e5103ea4798375fe6c59bae41b7d2a25/plugins/nf-validation/src/testResources/schema_input.json#L10-L25) for an example in the sample sheet.

### `unique`

Type: `Boolean` or `List`

Whether or not the field should contain a unique value over the entire sample sheet.

Default: `false`

- Can be `true`, in which case the value for this field should be unique for all samples in the sample sheet.
- Can be supplied with a list of field names, containing _other field names_ that should be unique _in combination with_ the current field.

!!! example

    Consider the following example:

    ```json
    "properties": {
      "field1": { "unique": true },
      "field2": { "unique": ["field1"] }
    }
    ```

    `field1` needs to be unique in this example. `field2` needs to be unique in combination with `field1`. So for a sample sheet like this:

    ```csv linenums="1"
    field1,field2
    value1,value2
    value1,value3
    value1,value2
    ```

    ..both checks will fail.

    * `field1` isn't unique since `value1` has been found more than once.
    * `field2` isn't unique in combination with `field1` because the `value1,value2` combination has been found more than once.

    See [`schema_input.json#L48-L55`](https://github.com/nextflow-io/nf-validation/blob/ce3aef60e5103ea4798375fe6c59bae41b7d2a25/plugins/nf-validation/src/testResources/schema_input.json#L48-L55)
    for an example in one of the plugin test-fixture sample sheets.

### `deprecated`

Type: `Boolean`

A boolean variable stating that the field is deprecated and will be removed in the nearby future. This will throw a warning to the user that the current field is deprecated. The default value is `false`.

Example:

```json
"field": {
    "deprecated": true
}
```

will show a warning stating that the use of `field` is deprecated:

```console
The 'field' field is deprecated and
will no longer be used in the future.
Please check the official documentation
of the pipeline for more information.
```

### `dependentRequired`

Type: `List`

- See [JSON Schema docs](https://json-schema.org/understanding-json-schema/reference/conditionals.html#dependentrequired)

A list containing names of other fields. The validator will check if these fields are filled in and throw an error if they aren't, but only when the field `dependentRequired` belongs to is filled in.

!!! example

    ```json
    "field1": {
        "dependentRequired": ["field2"]
    },
    "field2": {}
    ```

    will check if `field2` is given when `field1` has a value. So for example:

    ```csv linenums="1"
    field1,field2
    value1,value2
    value1,
    ,value2
    ```

    - [x] The first row will pass the check because both fields are set.
    - [ ] The second row will fail because `field1` is set, but `field2` isn't and `field1` is dependent on `field2`.
    - [x] The third row will pass the check because `field1` isn't set.

    See [here](https://github.com/nextflow-io/nf-validation/blob/ce3aef60e5103ea4798375fe6c59bae41b7d2a25/plugins/nf-validation/src/testResources/schema_input.json#L10-L25) for an example in the sample sheet.
