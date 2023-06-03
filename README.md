# ![nf-validation](docs/images/nf-validation.svg)

## 📚 Docs 👉🏻 <https://nextflow-io.github.io/nf-validation>

**A Nextflow plugin to work with validation of pipeline parameters and sample sheets.**

## Introduction

This [Nextflow plugin](https://www.nextflow.io/docs/latest/plugins.html#plugins) provides a number of functions that can be included into a Nextflow pipeline script to work with parameter and sample sheet schema. Using these functions you can:

- 📖 Print usage instructions to the terminal (for use with `--help`)
- ✍️ Print log output showing parameters with non-default values
- ✅ Validate supplied parameters against the pipeline schema
- 📋 Validate the contents of supplied sample sheet files
- 🛠️ Create a Nextflow channel with a parsed sample sheet

Supported sample sheet formats are CSV, TSV and YAML (simple).

## Quick Start

Declare the plugin in your Nextflow pipeline configuration file:

_(make sure you pin the latest stable release version after the `@`)_

```groovy title="nextflow.config"
plugins {
  id 'nf-validation@0.1.0'
}
```

This is all that is needed - Nextflow will automatically fetch the plugin code at run time.

You can now include the plugin helper functions into your Nextflow pipeline:

```groovy title="main.nf"
include { validateParameters; paramsHelp; paramsSummaryLog; fromSamplesheet } from 'plugin/nf-validation'

// Print help message, supply typical command line usage for the pipeline
if (params.help) {
   log.info paramsHelp("nextflow run my_pipeline --input input_file.csv")
   exit 0
}

// Validate input parameters
validateParameters()

// Print summary of supplied parameters
log.info paramsSummaryLog(workflow)

// Create a new channel of metadata from a sample sheet
// NB: `input` corresponds to `params.input` and associated sample sheet schema
ch_input = Channel.fromSamplesheet("input")
```

## Background

The [Nextflow](https://nextflow.io/) workflow manager is a powerful tool for scientific workflows.
In order for end users to launch a given workflow with different input data and varying settings, pipelines are developed using a special variable type called parameters (`params`). Defaults are hardcoded into scripts and config files but can be overwritten by user config files and command-line flags (see the [Nextflow docs](https://nextflow.io/docs/latest/config.html)).

In addition to config params, a common best-practice for pipelines is to use a "sample sheet" file containing required input information. For example: a sample identifier, filenames and other sample-level metadata.

Nextflow itself does not provide functionality to validate config parameters or parsed sample sheets. To bridge this gap, we developed code within the [nf-core community](https://nf-co.re/) to allow pipelines to work with a standard `nextflow_schema.json` file, written using the [JSON Schema](https://json-schema.org/) format. The file allows strict typing of parameter variables and inclusion of validation rules.

The nf-validation plugin moves this code out of the nf-core template into a stand-alone package, to make it easier to use for the wider Nextflow community. It also incorporates a number of new features, such as native Groovy sample sheet validation.

## Dependencies

- Java 11 or later
- <https://github.com/everit-org/json-schema>

## Credits

This plugin was written based on code initially written within the nf-core community,
as part of the nf-core pipeline template.

We would like to thank the key contributors who include (but are not limited to):

- Júlia Mir Pedrol ([@mirpedrol](https://github.com/mirpedrol))
- Nicolas Vannieuwkerke ([@nvnieuwk](https://github.com/nvnieuwk))
- Kevin Menden ([@KevinMenden](https://github.com/KevinMenden))
- Phil Ewels ([@ewels](https://github.com/ewels))
