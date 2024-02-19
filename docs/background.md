---
title: Background
description: Get up and running with nf-validation
hide:
  - toc
---

# Background

The [Nextflow](https://nextflow.io/) workflow manager is a powerful tool for scientific workflows.
In order for end users to launch a given workflow with different input data and varying settings, pipelines are developed using a special variable type called parameters (`params`). Defaults are hardcoded into scripts and config files but can be overwritten by user config files and command-line flags (see the [Nextflow docs](https://nextflow.io/docs/latest/config.html)).

In addition to config params, a common best-practice for pipelines is to use a "sample sheet" file containing required input information. For example: a sample identifier, filenames and other sample-level metadata.

Nextflow itself does not provide functionality to validate config parameters or parsed sample sheets. To bridge this gap, we developed code within the [nf-core community](https://nf-co.re/) to allow pipelines to work with a standard `nextflow_schema.json` file, written using the [JSON Schema](https://json-schema.org/) format. The file allows strict typing of parameter variables and inclusion of validation rules.

The nf-validation plugin moves this code out of the nf-core template into a stand-alone package, to make it easier to use for the wider Nextflow community. It also incorporates a number of new features, such as native Groovy sample sheet validation.
