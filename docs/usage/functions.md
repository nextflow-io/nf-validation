# Functions

nf-validation includes five different functions that you can include in your pipeline. Those functions can be used to:

- `validateParameters()` - validate user-provided parameters
- `paramsHelp()` - print a help message
- `paramsSummaryMap()` - summarize pipeline parameters
- `paramsSummaryLog()` - return summarized pipeline parameters as a string
- `fromSamplesheet()` - validate and convert a samplesheet into a Nextflow channel

## `validateParameters`

This function takes all pipeline parameters and checks that they adhere to the specifications defined in the JSON Schema.
It returns errors or warnings indicating the parameters that failed.

### Usage

When using this function in your pipeline, you can provide the name of a JSON Schema file. It defaults to '`nextflow_schema.json`'.
File paths should be relative to the root of the pipeline directory.
File paths should be relative to the root of the pipeline directory.

```nextflow
validateParameters('custom_nextflow_schema.json')
```

### Options

There are two specific params that affect the behavior of this function:

#### `validationFailUnrecognisedParams`

When parameters which are not specified in the JSON Schema are provided, the parameter validation returns a `WARNING`. To force the pipeline execution fail returning an `ERROR` instead, you can provide the `validationFailUnrecognisedParams` parameter.

```bash
nextflow run my_pipeline --validationFailUnrecognisedParams
```

or specifying it in the configuration file

```nextflow
params.validationFailUnrecognisedParams = true
```

#### `validationLenientMode`

The lenient mode of JSON Schema validation tries to cast parameters to their correct type. For example, providing an integer as a string won't fail when using such mode. You can find more information [here](https://github.com/everit-org/json-schema#lenient-mode).

```bash
nextflow run my_pipeline --validationLenientMode
```

or specifying it in the configuration file

```nextflow
params.validationLenientMode = true
```

## Formats

Formats can be used to check `string` values for certain properties.

Following table shows all additional formats implemented in this plugin. These formats will also be converted to the correct type.

| Format                | Description                                                                                                                                                                                                            |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| file-path             | States if the provided value is a file. Does not check its existence.                                                                                                                                                  |
| file-path-exists      | Automatically checks if the file exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input.                                                           |
| directory-path        | States if the provided value is a directory. Does not check its existence.                                                                                                                                             |
| directory-path-exists | Automatically checks if the directory exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input. This is currently synonymous for `file-path-exists`. |
| path                  | States if the provided value is a path (file or directory). Does not check its existence.                                                                                                                              |
| path-exists           | Automatically checks if the path (file or directory) exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input.                                       |

You can use the formats like this:

```json
"field": {
    "type":"string",
    "format":"file-path"
}
```

See [here](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/schema_input.json#L33-41) for an example in the samplesheet.

## `paramsHelp`

This function prints a help message with the command to run a pipeline and the available parameters.

!!! Note
    `paramsHelp()` doesn't stop pipeline execution after running. You must add this into your pipeline code if it's the desired functionality.

### Usage

This function required an argument providing the typical command used to run the pipeline. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

In this example we are executing the function if the parameter `help` is provided and ending the execution afterwards.

```nextflow
if (params.help) {
   def String command = "nextflow run my_pipeline --input input_file.csv --output output_directory"
   log.info paramsHelp(command, 'custom_nextflow_schema.json')
   exit 0
}
```

### Options

#### `validationShowHiddenParams`

Params that are defined to be hidden in the JSON Schema are not shown in the help message. In order to show these parameters you can use the `validationShowHiddenParams` parameter.

```bash
nextflow run my_pipeline --help --validationShowHiddenParams
```

or specifying it in the configuration file

```nextflow
params.validationShowHiddenParams = true
```

#### Show the complete information for one parameter

By default, when printing the help message only a selection of attributes are printed: type of the variable, accepted options "enums", description and default value. In order to print the complete information for a single parameter, you can pass the parameter name to `--help`:

```bash
nextflow run my_pipeline --help param_name
```

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

## `paramsSummaryMap`

This function returns a Groovy Map summarizing parameters/workflow options used by the pipeline.

!!! Note
    `paramsSummaryMap()` will return only the provided parameters that differ from the default values.

### Usage

This function requires an argument providing the a WorkflowMetadata object. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

```nextflow
paramsSummaryMap(workflow, 'custom_nextflow_schema.json')
```

## `paramsSummaryLog`

This function returns a string summarizing the parameters provided to the pipeline.

!!! Note
    `paramsSummaryLog()` will return only the provided parameters that differ from the default values.

### Usage

This function requires an argument providing the a WorkflowMetadata object. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

```nextflow
paramsSummaryLog(workflow, 'custom_nextflow_schema.json')
```

## `fromSamplesheet`

This function validates and converts a samplesheet to a ready-to-use Nextflow channel. The JSON schema used for the samplesheets slightly differs from the JSON schema (and supports draft 4-7). More information on this can be found in the [samplesheet docs](./samplesheet_docs.md).

### Usage

The function requires the name of the param used by the user to provide a samplesheet. The path to the parameters JSON schema can also be provided, defaults to `nextflow_schema.json`.
The provided parameter must contain a ['`schema`' field](#validate-an-input-file-provided-by-params-with-another-json-schema).

```nextflow
Channel.fromSamplesheet('input', 'custom_nextflow_schema.json')
```

For examples on how to process the created channel, see the [examples/](examples/) folder.
