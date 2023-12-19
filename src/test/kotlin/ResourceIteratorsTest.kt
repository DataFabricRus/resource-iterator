package cc.datafabric.iterators

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ResourceIteratorTest {

    companion object {

        private fun assertIsClosed(isClosed: Boolean, iterator: Iterator<Any>) {
            Assertions.assertTrue(isClosed)
            repeat(2) {
                Assertions.assertThrows(IllegalStateException::class.java) {
                    iterator.next()
                }
                Assertions.assertFalse(iterator.hasNext())
            }
        }

        private fun consume(any: Any) {
            Assertions.assertNotNull(any)
        }
    }

    @Test
    fun `test first`() {
        var isClosed = false
        var i = 0
        val n = 10
        val iterator = generateResourceIterator({ if (i++ < n) i else null }) { isClosed = true }
        Assertions.assertEquals(1, iterator.first())
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test firstOrNull`() {
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ null }) { isClosed1 = true }
        Assertions.assertNull(iterator1.firstOrNull())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        var i = 0
        val n = 10
        val iterator2 = generateResourceIterator({ if (i++ < n) i else null }) { isClosed2 = true }
        Assertions.assertEquals(1, iterator2.firstOrNull())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test toList`() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b") { isClosed = true }
        val res = iterator.toList()
        Assertions.assertEquals(listOf("a", "b"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test toSet`() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b", "c", "b", "c") { isClosed = true }
        val res = iterator.toSet()
        Assertions.assertEquals(setOf("a", "b", "c"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test forEach`() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b") { isClosed = true }
        val res = mutableListOf<String>()
        iterator.forEach { res.add(it) }
        Assertions.assertEquals(listOf("a", "b"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test forEachRemaining`() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b") { isClosed = true }
        val res = mutableListOf<String>()
        iterator.forEachRemaining { res.add(it) }
        Assertions.assertEquals(listOf("a", "b"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test filter toList`() {
        var isClosed1 = false
        val source1 = resourceIteratorOf("a", "b", "c", "d") { isClosed1 = true }
        val iterator1 = source1.filter { it == "b" || it == "c" }
        Assertions.assertEquals(listOf("b", "c"), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }
        val iterator2 = source2.filter { it > 2 }.filter { it < 6 }
        Assertions.assertEquals(listOf(3, 4), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test takeWhile onEach toList`() {
        var isClosed1 = false
        var count1 = 0
        val source1 = resourceIteratorOf(1, 2, 3, 1, 2, 3, 1, 2, 3) { isClosed1 = true }.onEach { count1++ }
        val iterator1 = source1.takeWhile { it < 3 }
        Assertions.assertEquals(listOf(1, 2), iterator1.toList())
        Assertions.assertEquals(3, count1)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        var count2 = 0
        val source2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }.onEach { count2++ }
        val iterator2 = source2.takeWhile { it < 4 }.takeWhile { it < 2 }
        Assertions.assertEquals(listOf(1), iterator2.toList())
        Assertions.assertEquals(2, count2)
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test map toSet`() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(1, 2, 3) { isClosed1 = true }
        val iterator1 = source1.map { it.toString() }
        Assertions.assertEquals(setOf("1", "2", "3"), iterator1.toSet())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf("a", "b") { isClosed2 = true }
        val iterator2 = source2.map { it.toCharArray()[0] }.map { it.uppercase() }
        Assertions.assertEquals(setOf("A", "B"), iterator2.toSet())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test filter map forEach`() {
        var isClosed = false
        val source = resourceIteratorOf(1, 2, 3) { isClosed = true }
        val iterator = source.filter { it > 1 }.map { it.toString() }.map { it.toInt() }.filter { it < 42 }
        val res = mutableListOf<Int>()
        iterator.forEach { res.add(it) }
        Assertions.assertEquals(listOf(2, 3), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test flatMapIterable toList`() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(listOf(1, 2), listOf(3, 4), listOf(5)) { isClosed1 = true }
        val iterator1 = source1.flatMap { it }
        Assertions.assertEquals(listOf(1, 2, 3, 4, 5), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(
            listOf(listOf(1), emptyList()),
            listOf(listOf(2, 3, 4), listOf(5, 6)),
            listOf(listOf(7))
        ) { isClosed2 = true }
        val iterator2 = source2.flatMap { it }.flatMap { it }
        Assertions.assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        val source3 = resourceIteratorOf(
            "a,b,c",
            "d,e",
            "f",
            "g,h"
        ) { isClosed3 = true }
        val iterator3 = source3.flatMap { it.split(",") }
        Assertions.assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h"), iterator3.toList())
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun `test flatMapIterator forEach`() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(listOf(1, 2), listOf(3, 4), listOf(5)) { isClosed1 = true }
        val iterator1 = source1.flatMap { it.iterator() }
        Assertions.assertEquals(listOf(1, 2, 3, 4, 5), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = 0
        val iterator2 = resourceIteratorOf("a", "b") { isClosed2++ }
            .asResourceIterator().flatMap {
                resourceIteratorOf("${it}1", "${it}2", "${it}3") { isClosed2++ }
            }
        val res = mutableListOf<String>()
        iterator2.forEach { res.add(it) }
        Assertions.assertEquals(listOf("a1", "a2", "a3", "b1", "b2", "b3"), res)
        Assertions.assertEquals(3, isClosed2)
    }

    @Test
    fun `test distinct forEach`() {
        var isClosed = false
        val source = resourceIteratorOf("a", "b", "c", "c", "b", "b", "b", "d") { isClosed = true }
        val iterator = source.distinct()
        val items = setOf("a", "b", "c", "d")
        var count = 0
        iterator.forEachRemaining {
            count++
            Assertions.assertTrue(items.contains(it))
        }
        Assertions.assertEquals(items.size, count)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun `test concat forEach`() {
        var isClosed1 = false
        var isClosed2 = false
        var isClosed3 = false
        val source1 = resourceIteratorOf("a", "b") { isClosed1 = true }
        val source2 = resourceIteratorOf("c", "d") { isClosed2 = true }
        val source3 = resourceIteratorOf("e") { isClosed3 = true }
        val iterator = source1 + source2 + source3
        val actual = mutableListOf<String>()
        iterator.forEachRemaining {
            actual.add(it)
        }
        Assertions.assertEquals(listOf("a", "b", "c", "d", "e"), actual)
        assertIsClosed(isClosed1, source1)
        assertIsClosed(isClosed2, source2)
        assertIsClosed(isClosed3, source3)
    }

    @Test
    fun `test for-cycle`() {
        var isClosed1 = false
        val iterator1 = resourceIteratorOf("a", "b") { isClosed1 = true }
        var count1 = 0
        for (e in iterator1) {
            consume(e)
            count1++
        }
        Assertions.assertEquals(2, count1)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        var i = 0
        val n = 10
        val iterator2 = generateResourceIterator({ if (i++ < n) i else null }) { isClosed2 = true }
        var count2 = 0
        for (e in iterator2) {
            consume(e)
            count2++
        }
        Assertions.assertEquals(n, count2)
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test any forEach`() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(1, 2, 3) { isClosed1 = true }
        val res1 = source1.asResourceIterator().any { it > 1 }
        Assertions.assertTrue(res1)
        Assertions.assertTrue(isClosed1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(1, 2, 3) { isClosed2 = true }
        val res2 = source2.asResourceIterator().any { it > 4 }
        Assertions.assertFalse(res2)
        Assertions.assertTrue(isClosed2)
    }

    @Test
    fun `test emptyIterator plus toList`() {
        Assertions.assertFalse(emptyResourceIterator<String>().map { 42 }.hasNext())
        Assertions.assertFalse(emptyResourceIterator<String>().filter { true }.hasNext())
        Assertions.assertFalse(emptyResourceIterator<String>().flatMap { resourceIteratorOf("a") }.hasNext())

        var isClosed1 = false
        val iterator1 = emptyResourceIterator<Int>() + resourceIteratorOf(42) { isClosed1 = true }
        Assertions.assertEquals(listOf(42), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(42) { isClosed2 = true } + emptyResourceIterator()
        Assertions.assertEquals(listOf(42), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun `test count`() {
        var isClosed = false
        val iterator = resourceIteratorOf(1, 2, 3) { isClosed = true }
        Assertions.assertEquals(3, iterator.count())
        Assertions.assertTrue(isClosed)
    }

    @Test
    fun `test asSafeSequence`() {
        var i = 0
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ if (i++ < 10) i else null }) { isClosed1 = true }
        val count1 = iterator1.asSafeSequence().count()
        Assertions.assertEquals(10, count1)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(1, 2) { isClosed2 = true }
        val count2 =
            iterator2.asSafeSequence().filter { it < 5 }.map { it * it }.filter { it > 4 }.map { it + it }.count()
        Assertions.assertEquals(0, count2)
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        var count3 = 0
        val iterator3 = resourceIteratorOf(1, 2, 3) { isClosed3 = true }
        iterator3.asSafeSequence().take(1).forEach { _ ->
            count3++
        }
        Assertions.assertEquals(1, count3)
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun `test iterate twice`() {
        var i = 0
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ if (i++ < 10) i else null }) { isClosed1 = true }
        Assertions.assertEquals(10, iterator1.toList().size)
        assertIsClosed(isClosed1, iterator1)
        Assertions.assertEquals(0, iterator1.toList().size)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }
        Assertions.assertEquals(4, iterator2.toList().size)
        assertIsClosed(isClosed2, iterator2)
        Assertions.assertEquals(0, iterator2.toList().size)
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        val iterator3 = listOf("a", "b").asResourceIterator { isClosed3 = true }
        Assertions.assertEquals(2, iterator3.toList().size)
        assertIsClosed(isClosed3, iterator3)
        Assertions.assertEquals(0, iterator3.toList().size)
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun `test flatMap filter findFirst`() {
        var closeOuter = 0
        var closeInner = 0
        val it1 = listOf<Long>(1, 2).asResourceIterator {
            closeOuter++
        }

        val it2 = it1.flatMap { x ->
            resourceIteratorOf(21 * x, 42 * x) {
                closeInner++
            }
        }
        val it3 = it2
            .filter { it > 0 }

        it3.use {
            Assertions.assertEquals(21, it.firstOrNull())
        }
        Assertions.assertEquals(1, closeOuter)
        Assertions.assertEquals(1, closeInner)
    }

    @Test
    fun `test nextOrNull`() {
        Assertions.assertNull(emptyResourceIterator<Any>().nextOrNull())
        Assertions.assertNotNull(resourceIteratorOf(42).nextOrNull())
    }

    @Test
    fun `test Sequence#asResourceIterator count`() {
        var onClose = 0
        Assertions.assertEquals(
            3,
            sequenceOf("a", "b", "c").asResourceIterator { onClose++ }.count()
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test Map#asResourceIterator count`() {
        var onClose = 0
        Assertions.assertEquals(
            3,
            mapOf("a" to "A", "b" to "B", "c" to "C").asResourceIterator { onClose++ }.count()
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test sortBy toList`() {
        val source = listOf(78, 2, 49, 8, 32, 42, 32, 3, 43, 34, 1, 42)
        val expected = source.map { it.toString() }.sorted().map { it.toInt() }

        val actual = source.asResourceIterator().sortedBy { it.toString() }.toList()
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test sorted toList`() {
        val source = listOf("h", "f", "D", "e", "m")
        val expected = source.sorted()
        var onClose = 0

        val actual = source.asResourceIterator { onClose++ }.sorted()
        Assertions.assertInstanceOf(ResourceIterator::class.java, actual)
        Assertions.assertEquals(expected, actual.toList())
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test mapNotNull toList`() {
        var onClose = 0
        Assertions.assertEquals(
            listOf("A"),
            resourceIteratorOf(1, 2) {
                onClose++
            }.mapNotNull { if (it == 1) null else "A" }.toList(),
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test groupBy`() {
        var onClose = 0
        val res = resourceIteratorOf(56, 2, 87, 99, 99, 99, 99, 2) { onClose++ }.groupBy { it.toString() }
        Assertions.assertEquals(
            mapOf("56" to listOf(56), "2" to listOf(2, 2), "87" to listOf(87), "99" to listOf(99, 99, 99, 99)),
            res
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test associateBy`() {
        var onClose = 0
        val res = resourceIteratorOf(4, 3, 5, 2, 6, 1, 7) { onClose++ }.associateBy { it.toString() }
        Assertions.assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test associate`() {
        var onClose = 0
        val res = resourceIteratorOf(4, 3, 5, 2, 6, 1, 7) { onClose++ }.associate { it.toString() to it }
        Assertions.assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test toMap`() {
        var onClose = 0
        val res = listOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7)
            .asResourceIterator { onClose++ }.toMap()
        Assertions.assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test toSortedSet`() {
        var onClose = 0
        val res = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.toSortedSet()
        Assertions.assertEquals(
            sortedSetOf(1, 2, 3, 4, 5, 6, 7),
            res,
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test fold`() {
        var onClose = 0
        val res = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }
            .fold(0) { a, b -> a + b }
        Assertions.assertEquals(39, res)
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test all`() {
        var onClose = 0
        val res1 = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.all { it > 0 }
        Assertions.assertTrue(res1)
        Assertions.assertEquals(1, onClose)
        val res2 = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.all { it < 6 }
        Assertions.assertFalse(res2)
        Assertions.assertEquals(2, onClose)
    }

    @Test
    fun `test onEach count`() {
        var onClose = 0
        var count = 0
        val res = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.onEach {
            count++
        }.count()
        Assertions.assertEquals(9, count)
        Assertions.assertEquals(9, res)
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test associateByTo`() {
        var onClose = 0
        val res: MutableMap<String, Int> = resourceIteratorOf(42, 42, 4242, 424242) { onClose++ }
            .associateByTo(mutableMapOf()) { it.toString() }
        Assertions.assertEquals(
            mapOf(
                "42" to 42, "4242" to 4242, "424242" to 424242
            ),
            res
        )
        Assertions.assertEquals(1, onClose)
    }

    @Test
    fun `test associateTo`() {
        var onClose = 0
        val res: MutableMap<Int, Int> = resourceIteratorOf(42, 42, 4242, 424242) { onClose++ }
            .associateTo(mutableMapOf()) { a -> a / 2 to a * 2 }
        Assertions.assertEquals(
            mapOf(
                21 to 84,
                2121 to 8484,
                212121 to 848484,
            ),
            res
        )
        Assertions.assertEquals(1, onClose)
    }
}