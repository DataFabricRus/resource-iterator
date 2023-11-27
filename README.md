## Resource Iterator

A simple JVM library which provides Closeable `kotlin.collections.Iterator` named `ResourceIterator`.
Standard kotlin-stdlib does not contain Closeable `Sequence` or `Iterator`.
This library complements the standard functionality with an extended iterator
as well as various utility methods such as `flatMap`, `filter`, etc, similar to `Sequence` methods.
Each of these methods produces an `ResourceIterator` iterator, closing which also closes the source.
`ResourceIterator` extends `Iterator`, not `Sequence`, since it is hard to control every method-extension of `Sequence`.

#### Example of use:

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

#### Available via [jitpack](https://jitpack.io/#DataFabricRus/textfile-utils):

```kotlin
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.DataFabricRus:resource-iterator:{{last_version}}'
}
```

#### Apache License Version 2.0