# nextflow-io/nf-schema: Changelog

# Version 2.0.0 - Kagoshima

To migrate from nf-validation please follow the [migration guide](https://nextflow-io.github.io/nf-validation/latest/migration_guide/)

## New features

- Added the `uniqueEntries` keyword. This keyword takes a list of strings corresponding to names of fields that need to be a unique combination. e.g. `uniqueEntries: ['sample', 'replicate']` will make sure that the combination of the `sample` and `replicate` fields is unique. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Added `samplesheetToList` which is the function equivalent of `.fromSamplesheet` [#3](https://github.com/nextflow-io/nf-schema/pull/3)

## Changes

- Changed the used draft for the schema from `draft-07` to `draft-2020-12`. See the [2019-09](https://json-schema.org/draft/2019-09/release-notes) and [2020-12](https://json-schema.org/draft/2020-12/release-notes) release notes for all changes ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed all validation code from the `.fromSamplesheet()` channel factory. The validation is now solely done in the `validateParameters()` function. A custom error message will now be displayed if any error has been encountered during the conversion ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed the `unique` keyword from the samplesheet schema. You should now use [`uniqueItems`](https://json-schema.org/understanding-json-schema/reference/array#uniqueItems) or `uniqueEntries` instead ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed the `skip_duplicate_check` option from the `fromSamplesheet()` channel factory and the `--validationSkipDuplicateCheck` parameter. You should now use the `uniqueEntries` or [`uniqueItems`](https://json-schema.org/understanding-json-schema/reference/array#uniqueItems) keywords in the schema instead ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `.fromSamplesheet()` now is a channel operator instead of a channel factory. It takes one required argument which can either be a string containing the relative path to the schema or a file object of the schema [#3](https://github.com/nextflow-io/nf-schema/pull/3)
- `.fromSamplesheet()` now does dynamic typecasting instead of using the `type` fields in the JSON schema. This is done due to the complexity of `draft-2020-12` JSON schemas. This should not have that much impact but keep in mind that some types can be different between this and earlier versions because of this ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `.fromSamplesheet()` will now set all missing values as `[]` instead of the type specific defaults (because of the changes in the previous point). This should not change that much as this will also result in `false` when used in conditions. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))

## Improvements

- Setting the `exists` keyword to `false` will now check if the path does not exist ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- The `schema` keyword will now work in all schemas. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Improved the error messages ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `.fromSamplesheet()` now supports deeply nested samplesheets ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
