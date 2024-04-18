---
title: Create a list
description: Function to create a list from a sample sheet.
---

# Create a list from a sample sheet

## `samplesheetToList`

This function validates and converts a sample sheet to a list. This is done using information encoded within a sample sheet schema (see the [docs](../nextflow_schema/sample_sheet_schema_specification.md)).

The function has two mandatory arguments:

1. The path to the samplesheet
2. The path to the JSON schema file corresponding to the samplesheet.

These can be either a string with the relative path (from the root of the pipeline) or a file object of the schema.

```groovy
samplesheetToList("path/to/samplesheet", "path/to/json/schema")
```

This function can be used together with existing channel factories/operators to create one channel entry per samplesheet entry.

### Use as a channel factory

The function can be given to the `.fromList` channel factory to mimic the functionality of a channel factory:

```groovy
Channel.fromList(samplesheetToList("path/to/samplesheet", "path/to/json/schema"))
```

!!! note

    This will mimic the `fromSamplesheet` channel factory as it was in [nf-validation](https://github.com/nextflow-io/nf-validation).

### Use as a channel oprator

The function can be used with the `.flatMap` channel operator to create a channel from samplesheets that are already in a channel:

```groovy
Channel.of("path/to/samplesheet").flatMap { samplesheetToList(it, "path/to/json/schema") }
```

## Basic example

In [this example](https://github.com/nextflow-io/nf-schema/tree/master/examples/samplesheetToListBasic), we create a simple channel from a CSV sample sheet.

```
--8<-- "examples/samplesheetToListBasic/log.txt"
```

=== "main.nf"

    ```groovy
    --8<-- "examples/samplesheetToListBasic/pipeline/main.nf"
    ```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/samplesheetToListBasic/samplesheet.csv"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/samplesheetToListBasic/pipeline/nextflow.config"
    ```

=== "assets/schema_input.json"

    ```json
    --8<-- "examples/samplesheetToListBasic/pipeline/assets/schema_input.json"
    ```

## Order of fields

[This example](https://github.com/nextflow-io/nf-schema/tree/master/examples/samplesheetToListOrder) demonstrates that the order of columns in the sample sheet file has no effect.

!!! danger

    It is the order of fields **in the sample sheet JSON schema** which defines the order of items in the channel returned by `samplesheetToList()`, _not_ the order of fields in the sample sheet file.

```
--8<-- "examples/samplesheetToListOrder/log.txt"
```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/samplesheetToListOrder/samplesheet.csv"
    ```

=== "assets/schema_input.json"

    ```json hl_lines="10 15 20 25"
    --8<-- "examples/samplesheetToListOrder/pipeline/assets/schema_input.json"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/samplesheetToListOrder/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/samplesheetToListOrder/pipeline/nextflow.config"
    ```

## Channel with meta map

In [this example](https://github.com/nextflow-io/nf-schema/tree/master/examples/samplesheetToListMeta), we use the schema to mark two columns as meta fields.
This returns a channel with a meta map.

```
--8<-- "examples/samplesheetToListMeta/log.txt"
```

=== "assets/schema_input.json"

    ```json hl_lines="14 30"
    --8<-- "examples/samplesheetToListMeta/pipeline/assets/schema_input.json"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/samplesheetToListMeta/pipeline/main.nf"
    ```

=== "samplesheet.csv"

    ```csv
    --8<-- "examples/samplesheetToListMeta/samplesheet.csv"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/samplesheetToListMeta/pipeline/nextflow.config"
    ```

