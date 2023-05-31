---
title: Working with sample sheets
description: Function to validate and process sample sheets
---

# nf-validation for sample sheets

Two nf-validation functions interact with files / sample sheets:

| Function               | Description                                                               |
| ---------------------- | ------------------------------------------------------------------------- |
| `validateParameters()` | Validate user-provided parameters <br>_(including sample sheet contents)_ |
| `fromSamplesheet()`    | Validate a samplesheet and return a Nextflow channel                      |

## Validate a sample sheet file contents

When a parameter provides the `schema` field, the `validateParameters()` function will automatically
parse and validate the provided file contents using this JSON schema.
It can validate CSV, TSV and simple YAML files.

The path of the schema file must be relative to the root of the pipeline directory.
See an example in the `input` field from the [example schema.json](https://raw.githubusercontent.com/nextflow-io/nf-validation/master/plugins/nf-validation/src/testResources/nextflow_schema_with_samplesheet.json#L20).

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

For more information about the samplesheet JSON schema refer to [samplesheet docs](../nextflow_schema/schema_specification.md).

## `fromSamplesheet`

This function validates and converts a samplesheet to a ready-to-use Nextflow channel.
This is done using information encoded within a sample sheet schema (see the [docs](../sample_sheet_schema/schema_specification.md)).

The function has one mandatory argument: a string specifying the name of the parameter used to provide a samplesheet.
This parameter be described in the Nextflow parameter schema using as a file, with a `schema` field:

```json
{
  "type": "string",
  "format": "file-path",
  "schema": "assets/foo_schema.json"
}
```

The given sample sheet schema specified in the `schema` key is then loaded and used for validation and sample sheet generation.

Two additional optional arguments can be used:

- The path to the parameters JSON schema: `schema_filename:<file_name>` (Default: `nextflow_schema.json`)
- Whether to create the meta map as an `ImmutableMap` (see [docs](immutable_map.md)) (Default: `true`)

!!! note

    The `immutable_meta` setting can also be set by pipeline users at run time, with the pipeline parameter `params.validationImmutableMeta`.
    This allows end-users to override the setting in case of problems launching the pipeline.

```groovy
Channel.fromSamplesheet('input')
```

```groovy
Channel.fromSamplesheet(
  'input',
  schema_filename: 'custom_nextflow_schema.json',
  immutable_meta: false
)
```

For examples on how to process the created channel, see the [examples](../../examples/branch/).
