---
description: Functions and methods for the ImmutableMap class
---

# Immutable meta maps

## Introduction and usage

A common pitfall when utilizing meta maps in Nextflow pipelines is overlooking their mutable nature. Meta maps in Nextflow (`LinkedHashMap`) are mutable, meaning that if you change a value inside the map, this change will persist across different processes and channels in your pipeline. This can lead to unexpected behavior if not handled correctly.

!!! info

    To learn more about this and see examples, watch the nf-core/bytesize talk [Bytesize: Workflow safety and immutable objects](https://nf-co.re/events/2023/bytesize_workflow_safety).

To avoid this, the nf-validation `fromSamplesheet()` function returns a custom map object when using a metamap.
This map is _immutable_ and will throw an error if you try to edit it.

This `ImmutableMap` object is returned by default, but if you prefer you can return a normal `LinkedHashMap` by setting the `fromSamplesheet()` option `immutable_meta` to `false`.

```groovy
Channel.fromSamplesheet(
  'input',
  immutable_meta: false
)
```

The `immutable_meta` setting can also be set by pipeline users at run time, with the pipeline parameter `params.validationImmutableMeta`.
This allows end-users to override the setting in case of problems launching the pipeline.

```bash
nextflow run <pipeline> --validationImmutableMeta
```

!!! info

    `nextflow.validation.ImmutableMap` is an extension of the `java.util.LinkedHashMap` class. It inherits all the methods from it's parent class, but changes and adds some methods.

## New methods

The following custom new methods have been added to the `ImmutableMap` class:

1. `dropKeys(<Collection>)`: Returns a new `ImmutableMap` without the specified keys
2. `dropKey(<String>)`: Returns a new `ImmutableMap` without the specified key

## Methods that return a copy of the map

These methods return a _new_ instance of `ImmutableMap`.
This copy is safe to work with as it does not affect the original meta map.

<div style="display: flex;" markdown>
<div style="padding: 0 1rem;" markdown>
`collectEntries()` <br>
`drop()` <br>
`dropWhile()` <br>
`each()` <br>
`eachWithIndex()` <br>
`findAll()` <br>
`groupBy()`
</div>
<div style="padding: 0 1rem;" markdown>
`groupEntriesBy()` <br>
`intersect()` <br>
`leftShift()` <br>
`minus()` <br>
`plus()` <br>
`reverseEach()`
</div>
<div style="padding: 0 1rem;" markdown>
`sort()` <br>
`subMap()` <br>
`take()` <br>
`takeWhile()` <br>
`toSorted()` <br>
`withDefault()` <br>
</div>
</div>

## Methods that return an error

The following methods _change_ the contents of the map and have been disabled.

If you use them in a pipeline it will exit with an error.

<div style="padding: 0 1rem;" markdown>
`put()`<br>
`putAll()`<br>
`remove()`<br>
`clear()`
</div>
