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

Clone the Nextflow repo into a sibling directory

```bash
cd .. && git clone https://github.com/nextflow-io/nextflow
cd nextflow && ./gradlew exportClasspath
```

Append to the `settings.gradle` in this project the following line:

```bash
includeBuild('../nextflow')
```

Compile the plugin code

```bash
./gradlew compileGroovy
```

Run nextflow with this command:

```bash
./launch.sh run -plugins nf-validation <script/pipeline name> [pipeline params]
```

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
