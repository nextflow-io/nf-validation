---
title: Create a channel
description: Channel factory to create a channel from a sample sheet.
---

# Create a channel from a sample sheet

## `fromSamplesheet`

This function validates and converts a samplesheet to a ready-to-use Nextflow channel.
This is done using information encoded within a sample sheet schema (see the [docs](../nextflow_schema/sample_sheet_schema_specification.md)).

The function has one mandatory argument: a string specifying the name of the parameter used to provide a samplesheet.
This parameter be described in the Nextflow parameter schema using as a file, with a `schema` field:

```json hl_lines="4"
{
  "type": "string",
  "format": "file-path",
  "schema": "assets/foo_schema.json"
}
```

The given sample sheet schema specified in the `schema` key is then loaded and used for validation and sample sheet generation.

Two additional function optional arguments can be used:

- File name for the pipeline parameters schema. (Default: `nextflow_schema.json`)
- Whether to create the meta map as an `ImmutableMap` - see [docs](immutable_map.md) (Default: `true`)

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

## Basic example

In [this example](../../examples/fromSamplesheetBasic/), we create a simple channel from a CSV samplesheet.

```
--8<-- "examples/fromSamplesheetBasic/log.txt"
```

=== "main.nf"

    ```groovy
    --8<-- "examples/fromSamplesheetBasic/pipeline/main.nf"
    ```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/fromSamplesheetBasic/samplesheet.csv"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/fromSamplesheetBasic/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json hl_lines="19"
    --8<-- "examples/fromSamplesheetBasic/pipeline/nextflow_schema.json"
    ```

=== "assets/schema_input.json"

    ```json
    --8<-- "examples/fromSamplesheetBasic/pipeline/assets/schema_input.json"
    ```

## Order of fields

[This example](../../examples/fromSamplesheetOrder/) demonstrates that the order of columns in the sample sheet file has no effect.
It is the order of fields in the sample sheet schema which defines the order returned by `fromSamplesheet()`.

```
--8<-- "examples/fromSamplesheetOrder/log.txt"
```

=== "assets/schema_input.json"

    ```json hl_lines="10 15 20 33"
    --8<-- "examples/fromSamplesheetOrder/pipeline/assets/schema_input.json"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/fromSamplesheetOrder/pipeline/main.nf"
    ```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/fromSamplesheetOrder/samplesheet.csv"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/fromSamplesheetOrder/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/fromSamplesheetOrder/pipeline/nextflow_schema.json"
    ```

## Channel with meta map

In [this example](../../examples/fromSamplesheetMeta/), we use the schema to mark two columns as meta fields.
This returns a channel with a meta map.

```
--8<-- "examples/fromSamplesheetMeta/log.txt"
```

=== "assets/schema_input.json"

    ```json hl_lines="14 38"
    --8<-- "examples/fromSamplesheetMeta/pipeline/assets/schema_input.json"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/fromSamplesheetMeta/pipeline/main.nf"
    ```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/fromSamplesheetMeta/samplesheet.csv"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/fromSamplesheetMeta/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/fromSamplesheetMeta/pipeline/nextflow_schema.json"
    ```
