# Count channel entries with a common value

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
