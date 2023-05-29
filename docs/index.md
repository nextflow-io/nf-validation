---
hide:
    - toc
---
# nf-validation

This [Nextlow](https://nextflow.io/) plugin implements a validation of pipeline parameters
based on [nf-core JSON schema](https://nf-co.re/pipeline_schema_builder).

The nf-validation plugin implements the validation of Nextflow pipeline parameters using the [JSON Schema](https://json-schema.org/) format. This plugin contains different functions that can be included into a Nextflow pipeline to provide documentation or validate the pipeline parameters.

It can also validate and convert a samplesheet to a Nextflow channel ready to use. Supported samplesheet formats are CSV, TSV and YAML (simple).

## Dependencies

- Java 11 or later
- <https://github.com/everit-org/json-schema>
