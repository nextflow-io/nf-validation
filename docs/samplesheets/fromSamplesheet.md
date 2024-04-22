---
title: Create a channel
description: Channel factory to create a channel from a sample sheet.
---

# Create a channel from a sample sheet

## `fromSamplesheet`

This function validates and converts a samplesheet to a ready-to-use Nextflow channel. This is done using information encoded within a sample sheet schema (see the [docs](../nextflow_schema/sample_sheet_schema_specification.md)).

The function has one mandatory argument: the name of the parameter which specifies the input samplesheet. The parameter specified must have the format `file-path` and include additional field `schema`:

```json hl_lines="4"
{
  "type": "string",
  "format": "file-path",
  "schema": "assets/foo_schema.json"
}
```

The path specified in the `schema` key determines the JSON used for validation of the samplesheet.

When using the `.fromSamplesheet` channel factory, some additional optional arguments can be used:

- `parameters_schema`: File name for the pipeline parameters schema. (Default: `nextflow_schema.json`)
- `skip_duplicate_check`: Skip the checking for duplicates. Can also be skipped with the `--validationSkipDuplicateCheck` parameter. (Default: `false`)

```groovy
Channel.fromSamplesheet('input')
```

```groovy
Channel.fromSamplesheet(
  'input',
  parameters_schema: 'custom_nextflow_schema.json',
  skip_duplicate_check: false
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

!!! danger

    It is the order of fields **in the sample sheet JSON schema** which defines the order of items in the channel returned by `fromSamplesheet()`, _not_ the order of fields in the CSV file.

```
--8<-- "examples/fromSamplesheetOrder/log.txt"
```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/fromSamplesheetOrder/samplesheet.csv"
    ```

=== "assets/schema_input.json"

    ```json hl_lines="10 15 20 33"
    --8<-- "examples/fromSamplesheetOrder/pipeline/assets/schema_input.json"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/fromSamplesheetOrder/pipeline/main.nf"
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
