# ImmutableMap
`nextflow.validation.ImmutableMap` is an extension of the `java.util.LinkedHashMap` class. It inherits all the methods from it's parent class, but changes and adds some methods. Below is a list of all changes done.

## New methods
As a community request these methods have been added to the `ImmutableMap` class:
1. `dropKeys(<Collection>)`: Returns a new `ImmutableMap` without the specified keys
2. `dropKey(<String>)`: Returns a new `ImmutableMap` without the specified key

## Changed methods
1. Methods that would change the contents of the map have been disabled and now show an error message instead. All affected methods are:
    - `put()`
    - `putAll()`
    - `remove()`
    - `clear()`

2. These methods return a new instance of `ImmutableMap` instead of a new instance of `LinkedHashMap`:
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