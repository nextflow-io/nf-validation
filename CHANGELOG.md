# nextflow-io/nf-validation: Changelog

## Version 0.4.0 (dev)

### Bug fixes

- Add parameters defined on the top level of the schema and within the definitions section as expected params ([#79](https://github.com/nextflow-io/nf-validation/pull/79))
- Fix error when a parameter is not present in the schema and evaluates to false ([#89](https://github.com/nextflow-io/nf-validation/pull/89))

## Version 0.3.1

### Bug fixes

- Don't check if path exists if param is not true ([#74](https://github.com/nextflow-io/nf-validation/pull/74))
- Don't validate a file if the parameter evaluates to false ([#75](https://github.com/nextflow-io/nf-validation/pull/75))

## Version 0.3.0

### New features

- Check that a sample sheet doesn't have duplicated entries by default. Can be disabled with `--validationSkipDuplicateCheck` ([#72](https://github.com/nextflow-io/nf-validation/pull/72))

### Bug fixes

- Only validate a path if it is not null ([#50](https://github.com/nextflow-io/nf-validation/pull/50))
- Only validate a file with a schema if the file path is provided ([#51](https://github.com/nextflow-io/nf-validation/pull/51))
- Handle errors when sample sheet not provided or doesn't have a schema ([#56](https://github.com/nextflow-io/nf-validation/pull/56))
- Silently ignore samplesheet fields that are not defined in samplesheet schema ([#59](https://github.com/nextflow-io/nf-validation/pull/59))
- Correctly handle double-quoted fields containing commas in csv files by `.fromSamplesheet()` ([#63](https://github.com/nextflow-io/nf-validation/pull/63))
- Print param name when path does not exist ([#65](https://github.com/nextflow-io/nf-validation/pull/65))
- Fix file or directory does not exist error not printed when it was the only error in a samplesheet ([#65](https://github.com/nextflow-io/nf-validation/pull/65))
- Do not return parameter in summary if it has no default in the schema and is set to 'false' ([#66](https://github.com/nextflow-io/nf-validation/pull/66))
- Skip the validation of a file if the path is an empty string and improve error message when the path is invalid ([#69](https://github.com/nextflow-io/nf-validation/pull/69))

### Deprecated

- The meta map of input channels is not an ImmutableMap anymore ([#68](https://github.com/nextflow-io/nf-validation/pull/68)). Reason: [Issue #52](https://github.com/nextflow-io/nf-validation/issues/52)

## Version 0.2.1

### Bug fixes

- Fixed a bug where `immutable_meta` option in `fromSamplesheet()` wasn't working when using `validateParameters()` first. (@nvnieuwk)

## Version 0.2.0

### New features

- Added a new [documentation site](https://nextflow-io.github.io/nf-validation/). (@ewels and @mashehu)
- Removed the `file-path-exists`, `directory-path-exists` and `path-exists` and added a [`exists`](https://nextflow-io.github.io/nf-validation/nextflow_schema/nextflow_schema_specification/#exists) parameter to the schema. (@mirpedrol)
- New [`errorMessage`](https://nextflow-io.github.io/nf-validation/nextflow_schema/nextflow_schema_specification/#errormessage) parameter for the schema which can be used to create custom error messages. (@mirpedrol)
- Samplesheet validation now happens in `validateParameters()` using the schema specified by the `schema` parameter in the parameters schema. (@mirpedrol)

### Improvements

- The `meta` maps are now immutable by default, see [`ImmutableMap`](https://nextflow-io.github.io/nf-validation/samplesheets/immutable_map/) for more info (@nvnieuwk)
- `validateAndConvertSamplesheet()` has been renamed to `fromSamplesheet()`
- Refactor `--schema_ignore_params` to `--validationSchemaIgnoreParams`

### Bug fixes

- Fixed a bug where an empty meta map would be created when no meta values are in the samplesheet schema. (@nvnieuwk)

## Version 0.1.0

Initial release.
