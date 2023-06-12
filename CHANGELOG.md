# nextflow-io/nf-validation: Changelog

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
