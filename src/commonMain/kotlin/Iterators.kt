@file:OptIn(ExperimentalStdlibApi::class)

package cc.datafabric.iterators


/**
 * Shortcut for `if (hasNext()) next() else null`
 */
fun <X> Iterator<X>.nextOrNull() = if (hasNext()) next() else null

/**
 * Returns an empty resource iterator.
 */
fun <X> emptyResourceIterator(): ResourceIterator<X> {
    return EmptyResourceIterator()
}

/**
 * Creates a resource iterator that returns the specified values.
 */
fun <X> resourceIteratorOf(vararg elements: X, onClose: () -> Unit = {}): ResourceIterator<X> {
    return WrappedResourceIterator(elements.iterator(), onClose)
}

/**
 * Returns a resource iterator which invokes the function to calculate the next value on each iteration until the function returns `null`.
 * The returned resource iterator is constrained to be iterated only once.
 */
fun <X> generateResourceIterator(next: () -> X?, onClose: () -> Unit = {}): ResourceIterator<X> {
    return GeneratorResourceIterator(next, onClose)
}

/**
 * Returns an [ResourceIterator] that returns the values from the resource iterator.
 * Throws an exception if the resource iterator is constrained to be iterated once and `iterator`
 * is invoked the second time.
 */
fun <X> Sequence<X>.asResourceIterator(onClose: () -> Unit = {}): ResourceIterator<X> {
    return this.iterator().asResourceIterator(onClose)
}

/**
 * Returns an iterator over the elements in this collection.
 * There are no guarantees concerning the order in which the elements are returned
 * (unless this collection is an instance of some class that provides a guarantee).
 */
fun <X> Iterable<X>.asResourceIterator(onClose: () -> Unit = {}): ResourceIterator<X> {
    return this.iterator().asResourceIterator(onClose)
}

/**
 * Returns an [ResourceIterator] that returns the values from the [Iterator].
 */
fun <X> Iterator<X>.asResourceIterator(onClose: () -> Unit = {}): ResourceIterator<X> {
    return WrappedResourceIterator(this, onClose)
}

/**
 * Returns an [ResourceIterator] over the entries in the [Map].
 */
fun <K, V> Map<out K, V>.asResourceIterator(onClose: () -> Unit = {}): ResourceIterator<Map.Entry<K, V>> {
    return this.entries.asResourceIterator(onClose)
}

/**
 * Returns a resource-iterator that yields elements of this resource-iterator sorted according to natural sort order of the value returned by specified [selector] function.
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 * The operation is _intermediate_ and _stateful_.
 */
inline fun <X, R : Comparable<R>> ResourceIterator<X>.sortedBy(crossinline selector: (X) -> R?): ResourceIterator<X> =
    sortedWith(compareBy(selector))

/**
 * Returns a resource-iterator that yields elements of this resource-iterator sorted according to the specified [comparator].
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 * The operation is _intermediate_ and _stateful_.
 */
fun <X> ResourceIterator<X>.sortedWith(comparator: Comparator<in X>): ResourceIterator<X> = use {
    val sortedList = toMutableList()
    sortedList.sortWith(comparator)
    return sortedList.asResourceIterator()
}

/**
 * Returns a resource-iterator that yields elements of this resource-iterator sorted according to their natural sort order.
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 * The operation is _intermediate_ and _stateful_.
 */
fun <T : Comparable<T>> ResourceIterator<T>.sorted(): ResourceIterator<T> = use {
    val sortedList = toMutableList()
    sortedList.sort()
    return sortedList.asResourceIterator()
}

/**
 * Returns a resource-iterator containing only the non-null results of applying the given [transform] function
 * to each element in the original resource-iterator.
 * The operation is _intermediate_ and _stateless_.
 */
@Suppress("UNCHECKED_CAST")
fun <T, R : Any> ResourceIterator<T>.mapNotNull(transform: (T) -> R?): ResourceIterator<R> =
    map(transform).filter { it != null } as ResourceIterator<R>

