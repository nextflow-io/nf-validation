# Separate channel entries based on a condition

You can use the [`.branch()` operator](https://www.nextflow.io/docs/latest/operator.html#branch) to separate the channel entries based on a condition. This is especially useful when you can get multiple types of input data. 

This example shows a channel which can have entries for WES or WGS data. These analysis are different so we want to separate the WES and WGS entries from eachother. We also don't want the `bed` file input for the WGS data, so the resulting channel with WGS data should not have this file in it.

```groovy
// example channel: 
// [[id:example, type:WGS], WGS.bam, WGS.bam.bai, []]
// [[id:example2, type:WES], WES.bam, WES.bam.bai, WES.bed]
params.input = "samplesheet.csv"
Channel.validateAndConvertSamplesheet(file(params.input), file("schema.json"))
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