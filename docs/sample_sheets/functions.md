# nf-validation functions

Two nf-validation functions interact with sample sheets:

| Function               | Description                                                               |
| ---------------------- | ------------------------------------------------------------------------- |
| `validateParameters()` | Validate user-provided parameters <br>_(including sample sheet contents)_ |
| `fromSamplesheet()`    | Validate a samplesheet and return a Nextflow channel                      |

## Validate an input file provided by params with another JSON schema

By provided the `schema` field in one off the parameters, the function will automatically validate the provided file using this JSON schema. It can validate CSV, TSV and simple YAML files.
The path of the schema file must be relative to the root of the pipeline directory. See an example in the `input` field from the [example schema.json](https://raw.githubusercontent.com/nextflow-io/nf-validation/master/plugins/nf-validation/src/testResources/nextflow_schema_with_samplesheet.json#L20).

```json
{
  "properties": {
    "input": {
      "type": "string",
      "format": "file-path",
      "pattern": "^\\S+\\.csv$",
      "schema": "src/testResources/samplesheet_schema.json",
      "description": "Path to comma-separated file containing information about the samples in the experiment."
    }
  }
}
```

For more information about the samplesheet JSON schema refer to [samplesheet docs](./samplesheet_docs.md). Note that the validation performed by `validateParameters` is limited to the [JSON Schema](https://json-schema.org/) validation. Additional validation checks are performed by [`fromSamplesheet`](#fromSamplesheet).

## `fromSamplesheet`

This function validates and converts a samplesheet to a ready-to-use Nextflow channel. The JSON schema used for the samplesheets slightly differs from the JSON schema (and supports draft 4-7). More information on this can be found in the [samplesheet docs](./samplesheet_docs.md).

The function requires the name of the param used by the user to provide a samplesheet. The path to the parameters JSON schema can also be provided with `schema_filename:<file_name>`, defaults to `nextflow_schema.json`.
By default the meta (a map for meta values specified in the schema) is an [`ImmutableMap`](docs/immutablemap.md) to make sure no unexpected values are overwritten, which can cause trouble because of the asynchronous flow of Nextflow. The output map can also be a normal `LinkedHashMap` when the option `immutable_meta` is set to `false`. This can also be affected by a pipeline parameter called `validationImmutableMeta` which can take a boolean value.
The provided parameter must contain a ['`schema`' field](#validate-an-input-file-provided-by-params-with-another-json-schema).

```nextflow
Channel.fromSamplesheet(
  'input',
  schema_filename:'custom_nextflow_schema.json',
  immutable_meta:true
)
```

### Usage

The function requires the name of the param used by the user to provide a samplesheet. The path to the parameters JSON schema can also be provided, defaults to `nextflow_schema.json`.
The provided parameter must contain a ['`schema`' field](#validate-an-input-file-provided-by-params-with-another-json-schema).

```groovy
Channel.fromSamplesheet('input', 'custom_nextflow_schema.json')
```

For examples on how to process the created channel, see the [examples/](examples/) folder.
