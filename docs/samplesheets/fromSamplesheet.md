---
title: Create a channel
description: Channel operator to create a channel from a sample sheet.
---

# Create a channel from a sample sheet

## `fromSamplesheet`

This channel operator validates and converts a sample sheet to ready-to-use channel entries. This is done using information encoded within a sample sheet schema (see the [docs](../nextflow_schema/sample_sheet_schema_specification.md)).

The operator has one mandatory argument: the path of the JSON schema file corresponding to the samplesheet. This can be either a string with the relative path (from the root of the pipeline) or a file object of the schema.


```groovy
Channel.of("path/to/samplesheet").fromSamplesheet("path/to/json/schema")
```

## Basic example

In [this example](https://github.com/nextflow-io/nf-schema/tree/master/examples/fromSamplesheetBasic), we create a simple channel from a CSV sample sheet.

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

=== "assets/schema_input.json"

    ```json
    --8<-- "examples/fromSamplesheetBasic/pipeline/assets/schema_input.json"
    ```

## Order of fields

[This example](https://github.com/nextflow-io/nf-schema/tree/master/examples/fromSamplesheetOrder) demonstrates that the order of columns in the sample sheet file has no effect.

!!! danger

    It is the order of fields **in the sample sheet JSON schema** which defines the order of items in the channel returned by `fromSamplesheet()`, _not_ the order of fields in the sample sheet file.

```
--8<-- "examples/fromSamplesheetOrder/log.txt"
```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/fromSamplesheetOrder/samplesheet.csv"
    ```

=== "assets/schema_input.json"

    ```json hl_lines="10 15 20 25"
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

## Channel with meta map

In [this example](https://github.com/nextflow-io/nf-schema/tree/master/examples/fromSamplesheetMeta), we use the schema to mark two columns as meta fields.
This returns a channel with a meta map.

```
--8<-- "examples/fromSamplesheetMeta/log.txt"
```

=== "assets/schema_input.json"

    ```json hl_lines="14 30"
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
