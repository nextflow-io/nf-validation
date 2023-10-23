---
title: Contribution instructions
description: How to contribute to nf-validation
---

# Getting started with plugin development

## Compiling

To compile and run the tests use the following command:

```bash
./gradlew check
```

## Launch it with Nextflow

To test with Nextflow for development purpose:

Compile and install the plugin code

```bash
make compile
make install
```

!!! warning

    This installs the compiled plugin code into your `${HOME}/.nextflow/plugins`
    directory. If the manifest version of your dev code matches an existing plugin any
    install will be overwritten.

## Change and preview the docs

The docs are generated using [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).
You can install the required packages as follows:

```bash
pip install mkdocs-material pymdown-extensions pillow cairosvg
```

To change the docs, edit the files in the [docs/](https://github.com/nextflow-io/nf-validation/tree/master/docs) folder and run the following command to generate the docs:

```bash
mkdocs serve
```

To preview the docs, open the URL provided by mkdocs in your browser.
