# nf-validation 

This Nextlow plugins implements a validation of pipeline parameters
based on [nf-core JSON schema](https://nf-co.re/pipeline_schema_builder).

It can also validate and convert a samplesheet to a Nextflow channel ready to use. Supported samplesheet formats are CSV, TSV and YAML (simple).

## Get started 

To compile and run the tests use the following command: 


```
./gradlew check
```      


## Launch it with Nextflow 

[WORK IN PROGRESS]

To test with Nextflow for development purpose:

1. Clone the Nextflow repo into a sibling directory  

   ```
   cd .. && https://github.com/nextflow-io/nextflow
   cd nextflow && ./gradlew exportClasspath
   ``` 

2. Append to the `settings.gradle` in this project the following line:

   ```
   includeBuild('../nextflow')
   ```                        
   
3. Compile the plugin code

   ```
   ./gradlew compileGroovy
   ```
   
4. run nextflow with this command:

    ```
    ./launch.sh run -plugins nf-validation <script/pipeline name> [pipeline params]
    ```


## Dependencies

* Java 11 or later
* https://github.com/everit-org/json-schema

# Usage

## Introduction

The nf-validation plugin implements the validation of Nextflow pipeline parameters using the [JSON Schema](https://json-schema.org/) format. This plugin contains different functions that can be included into a Nextflow pipeline to provide documentation or validate the pipeline parameters.

## Quick Start

Declare the plugin in the Nextflow configuration file:

```nextflow
plugins {
  id 'nf-validation@0.0.1'
}
```

Include a function into your Nextflow pipeline and execute it.

```nextflow
include { validateParameters, paramsHelp, paramsSummaryMap, paramsSummaryLog, validateAndConvertSamplesheet } from 'plugin/nf-validation'

// Print help message
if (params.help) {
   def String command = "nextflow run my_pipeline --input input_file.csv"
   log.info paramsHelp(command)
   exit 0
}

// Validate input parameters
validateParameters()

// Print parameter summary log to screen
log.info paramsSummaryLog(workflow)

// Obtain an input channel from a sample sheet
ch_input = Channel.validateAndConvertSamplesheet(params.input, "${projectDir}/assets/schema_input.json)
```

You can find more information on plugins in the [Nextflow documentation](https://www.nextflow.io/docs/latest/plugins.html#plugins).

### The JSON Schema

A JSON schema file will contains the information for all pipeline parameters. It can define the required parameters, describe parameter characteristics such as type (ex. string, integer), the pattern (regular expressions matching strings) or a description of the parameter.

You can find more information about JSON Schemas in their official [documentation webpage](https://json-schema.org/). You can see an example JSON Schema for a Nextflow pipeline [`nextflow_schema.json` file](https://raw.githubusercontent.com/nextflow-io/nf-validation/master/plugins/nf-validation/src/testResources/nextflow_schema.json).

> **Note**  
> Although the JSON Schema allows schema objects (eg. params.foo.bar = "baz") or arrays, this is not supported by this plugin.  
> In the example schema file, we use some extra JSON keys not available in the standard JSON Schema set: `help_text`, `hidden` and `fa_icon`.  
> You can find an interactive [schema builder tool](https://nf-co.re/pipeline_schema_builder) in the nf-core website, and more information about the extra keys under the `Help` section.

## Functions

nf-validation includes five different functions that you can include in your pipeline. Those functions can be used to:

- `validateParameters()` - validate user-provided parameters
- `paramsHelp()` - print a help message
- `paramsSummaryMap()` - summarize pipeline parameters
- `paramsSummaryLog()` - return summarized pipeline parameters as a string
- `validateAndConvertSamplesheet()` - validate and convert a samplesheet into a Nextflow channel

### validateParameters

This function takes all pipeline parameters and checks that they adhere to the specifications defined in the JSON Schema.
It returns errors or warnings indicating the parameters that failed.

#### Usage

When using this function in your pipeline, you can provide the name of a JSON Schema file. It defaults to '`nextflow_schema.json`'.
File paths should be relative to the root of the pipeline directory.
File paths should be relative to the root of the pipeline directory.

```nextflow
validateParameters('custom_nextflow_schema.json')
```

#### Options

There are two specific params that affect the behavior of this function:

##### `validationFailUnrecognisedParams`

When parameters which are not specified in the JSON Schema are provided, the parameter validation returns a `WARNING`. To force the pipeline execution fail returning an `ERROR` instead, you can provide the  `validationFailUnrecognisedParams` parameter.
```bash
nextflow run my_pipeline --validationFailUnrecognisedParams
```
or specifying it in the configuration file
```nextflow
params.validationFailUnrecognisedParams = true
```

##### `validationLenientMode`

The lenient mode of JSON Schema validation tries to cast parameters to their correct type. For example, providing an integer as a string won't fail when using such mode. You can find more information [here](https://github.com/everit-org/json-schema#lenient-mode).

```bash
nextflow run my_pipeline --validationLenientMode
```
or specifying it in the configuration file
```nextflow
params.validationLenientMode = true
```

### paramsHelp

This function prints a help message with the command to run a pipeline and the available parameters.

> **Note**  
> `paramsHelp()` doesn't stop pipeline execution after running. You must add this into your pipeline code if it's the desired functionality.

#### Usage

This function required an argument providing the typical command used to run the pipeline. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

In this example we are executing the function if the parameter `help` is provided and ending the execution afterwards.
```nextflow
if (params.help) {
   def String command = "nextflow run my_pipeline --input input_file.csv --output output_directory"
   log.info paramsHelp(command, 'custom_nextflow_schema.json')
   exit 0
}
```

#### Options

##### `validationShowHiddenParams`

Params that are defined to be hidden in the JSON Schema are not shown in the help message. In order to show these parameters you can use the `validationShowHiddenParams` parameter.

```bash
nextflow run my_pipeline --help --validationShowHiddenParams
```
or specifying it in the configuration file
```nextflow
params.validationShowHiddenParams = true
```

##### Show the complete information for one parameter

By default, when printing the help message only a selection of attributes are printed: type of the variable, accepted options "enums", description and default value. In order to print the complete information for a single parameter, you can pass the parameter name to `--help`:

```bash
nextflow run my_pipeline --help param_name
```

### Validate an input file provided by params with another JSON schema

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
         "description": "Path to comma-separated file containing information about the samples in the experiment.",
      }
}
```

For more information about the samplesheet JSON schema refer to [samplesheet docs](docs/samplesheetDocs.md). Note that the validation performed by `validateParameters` is limited to the [JSON Schema](https://json-schema.org/) validation. Additional validation checks are performed by []`validateAndConvertSamplesheet`](https://github.com/mirpedrol/nf-validation/blob/75babb6fc293042d2c0a5acd728291bb3c5d7cf5/README.md#L250).

### paramsSummaryMap

This function returns a Groovy Map summarizing parameters/workflow options used by the pipeline.

> **Note**  
> `paramsSummaryMap()` will return only the provided parameters that differ from the default values.

#### Usage

This function requires an argument providing the a WorkflowMetadata object. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

```nextflow
paramsSummaryMap(workflow, 'custom_nextflow_schema.json')
```

### paramsSummaryLog

This function returns a string summarizing the parameters provided to the pipeline. 

> **Note**  
> `paramsSummaryLog()` will return only the provided parameters that differ from the default values.

#### Usage

This function requires an argument providing the a WorkflowMetadata object. It can also accept the name of a JSON Schema file [default = '`nextflow_schema.json`'].

```nextflow
paramsSummaryLog(workflow, 'custom_nextflow_schema.json')
```

### validateAndConvertSamplesheet

This function validates and converts a samplesheet to a ready-to-use Nextflow channel. The JSON schema used for the samplesheets slightly differs from the JSON schema (and supports draft 4-7). More information on this can be found in the [samplesheet docs](docs/samplesheetDocs.md).

#### Usage

The function requires two different inputs: the samplesheet and the schema used for the samplesheet. Both files need to be passed through the `file()` function as input for this function.

```nextflow
validateAndConvertSamplesheet(
   file('path/to/samplesheet', checkIfExists:true),
   file('path/to/schema', checkIfExists:true)
)
```

Note that in order to fully validate the sample sheet you must always run [`validateParameters()`](https://github.com/mirpedrol/nf-validation/blob/ce409583b4582f4221cbf0d0d3917e35f4ba628d/README.md#L116) with the [`schema` field provided](https://github.com/mirpedrol/nf-validation/blob/ce409583b4582f4221cbf0d0d3917e35f4ba628d/README.md#L200).