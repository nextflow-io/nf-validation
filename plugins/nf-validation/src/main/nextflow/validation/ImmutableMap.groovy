package nextflow.validation

// A class that works like Map, but returns an immutable copy with each method
class ImmutableMap extends LinkedHashMap {

    private Map internalMap

    ImmutableMap(Map initialMap) {
        internalMap = initialMap
    }

    // Override the methods of the Map interface

    @Override
    int size() {
        internalMap.size()
    }

    @Override
    boolean isEmpty() {
        internalMap.isEmpty()
    }

    @Override
    boolean containsKey(Object key) {
        internalMap.containsKey(key)
    }

    @Override
    boolean containsValue(Object value) {
        internalMap.containsValue(value)
    }

    @Override
    Object get(Object key) {
        internalMap.get(key)
    }

    @Override
    Set keySet() {
        internalMap.keySet()
    }

    @Override
    Collection values() {
        internalMap.values()
    }

    @Override
    Set entrySet() {
        internalMap.entrySet()
    }

    // Override methods that can modify the map

    @Override
    Object put(Object key, Object value) {
        throw new UnsupportedOperationException("Cannot .put() items into an ImmutableMap, use .plus() instead to create a new ImmutableMap with the specified entries appended to it.")
    }

    @Override
    void putAll(Map m) {
        throw new UnsupportedOperationException("Cannot .putAll() items into an ImmutableMap, use .plus() instead to create a new ImmutableMap the specified entries appended to it.")
    }

    @Override
    Object remove(Object key) {
        throw new UnsupportedOperationException("Cannot .remove() items from an ImmutableMap, use .dropKey() or .dropKeys() instead to create a new ImmutableMap without the specified keys.")
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException("Cannot clear an ImmutableMap.")
    }

    // Additional methods

    @Override
    String toString() {
        internalMap.toString()
    }

    @Override
    boolean equals(Object obj) {
        internalMap.equals(obj)
    }

    @Override
    int hashCode() {
        internalMap.hashCode()
    }

    // Additional methods for the ImmutableMap class

    Map dropKeys(Collection keys) {
        // Returns a new ImmutableMap without the keys specified in the collection
        new ImmutableMap(internalMap - internalMap.subMap(keys))
    }

    Map dropKey(String key) {
        // Returns a new ImmutableMap without the key specified in the collection
        new ImmutableMap(internalMap - internalMap.subMap(key))
    }

    // Make sure ImmutableMap gets returned instead of Map

    Map collectEntries(Closure transform) {
        new ImmutableMap(internalMap.collectEntries(transform))
    }

    Map collectEntries(Map collector, Closure transform) {
        new ImmutableMap(internalMap.collectEntries(collector, transform))
    }

    Map drop(int num) {
        new ImmutableMap(internalMap.drop(num))
    }

    Map dropWhile(Closure closure) {
        new ImmutableMap(internalMap.dropWhile(closure))
    }

    Map each(Closure closure) {
        new ImmutableMap(internalMap.each(closure))
    }

    Map eachWithIndex(Closure closure) {
        new ImmutableMap(internalMap.eachWithIndex(closure))
    }

    Map findAll(Closure closure) {
        new ImmutableMap(internalMap.findAll(closure))
    }

    Map groupBy(Closure closure) {
        new ImmutableMap(internalMap.groupBy(closure))
    }

    Map groupBy(Object closures) {
        new ImmutableMap(internalMap.groupBy(closures))
    }

    Map groupBy(List closures) {
        new ImmutableMap(internalMap.groupBy(closures))
    }

    Map groupEntriesBy(Closure closure) {
        new ImmutableMap(internalMap.groupEntriesBy(closure))
    }

    Map intersect(Map right) {
        new ImmutableMap(internalMap.intersect(right))
    }

    Map leftShift(Map other) {
        new ImmutableMap(internalMap.leftShift(other))
    }

    Map leftShift(Entry entry) {
        new ImmutableMap(internalMap.leftShift(entry))
    }

    Map minus(Map removeMe) {
        new ImmutableMap(internalMap.minus(removeMe))
    }

    Map plus(Collection entries) {
        new ImmutableMap(internalMap.plus(entries))
    }

    Map plus(Map entries) {
        new ImmutableMap(internalMap.plus(entries))
    }

    Map reverseEach(Closure closure) {
        new ImmutableMap(internalMap.reverseEach(closure))
    }

    Map sort() {
        new ImmutableMap(internalMap.sort())
    }

    Map sort(Closure closure) {
        new ImmutableMap(internalMap.sort(closure))
    }

    Map sort(Comparator comparator) {
        new ImmutableMap(internalMap.sort(comparator))
    }

    Map subMap(Object[] keys) {
        new ImmutableMap(internalMap.subMap(keys))
    }

    Map subMap(Collection keys) {
        new ImmutableMap(internalMap.subMap(keys))
    }

    Map take(int num) {
        new ImmutableMap(internalMap.take(num))
    }

    Map takeWhile(Closure condition) {
        new ImmutableMap(internalMap.takeWhile(condition))
    }

    Map toSorted() {
        new ImmutableMap(internalMap.toSorted())
    }

    Map toSorted(Closure closure) {
        new ImmutableMap(internalMap.toSorted(closure))
    }

    Map toSorted(Comparator comparator) {
        new ImmutableMap(internalMap.toSorted(comparator))
    }

    Map withDefault(boolean autoGrow, boolean autoShrink, Closure init) {
        new ImmutableMap(internalMap.withDefault(autoGrow, autoShrink, init))
    }

    Map withDefault(Closure init) {
        new ImmutableMap(internalMap.withDefault(init))
    }
}