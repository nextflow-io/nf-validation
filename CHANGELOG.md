# nextflow-io/nf-validation: Changelog

# Version 1.1.4

## Vulnerability fix

- Updated the org.json package to version `20240303` ([#172](https://github.com/nextflow-io/nf-validation/pull/172))

# Version 1.1.3 - Asahikawa

## Improvements

- Added support for double quotes (`"`) in CSV and TSV samplesheets ([#134](https://github.com/nextflow-io/nf-validation/pull/134))

# Version 1.1.2 - Wakayama

## Bug fixes

- Fixed an issue with inputs using `file-path-pattern` where only one file was found (`Path` casting to `ArrayList` error) ([#132](https://github.com/nextflow-io/nf-validation/pull/132))

# Version 1.1.1 - Shoyu

## Bug fixes

- Fixed an issue where samplesheet with a lot of null values would take forever to validate ([#120](https://github.com/nextflow-io/nf-validation/pull/120)) => Thanks @awgymer for fixing this!
- Now YAML files are actually validated instead of skipped ([#124](https://github.com/nextflow-io/nf-validation/pull/120))

# Version 1.1.0 - Miso

## Features

- Add support for samplesheets with no header ([#115](https://github.com/nextflow-io/nf-validation/pull/115))

## Bug fixes

- Floats and doubles should now be created when using the `number` type in the schema ([#113](https://github.com/nextflow-io/nf-validation/pull/113/))
- When `0` is used as a default value in the schema, a `0` will now be used as the value in the `.fromSamplesheet()` channel instead of `null` ([#114](https://github.com/nextflow-io/nf-validation/pull/114))

## New features

- Added `file-path-pattern` format to check every file fetched using a glob pattern. Using a glob is now also possible in the samplesheet and will create a list of all files found using that glob pattern. ([#118](https://github.com/nextflow-io/nf-validation/pull/118))

# Version 1.0.0 - Tonkotsu

The nf-validation plugin is now in production use across many pipelines and has (we hope) now reached a point of relative stability. The bump to major version v1.0.0 signifies that it is suitable for use in production pipelines.

This version also introduces a small breaking change of syntax when providing optional arguments to the functions. You can now provide optional arguments such as the nextflow parameters schema path as:
`validateParameters(parameters_schema: 'my_file.json')`

(previous syntax used positional arguments instead).

## Bug fixes

- The path to a custom parameters schema must be provided through a map '`parameters_schema: 'my_file.json'`' in `validateParameters()` and `paramsSummaryMap()` ([#108](https://github.com/nextflow-io/nf-validation/pull/108))

# Version 0.3.4

This version introduced a bug which made all pipeline runs using the function `validateParameters()` without providing any arguments fail.

This bug causes Nextflow to exit with an error on launch for most pipelines. It should not be used. It was [removed](https://github.com/nextflow-io/plugins/pull/40) from the Nextflow Plugin registry to avoid breaking people's runs.

### Bug fixes

- Do not check S3 URL paths with `PathValidator` `FilePathValidator` and `DirectoryPathValidator` ([#106](https://github.com/nextflow-io/nf-validation/pull/106))
- Make monochrome_logs an option in `paramsSummaryLog()`, `paramsSummaryMap()` and `paramsHelp()` instead of a global parameter ([#101](https://github.com/nextflow-io/nf-validation/pull/101))

# Version 0.3.3

### Bug fixes

- Do not check if S3 URL paths exists to avoid AWS errors, and add a new parameter `validationS3PathCheck` ([#104](https://github.com/nextflow-io/nf-validation/pull/104))

# Version 0.3.2

### Bug fixes

- Add parameters defined on the top level of the schema and within the definitions section as expected params ([#79](https://github.com/nextflow-io/nf-validation/pull/79))
- Fix error when a parameter is not present in the schema and evaluates to false ([#89](https://github.com/nextflow-io/nf-validation/pull/89))
- Changed the `schema_filename` option of `fromSamplesheet` to `parameters_schema` to make this option more clear to the user ([#91](https://github.com/nextflow-io/nf-validation/pull/91))

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
