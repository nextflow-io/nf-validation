---
title: Channel usage examples
description: Examples of advanced sample sheet creation techniques.
---

# Sample sheet channel manipulation examples

## Separate entries based on a condition

You can use the [`.branch()` operator](https://www.nextflow.io/docs/latest/operator.html#branch) to separate the channel entries based on a condition. This is especially useful when you can get multiple types of input data.

This example shows a channel which can have entries for WES or WGS data. These analysis are different so we want to separate the WES and WGS entries from eachother. We also don't want the `bed` file input for the WGS data, so the resulting channel with WGS data should not have this file in it.

```groovy
// example channel:
// [[id:example, type:WGS], WGS.bam, WGS.bam.bai, []]
// [[id:example2, type:WES], WES.bam, WES.bam.bai, WES.bed]
params.input = "samplesheet.csv"
Channel.fromSamplesheet("input")
    .branch { meta, bam, bai, bed ->
        WGS: meta.type == "WGS"
            return [meta, bam, bai]
            // The original channel structure will be used when no return statement is used.
        WES: meta.type == "WES"
    }
    .set { input }

input.WGS.view() // [[id:example, type:WGS], WGS.bam, WGS.bam.bai]
input.WES.view() // [[id:example2, type:WES], WES.bam, WES.bam.bai, WES.bed]
```

## Count entries with a common value

This example is based on this [code](https://github.com/mribeirodantas/NextflowSnippets/blob/main/snippets/countBy.md) from @mribeirodantas.

It's useful to determine the count of channel entries with similar values when you want to merge them later on (to prevent pipeline bottlenecks with `.groupTuple()`).

This example contains a channel where multiple samples can be in the same family. Later on in the pipeline we want to merge the analyzed files so one file gets created for each family. The result will be a channel with an extra meta field containing the count of channel entries with the same family name.

```groovy
// example channel:
// [[id:example1, family:family1], example1.txt]
// [[id:example2, family:family1], example2.txt]
// [[id:example3, family:family2], example3.txt]
params.input = "samplesheet.csv"
Channel.fromSamplesheet("input")
    .tap { ch_raw } // Create a copy of the original channel
    .map { meta, txt -> [ meta.family ] } // Isolate the value to count on
    .reduce([:]) { counts, family ->
        counts[family] = (counts[family] ?: 0) + 1
        counts
    } // Creates a map like this: [family1:2, family2:1]
    .combine(ch_raw) // Add the count map to the original channel
    .map { counts, meta, txt ->
        new_meta = meta + [count:counts[meta.family]]
        [ new_meta, txt ]
    } // Add the counts of the current family to the meta
    .set { input }

input.view()
// [[id:example1, family:family1, count:2], example1.txt]
// [[id:example2, family:family1, count:2], example2.txt]
// [[id:example3, family:family2, count:1], example3.txt]
```

## Split into multiple channels

Sometimes you don't want all inputs to remain in the same channel (e.g. when the files need to be preprocessed separately).

Following code shows an example where a `cram` file and a `bed` file are given in the samplesheet. The result contains two channels: one with the `cram` file and one with the `bed` file.

```groovy
// example channel: [[id:example], example.cram, example.cram.crai, example.bed]
params.input = "samplesheet.csv"
Channel.fromSamplesheet("input")
    .multiMap { meta, cram, crai, bed ->
        cram: [meta, cram, crai]
        bed:  [meta, bed]
    }
    .set { input }

input.cram.view() // [[id:example], example.cram, example.cram.crai]
input.bed.view() // [[id:example], example.bed]
```
