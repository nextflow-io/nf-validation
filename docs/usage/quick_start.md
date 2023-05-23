# Quick Start

Declare the plugin in the Nextflow configuration file:

```nextflow
plugins {
  id 'nf-validation@0.0.1'
}
```

Include a function into your Nextflow pipeline and execute it.

```nextflow
include { validateParameters; paramsHelp; paramsSummaryMap; paramsSummaryLog; validateAndConvertSamplesheet } from 'plugin/nf-validation'

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
ch_input = Channel.validateAndConvertSamplesheet(params.input, "${projectDir}/assets/schema_input.json")
```

You can find more information on plugins in the [Nextflow documentation](https://www.nextflow.io/docs/latest/plugins.html#plugins).

## The JSON Schema

A JSON schema file will contains the information for all pipeline parameters. It can define the required parameters, describe parameter characteristics such as type (ex. string, integer), the pattern (regular expressions matching strings) or a description of the parameter.

You can find more information about JSON Schemas in their official [documentation webpage](https://json-schema.org/). You can see an example JSON Schema for a Nextflow pipeline [`nextflow_schema.json` file](https://raw.githubusercontent.com/nextflow-io/nf-validation/master/plugins/nf-validation/src/testResources/nextflow_schema.json).

> **Note**  
> Although the JSON Schema allows schema objects (eg. params.foo.bar = "baz") or arrays, this is not supported by this plugin.  
> In the example schema file, we use some extra JSON keys not available in the standard JSON Schema set: `help_text`, `hidden` and `fa_icon`.  
> You can find an interactive [schema builder tool](https://nf-co.re/pipeline_schema_builder) in the nf-core website, and more information about the extra keys under the `Help` section.
