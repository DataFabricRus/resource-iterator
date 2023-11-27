## Resource Iterator

A simple JVM library providing `ResourceIterator`,
which is an extended `kotlin.collections.Iterator` implementing `java.lang.AutoCloseable` interface:

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

#### Examples of use:

```kotlin
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
    implementation("com.github.DataFabricRus:resource-iterator:{{latest_version}}")
}
```

#### Apache License Version 2.0