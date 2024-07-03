## Resource Iterator

[![](https://jitpack.io/v/DataFabricRus/resource-iterator.svg)](https://jitpack.io/#DataFabricRus/resource-iterator)

A simple kotlin library providing `ResourceIterator`,
which is an extended `kotlin.collections.Iterator` implementing `AutoCloseable` interface:

```kotlin
interface ResourceIterator<out X> : Iterator<X>, AutoCloseable {
    ...
}
```

The standard kotlin-stdlib does not contain `Sequence` or `Iterator` which implement `AutoCloseable` interface
(e.g., see open discussion: [KT-34719](https://youtrack.jetbrains.com/issue/KT-34719/Closeable-Sequences)).

This library complements the standard kotlin functionality with an extended iterator
as well as various utility methods such as `flatMap`, `filter`, etc., similar to `Sequence` methods.
Each of these methods produces a subsequent `ResourceIterator`, closing which also closes the source iterator.
The `ResourceIterator` extends `Iterator`, not `Sequence`, since it is hard to control every standard method-extension
of `Sequence`.

#### Known alternatives:

- JDK `java.util.stream.Stream`
- Apache Jena `org.apache.jena.util.iterator.ExtendedIterator`
- Apache Kafka `org.apache.kafka.common.utils.CloseableIterator`

#### When to use:

If you have some resources which need to be closed after or during iteration,
and you don't want to use JDK or some external heavy library.
For example, `ResourceIterator` is used by [textfile-utils](https://github.com/DataFabricRus/textfile-utils)
(see `cc.datafabric.textfileutils.files.LineReade.kt`).

#### Examples of use:

```kotlin
// with standard collections:
listOf<Long>(1, 2).asResourceIterator { println("close outer") }
    .flatMap { x ->
        resourceIteratorOf(21 * x, 42 * x) { println("close inner for $x") }
    }
    .filter { it > 0 }
    .use {
        println("it = ${it.firstOrNull()}")
    }
```

```kotlin
// with jdbc:
fun <X> Connection.executeQuery(
    query: String,
    extractor: (ResultSet) -> X,
    exec: PreparedStatement.() -> ResultSet
): ResourceIterator<X> {
    val statement = prepareStatement(query)
    val rs = statement.exec()
    return generateResourceIterator({ if (rs.next()) extractor(rs) else null }) { statement.close() }
}
```

#### Available via [jitpack](https://jitpack.io/#DataFabricRus/resource-iterator):

```kotlin
repositories {
    ...
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.DataFabricRus.resource-iterator:resource-iterator-jvm:{{latest_version}}")
}
```

#### Apache License Version 2.0