---
title: Create a list
description: Function to create a list from a sample sheet.
---

# Create a list from a sample sheet

## `samplesheetToList`

This function validates and converts a sample sheet to a list in a similar way to the [`fromSamplesheet`](./fromSamplesheet.md) channel operator. This is done using information encoded within a sample sheet schema (see the [docs](../nextflow_schema/sample_sheet_schema_specification.md)).

The function has two mandatory arguments:

1. The path to the samplesheet
2. The path to the JSON schema file corresponding to the samplesheet.

These can be either a string with the relative path (from the root of the pipeline) or a file object of the schema.

```groovy
samplesheetToList("path/to/samplesheet", "path/to/json/schema")
```

!!! note

    This function works very similar to the `fromSamplesheet` channel operator. See the [`fromSamplesheet` examples](./fromSamplesheet.md#basic-example) for some examples on how to use this function.
