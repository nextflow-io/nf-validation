---
description: Return a summary of the parameters being set.
---

# Summary log

## `paramsSummaryLog()`

This function returns a string that can be logged to the terminal, summarizing the parameters provided to the pipeline.

It only returns the provided parameters that are **different** to the schema default values.

The function takes two arguments:

- The `WorkflowMetadata` object, `workflow` (required)
- File name of a schema file (optional, default: `nextflow_schema.json`).

Typical usage:

=== "main.nf"

    ```groovy
    --8<-- "examples/paramsSummaryLog/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/paramsSummaryLog/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/paramsSummaryLog/pipeline/nextflow_schema.json"
    ```

Output:

```
--8<-- "examples/paramsSummaryLog/log.txt"
```

## `paramsSummaryMap()`

This function returns a Groovy Map summarizing parameters/workflow options used by the pipeline.
It **only** returns the provided parameters that are **different** to the default values.

This function takes the same arguments as `paramsSummaryLog()`: the `workflow` object and an optional schema file path.

Typical usage:

=== "main.nf"

    ```groovy
    --8<-- "examples/paramsSummaryMap/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/paramsSummaryMap/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/paramsSummaryMap/pipeline/nextflow_schema.json"
    ```

Output:

```
--8<-- "examples/paramsSummaryMap/log.txt"
```
