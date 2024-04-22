---
title: Validate a sample sheet
description: Validate the contents of a sample sheet file.
---

# Validate a sample sheet file contents

When a parameter provides the `schema` field, the `validateParameters()` function will automatically
parse and validate the provided file contents using this JSON schema.
It can validate CSV, TSV and simple YAML files.

The path of the schema file must be relative to the root of the pipeline directory.
See an example in the `input` field from the [example schema.json](https://raw.githubusercontent.com/nextflow-io/nf-validation/master/plugins/nf-validation/src/testResources/nextflow_schema_with_samplesheet.json#L20).

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

For more information about the samplesheet JSON schema refer to [samplesheet docs](../nextflow_schema/nextflow_schema_specification.md).
