# Split the channel into multiple channels

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
