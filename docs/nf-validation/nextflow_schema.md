---
title: Parameters
description: Functions for working with pipeline parameters
---

# nf-validation for parameters

nf-validation includes four different functions for working with pipeline parameters:

| Function               | Description                                         |
| ---------------------- | --------------------------------------------------- |
| `validateParameters()` | Validate user-provided parameters                   |
| `paramsHelp()`         | Return a pipeline help message string               |
| `paramsSummaryMap()`   | Return a Groovy map summarizing pipeline parameters |
| `paramsSummaryLog()`   | Return summarized pipeline parameters as a string   |

## `validateParameters()`

This function takes all pipeline parameters and checks that they adhere to the specifications defined in the JSON Schema.

- It does not return anything, but logs errors or warnings indicating the parameters that failed to the command line.
- If any parameter validation has failed, it throws a `SchemaValidationException` exception to stop the pipeline.
- If any parameters in the schema reference a sample sheet schema with `schema`, that file is loaded and validated.

The function takes a single argument: the filename of a JSON Schema file.
File paths should be relative to the root of the pipeline directory.
Default: `nextflow_schema.json`.

```groovy
validateParameters() // Default: nextflow_schema.json
validateParameters('custom_nextflow_schema.json')
```

!!! tip

    As much of the Nextflow ecosystem uses this filename, it's recommended to stick with the default, if possible.

See the [Schema specification](../nextflow_schema/schema_specification.md) for information about what validation data you can encode within the schema for each parameter.

There are two specific pipeline parameters that affect the behavior of this function:

!!! abstract "`validationFailUnrecognisedParams`"

    When parameters which are not specified in the JSON Schema are provided, the parameter validation returns a `WARNING`.
    This is because user-specific institutional configuration profiles may make use of params that are unknown to the pipeline.
    The down-side of this is that warnings about typos in parameters can go unnoticed.

    To force the pipeline execution fail with an error instead, you can provide the `validationFailUnrecognisedParams` parameter:

    ```bash
    nextflow run my_pipeline --validationFailUnrecognisedParams
    ```

    or specifying it in the configuration file

    ```groovy
    params.validationFailUnrecognisedParams = true
    ```

!!! abstract "`validationLenientMode`"

    The lenient mode of JSON Schema validation tries to cast parameters to their correct type.
    For example, providing an integer as a string won't fail when using such mode.
    You can find more information [here](https://github.com/everit-org/json-schema#lenient-mode).

    ```bash
    nextflow run my_pipeline --validationLenientMode
    ```

    or specifying it in the configuration file

    ```groovy
    params.validationLenientMode = true
    ```

## `paramsHelp()`

This function returns a help message with the command to run a pipeline and the available parameters.
Pass it to `log.info` to print in the terminal.

It accepts two arguments:

1. An example command, typically used to run the pipeline, to be included in the help string
2. The file name of a Nextflow Schema file (Default: `nextflow_schema.json`)

!!! Note

    `paramsHelp()` doesn't stop pipeline execution after running.
    You must add this into your pipeline code if it's the desired functionality.

Typical usage:

```groovy
if (params.help) {
    log.info paramsHelp("nextflow run my_pipeline --input input_file.csv")
    exit 0 // (1)!
}
```

1.  We shouldn't be using `exit`, but at the time of writing there is no good alternative.
    See [`nextflow-io/nextflow#3984`](https://github.com/nextflow-io/nextflow/issues/3984).

Longer example with a custom schema:

```groovy
if (params.help) {
   def String command = "nextflow run my_pipeline --input input_file.csv --output output_directory"
   log.info paramsHelp(command, 'custom_nextflow_schema.json')
   exit 0
}
```

There are a couple of pipeline parameters that affect how this function works:

!!! abstract "`validationShowHiddenParams`"

    Params that are set as `hidden` in the JSON Schema are not shown in the help message.
    To show these parameters, set the `validationShowHiddenParams` parameter:

    ```bash
    nextflow run my_pipeline --help --validationShowHiddenParams
    ```

    or specifying it in the configuration file

    ```groovy
    params.validationShowHiddenParams = true
    ```

!!! abstract "`help`"

    By default, when printing the help message only a selection of attributes are printed: type of the variable, accepted options (enums), description and default value.

    To print the complete information for a single parameter, pass the parameter name to `--help`:

    ```bash
    nextflow run my_pipeline --help param_name
    ```

## `paramsSummaryMap()`

This function returns a Groovy Map summarizing parameters/workflow options used by the pipeline.
It **only** returns the provided parameters that are **different** to the default values.

The function requires the `WorkflowMetadata` object to be passed as an argument (`workflow`).

It can also accept a custom file name for of a schema file (Default: `nextflow_schema.json`).

```groovy
summary_map = paramsSummaryMap(workflow)
// OR
summary_map = paramsSummaryMap(workflow, 'custom_nextflow_schema.json')
```

## `paramsSummaryLog()`

This function uses the above to generate a human-readable string summarizing the parameters provided to the pipeline.
It **only** returns the provided parameters that are **different** to the default values.

This function takes the same arguments as `paramsSummaryMap()` (the `workflow` object and an optional schema file path).

```groovy
log.info paramsSummaryLog(workflow)
// OR
log.info paramsSummaryLog(workflow, 'custom_nextflow_schema.json')
```
