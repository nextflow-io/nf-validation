---
title: Migration guide
description: Guide to migrate pipelines using nf-validation pre v2.0.0 to after v2.0.0
hide:
  - toc
---

# Migration guide

This guide is intended to help you migrate your pipeline from older versions of the plugin to version 2.0.0 and later.

## Major changes in the plugin

Following list shows the major breaking changes introduced in version 2.0.0:

1. The JSON schema draft has been updated from `draft-07` to `draft-2020-12`. See [JSON Schema draft 2020-12 release notes](https://json-schema.org/draft/2020-12/release-notes) and [JSON schema draft 2019-09 release notes](https://json-schema.org/draft/2019-09/release-notes) for more information.
2. The `unique` keyword for samplesheet schemas has been removed. Please use [`uniqueItems`](https://json-schema.org/understanding-json-schema/reference/array#uniqueItems) or [`uniqueEntries`](nextflow_schema/nextflow_schema_specification.md#uniqueentries) now instead.
3. The `dependentRequired` keyword now works as it's supposed to work in JSON schema. See [`dependentRequired`](https://json-schema.org/understanding-json-schema/reference/conditionals#dependentRequired) for more information

A full list of changes can be found in the [changelog](../CHANGELOG.md).

## Updating your pipeline

If you aren't using any special features in your schemas, you can simply update your `nextflow_schema.json` file using the following command:

```bash
sed -i -e 's/http:\/\/json-schema.org\/draft-07\/schema/https:\/\/json-schema.org\/draft\/2020-12\/schema/g' -e 's/definitions/defs/g' nextflow_schema.json
```

!!! note
Repeat this command for every JSON schema you use in your pipeline. e.g. for the default samplesheet schema:
`bash sed -i -e 's/http:\/\/json-schema.org\/draft-07\/schema/https:\/\/json-schema.org\/draft\/2020-12\/schema/g' -e 's/definitions/defs/g' assets/schema_input.json `

If you are using any special features in your schemas, you will need to update your schemas manually. Please refer to the [JSON Schema draft 2020-12 release notes](https://json-schema.org/draft/2020-12/release-notes) and [JSON schema draft 2019-09 release notes](https://json-schema.org/draft/2019-09/release-notes) for more information.

However here are some guides to the more common migration patterns:

### Updating `unique` keyword

When you use `unique` in your schemas, you should update it to use `uniqueItems` or `uniqueEntries` instead.

If you used the `unique:true` field, you should update it to use `uniqueItems` like this:

=== "Before v2.0"
`json hl_lines="9" { "$schema": "http://json-schema.org/draft-07/schema", "type": "array", "items": { "type": "object", "properties": { "sample": { "type": "string", "unique": true } } } } `

=== "After v2.0"
`json hl_lines="12" { "$schema": "https://json-schema.org/draft/2020-12/schema", "type": "array", "items": { "type": "object", "properties": { "sample": { "type": "string" } } }, "uniqueItems": true } `

If you used the `unique: ["field1", "field2"]` field, you should update it to use `uniqueEntries` like this:

=== "Before v2.0"
`json hl_lines="9" { "$schema": "http://json-schema.org/draft-07/schema", "type": "array", "items": { "type": "object", "properties": { "sample": { "type": "string", "unique": ["sample"] } } } } `

=== "After v2.0"
`json hl_lines="12" { "$schema": "https://json-schema.org/draft/2020-12/schema", "type": "array", "items": { "type": "object", "properties": { "sample": { "type": "string" } } }, "uniqueEntries": ["sample"] } `

### Updating `dependentRequired` keyword

When you use `dependentRequired` in your schemas, you should update it like this:

=== "Before v2.0"
`json hl_lines="12" { "$schema": "http://json-schema.org/draft-07/schema", "type": "object", "properties": { "fastq_1": { "type": "string", "format": "file-path" }, "fastq_2": { "type": "string", "format": "file-path" "dependentRequired": ["fastq_1"] } } } `

=== "After v2.0"
`json hl_lines="14 15 16" { "$schema": "https://json-schema.org/draft/2020-12/schema", "type": "object", "properties": { "fastq_1": { "type": "string", "format": "file-path" }, "fastq_2": { "type": "string", "format": "file-path" } }, "dependentRequired": { "fastq_2": ["fastq_1"] } } `
