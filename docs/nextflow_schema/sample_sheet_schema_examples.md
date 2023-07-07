---
description: Example JSON Schema for a Nextflow pipeline `nextflow_schema.json` file
---

# Example sample sheet schema

## nf-core/rnaseq example

The nf-core/rnaseq pipeline was one of the first to have a sample sheet schema.
You can see this, used for validating sample sheets with `--input` here: [`assets/schema_input.json`](https://github.com/nf-core/rnaseq/blob/5671b65af97fe78a2f9b4d05d850304918b1b86e/assets/schema_input.json).

!!! tip

    Note the approach used for validating filenames in the `fastq_2` column.
    The column is optional, so if a `pattern` was supplied by itself then validation would fail
    when no string is supplied.

    Instead, we say that the string must _either_ match that pattern or it must have a
    `maxLength` of `0` (an empty string).

```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "$id": "https://raw.githubusercontent.com/nf-core/rnaseq/master/assets/schema_input.json",
  "title": "nf-core/rnaseq pipeline - params.input schema",
  "description": "Schema for the file provided with params.input",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "sample": {
        "type": "string",
        "pattern": "^\\S+$",
        "errorMessage": "Sample name must be provided and cannot contain spaces",
        "meta": ["my_sample"]
      },
      "fastq_1": {
        "type": "string",
        "pattern": "^\\S+\\.f(ast)?q\\.gz$",
        "format": "file-path",
        "errorMessage": "FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
      },
      "fastq_2": {
        "errorMessage": "FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'",
        "anyOf": [
          {
            "type": "string",
            "pattern": "^\\S+\\.f(ast)?q\\.gz$",
            "format": "file-path"
          },
          {
            "type": "string",
            "maxLength": 0
          }
        ]
      },
      "strandedness": {
        "type": "string",
        "errorMessage": "Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'",
        "enum": ["forward", "reverse", "unstranded"],
        "meta": ["my_strandedness"]
      }
    },
    "required": ["sample", "fastq_1", "strandedness"]
  }
}
```

## nf-validation test case

You can see a very feature-complete example JSON Schema for a sample sheet schema file below.

It is used as a test fixture in the nf-validation package [here](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/schema_input.json).

!!! note

    More examples can be found in the plugin [`testResources` directory](https://github.com/nextflow-io/nf-validation/blob/master/plugins/nf-validation/src/testResources/).

```json
--8<-- "plugins/nf-validation/src/testResources/schema_input.json"
```
