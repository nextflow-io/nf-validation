---
title: ImmutableMap
description: Functions and methods for the ImmutableMap class
---

# ImmutableMap

By default the meta (a map for meta values specified in the schema) is an [`ImmutableMap`](./immutable_map.md) to make sure no unexpected values are overwritten, which can cause trouble because of the asynchronous flow of Nextflow. The output map can also be a normal `LinkedHashMap` when the option `immutable_meta` is set to `false`.

`nextflow.validation.ImmutableMap` is an extension of the `java.util.LinkedHashMap` class. It inherits all the methods from it's parent class, but changes and adds some methods. Below is a list of all changes done.

## New methods

As a community request these methods have been added to the `ImmutableMap` class:

1. `dropKeys(<Collection>)`: Returns a new `ImmutableMap` without the specified keys
2. `dropKey(<String>)`: Returns a new `ImmutableMap` without the specified key

## Changed methods

Methods that would change the contents of the map have been disabled and now show an error message instead. All affected methods are:

- `put()`
- `putAll()`
- `remove()`
- `clear()`

These methods return a new instance of `ImmutableMap` instead of a new instance of `LinkedHashMap`:

- `collectEntries()`
- `drop()`
- `dropWhile()`
- `each()`
- `eachWithIndex()`
- `findAll()`
- `groupBy()`
- `groupEntriesBy()`
- `intersect()`
- `leftShift()`
- `minus()`
- `plus()`
- `reverseEach()`
- `sort()`
- `subMap()`
- `take()`
- `takeWhile()`
- `toSorted()`
- `withDefault()`
