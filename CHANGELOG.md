# nextflow-io/nf-validation: Changelog

## Version 0.3.0 (dev)

### Bug fixes

- Only validate a path if it is not null ([#50](https://github.com/nextflow-io/nf-validation/pull/50))
- Only validate a file with a schema if the file path is provided ([#51](https://github.com/nextflow-io/nf-validation/pull/51))
- Handle errors when sample sheet not provided or doesn't have a schema ([#56](https://github.com/nextflow-io/nf-validation/pull/56))
- Silently ignore samplesheet fields that are not defined in samplesheet schema ([#59](https://github.com/nextflow-io/nf-validation/pull/59))
- Correctly handle double-quoted fields containing commas in csv files by `.fromSamplesheet()` [#63](https://github.com/nextflow-io/nf-validation/pull/63))
- Assume that a value with no default in the schema will have a default of 'false' ([#66](https://github.com/nextflow-io/nf-validation/pull/66))

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
