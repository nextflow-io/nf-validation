---
title: nf-validation
description: Nextflow plugin for sample sheet validation
---

# ![nf-validation](images/nf-validation.svg)

{==

**A Nextflow plugin to work with validation of pipeline parameters and sample sheets.**

==}

!!! warning "`nf-validation` has now been renamed to `nf-schema`."

    **`nf-validation` has now been renamed to `nf-schema`. The `nf-validation` plugin will not receive any future updates.**
    **Please update your pipelines to use [`nf-schema`](https://github.com/nextflow-io/nf-schema) instead.**
    This change was necessary to prevent older versions of `nf-core` pipelines from with unpinned plugin references from breaking when updating to the latest version of `nf-validation`.
    **Please pin the version of `nf-schema` in your pipeline's `nextflow.config` file:**
    ```nextflow
    plugins { id 'nf-schema@2.0.0' }
    ```

--8<-- "README.md:18:"