/**
 * Groups elements of the original resource-iterator by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 * The returned map preserves the entry iteration order of the keys produced from the original resource-iterator.
 * The operation is _terminal_.
 */
inline fun <T, K> ResourceIterator<T>.groupBy(keySelector: (T) -> K): Map<K, List<T>> = use {
    return groupByTo(LinkedHashMap(), keySelector)
}

/**
 * Groups elements of the original resource-iterator by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 * The operation is _terminal_.
 */
inline fun <T, K, M : MutableMap<in K, MutableList<T>>> ResourceIterator<T>.groupByTo(
    destination: M,
    keySelector: (T) -> K,
): M = use {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { arrayListOf() }
        list.add(element)
    }
    return destination
}

/**
 * Returns a [Map] containing the elements from the given resource-iterator indexed by the key
 * returned from [keySelector] function applied to each element.
 * If any two elements had the same key returned by [keySelector] the last one gets added to the map.
 * The returned map preserves the entry iteration order of the original resource-iterator.
 * The operation is _terminal_.
 */
fun <T, K> ResourceIterator<T>.associateBy(keySelector: (T) -> K): Map<K, T> = use {
    asInternalSequence().associateBy(keySelector)
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given resource-iterator.
 * If any of two pairs have the same key, the last one gets added to the map.
 * The returned map preserves the entry iteration order of the original resource-iterator.
 * The operation is _terminal_.
 */
fun <T, K, V> ResourceIterator<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> = use {
    asInternalSequence().associate(transform)
}

/**
 * Analogy of the `Sequence<T>.associateByTo`.
 */
inline fun <T, K, M : MutableMap<in K, in T>> ResourceIterator<T>.associateByTo(
    destination: M,
    keySelector: (T) -> K
): M =
    use {
        for (i in this) {
            destination.put(keySelector(i), i)
        }
        return destination
    }

/**
 * Analogy of the `Sequence<T>.associateTo`.
 */
inline fun <T, K, V, M : MutableMap<in K, in V>> ResourceIterator<T>.associateTo(
    destination: M,
    transform: (T) -> Pair<K, V>
): M = use {
    for (i in this) {
        destination += transform(i)
    }
    return destination
}

/**
 * Returns a new map containing all key-value pairs from the given resource-iterator of pairs.
 *
 * The returned map preserves the entry iteration order of the original resource-iterator.
 * If any of two pairs have the same key, the last one gets added to the map.
 */
fun <K, V> ResourceIterator<Pair<K, V>>.toMap(): Map<K, V> = use {
    asInternalSequence().toMap()
}

/**
 * Appends all elements to the given [destination] collection.
 * The operation is _terminal_.
 */
fun <T, C : MutableCollection<in T>> ResourceIterator<T>.toCollection(destination: C): C = use {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element.
 *
 * Returns the specified [initial] value if the resource-iterator is empty.
 * @param [operation] function that takes current accumulator value and an element, and calculates the next accumulator value.
 * The operation is _terminal_.
 */
inline fun <T, R> ResourceIterator<T>.fold(initial: R, operation: (acc: R, T) -> R): R = use {
    var res = initial
    for (element in this) {
        res = operation(res, element)
    }
    res
}

/**
 * Returns `true` if all elements match the given [predicate].
 * The operation is _terminal_.
 */
inline fun <T> ResourceIterator<T>.all(predicate: (T) -> Boolean): Boolean = use {
    for (element in this) {
        if (!predicate(element)) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 * The operation is _terminal_.
 */
inline fun <X> ResourceIterator<X>.any(predicate: (X) -> Boolean): Boolean = use {
    for (item in this) {
        if (predicate(item)) {
            return true
        }
    }
    return false
}

/**
 * Performs the given [action] for each remaining element until all elements
 * have been processed or the [action] throws an exception.
 */
inline fun <X> ResourceIterator<X>.forEach(action: (X) -> Unit) {
    use {
        while (hasNext()) {
            action(next())
        }
    }
}

/**
 * Returns a resource-iterator which performs the given [action] on each element of the original resource-iterator as they pass through it.
 * The operation is _intermediate_ and _stateless_.
 */
fun <T> ResourceIterator<T>.onEach(action: (T) -> Unit): ResourceIterator<T> = map {
    action(it)
    it
}


/**
 * Splits this ResourceIterator into a ResourceIterator of lists each not exceeding the given [size].
 * The last list in the resulting ResourceIterator may have fewer elements than the given [size].
 * @param size the number of elements to take in each list,
 * must be positive and can be greater than the number of elements in this sequence.
 * The operation is _intermediate_ and _stateful_.
 */
fun <T> ResourceIterator<T>.chunked(size: Int): ResourceIterator<List<T>> =
    asInternalSequence().chunked(size).asResourceIterator { this.close() }

/**
 * Returns a resource-iterator containing first [n] elements.
 * @throws IllegalArgumentException if [n] is negative or bigger than max-int
 */
fun <T> ResourceIterator<T>.take(n: Long): ResourceIterator<T> {
    require(n >= 0 && n <= Int.MAX_VALUE) { "Requested element count $n should be in range [0, ${Int.MAX_VALUE}]." }
    return asInternalSequence().take(n.toInt()).asResourceIterator {
        this.close()
    }
}

/**
 * Returns a resource-iterator containing all elements except first [n] elements.
 * The operation is _intermediate_ and _stateless_.
 * @throws IllegalArgumentException if [n] is negative or bigger than max-int
 */
fun <T> ResourceIterator<T>.drop(n: Long): ResourceIterator<T> {
    require(n >= 0 && n <= Int.MAX_VALUE) { "Requested element count $n should be in range [0, ${Int.MAX_VALUE}]." }
    return asInternalSequence().drop(n.toInt()).asResourceIterator {
        this.close()
    }
}

/**
 * Safe closes all resources from this [Iterable] by calling [AutoCloseable.close] on each element in turn.
 * It is guaranteed that every [AutoCloseable.close] will be called even if some exception occurs.
 * Note that in case of multiple exception [rootError] is thrown,
 * if there is a single exception it will be thrown, not [rootError].
 */
fun <X> Iterable<X>.closeAll(rootError: Throwable = Exception("Error while closing iterables")) {
    forEach {
        try {
            (it as? AutoCloseable)?.close()
        } catch (ex: Exception) {
            rootError.addSuppressed(ex)
        }
    }
    if (rootError.suppressedExceptions.size == 1) {
        throw rootError.suppressedExceptions[0]
    }
    if (rootError.suppressedExceptions.isNotEmpty()) {
        throw rootError
    }
}

/**
 * Executes safely provided [operations].
 * It is guaranteed that every operation will be executed, even if some exception occurs.
 * Note that in case of multiple exception [rootError] is thrown,
 * if there is a single exception it will be thrown, not rootError.
 */
internal fun safeExec(
    vararg operations: () -> Unit,
    rootError: Throwable = Exception("Error while executing operations")
) {
    operations.forEach {
        try {
            it()
        } catch (ex: Exception) {
            rootError.addSuppressed(ex)
        }
    }
    if (rootError.suppressedExceptions.size == 1) {
        throw rootError.suppressedExceptions[0]
    }
    if (rootError.suppressedExceptions.isNotEmpty()) {
        throw rootError
    }
}

/**
 * Provides an internal [Sequence] to be used for calling terminal Sequence's methods
 * (i.e., those methods that reach the end of the underlying [ResourceIterator]).
 */
private fun <X> ResourceIterator<X>.asInternalSequence(): Sequence<X> {
    require(this is BaseResourceIterator)
    return this.asInternalSequence()
}