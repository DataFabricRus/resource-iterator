@file:OptIn(ExperimentalStdlibApi::class)

package cc.datafabric.iterators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


internal class ResourceIteratorTest {

    companion object {

        private fun assertIsClosed(isClosed: Boolean, iterator: Iterator<Any>) {
            assertTrue(isClosed)
            repeat(2) {
                assertFailsWith(IllegalStateException::class) {
                    iterator.next()
                }
                assertFalse(iterator.hasNext())
            }
        }

        private fun consume(any: Any) {
            assertNotNull(any)
        }
    }

    @Test
    fun testFirst() {
        var isClosed = false
        var i = 0
        val n = 10
        val iterator = generateResourceIterator({ if (i++ < n) i else null }) { isClosed = true }
        assertEquals(1, iterator.first())
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testFirstOrNull() {
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ null }) { isClosed1 = true }
        assertNull(iterator1.firstOrNull())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        var i = 0
        val n = 10
        val iterator2 = generateResourceIterator({ if (i++ < n) i else null }) { isClosed2 = true }
        assertEquals(1, iterator2.firstOrNull())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testToList() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b") { isClosed = true }
        val res = iterator.toList()
        assertEquals(listOf("a", "b"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testToSet() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b", "c", "b", "c") { isClosed = true }
        val res = iterator.toSet()
        assertEquals(setOf("a", "b", "c"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testForEach() {
        var isClosed = false
        val iterator = resourceIteratorOf("a", "b") { isClosed = true }
        val res = mutableListOf<String>()
        iterator.forEach { res.add(it) }
        assertEquals(listOf("a", "b"), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testFilterAndToList() {
        var isClosed1 = false
        val source1 = resourceIteratorOf("a", "b", "c", "d") { isClosed1 = true }
        val iterator1 = source1.filter { it == "b" || it == "c" }
        assertEquals(listOf("b", "c"), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }
        val iterator2 = source2.filter { it > 2 }.filter { it < 6 }
        assertEquals(listOf(3, 4), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testTakeWhileAndOnEachAndToList() {
        var isClosed1 = false
        var count1 = 0
        val source1 = resourceIteratorOf(1, 2, 3, 1, 2, 3, 1, 2, 3) { isClosed1 = true }.onEach { count1++ }
        val iterator1 = source1.takeWhile { it < 3 }
        assertEquals(listOf(1, 2), iterator1.toList())
        assertEquals(3, count1)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        var count2 = 0
        val source2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }.onEach { count2++ }
        val iterator2 = source2.takeWhile { it < 4 }.takeWhile { it < 2 }
        assertEquals(listOf(1), iterator2.toList())
        assertEquals(2, count2)
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testMapToSet() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(1, 2, 3) { isClosed1 = true }
        val iterator1 = source1.map { it.toString() }
        assertEquals(setOf("1", "2", "3"), iterator1.toSet())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf("a", "b") { isClosed2 = true }
        val iterator2 = source2.map { it.toCharArray()[0] }.map { it.uppercase() }
        assertEquals(setOf("A", "B"), iterator2.toSet())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testFilterMapForEach() {
        var isClosed = false
        val source = resourceIteratorOf(1, 2, 3) { isClosed = true }
        val iterator = source.filter { it > 1 }.map { it.toString() }.map { it.toInt() }.filter { it < 42 }
        val res = mutableListOf<Int>()
        iterator.forEach { res.add(it) }
        assertEquals(listOf(2, 3), res)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testFlatMapIterableToList() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(listOf(1, 2), listOf(3, 4), listOf(5)) { isClosed1 = true }
        val iterator1 = source1.flatMap { it }
        assertEquals(listOf(1, 2, 3, 4, 5), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(
            listOf(listOf(1), emptyList()),
            listOf(listOf(2, 3, 4), listOf(5, 6)),
            listOf(listOf(7))
        ) { isClosed2 = true }
        val iterator2 = source2.flatMap { it }.flatMap { it }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        val source3 = resourceIteratorOf(
            "a,b,c",
            "d,e",
            "f",
            "g,h"
        ) { isClosed3 = true }
        val iterator3 = source3.flatMap { it.split(",") }
        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h"), iterator3.toList())
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun testFlatMapIteratorAndForEach() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(listOf(1, 2), listOf(3, 4), listOf(5)) { isClosed1 = true }
        val iterator1 = source1.flatMap { it.iterator() }
        assertEquals(listOf(1, 2, 3, 4, 5), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = 0
        val iterator2 = resourceIteratorOf("a", "b") { isClosed2++ }
            .asResourceIterator().flatMap {
                resourceIteratorOf("${it}1", "${it}2", "${it}3") { isClosed2++ }
            }
        val res = mutableListOf<String>()
        iterator2.forEach { res.add(it) }
        assertEquals(listOf("a1", "a2", "a3", "b1", "b2", "b3"), res)
        assertEquals(3, isClosed2)
    }

    @Test
    fun testDistinctForEach() {
        var isClosed = false
        val source = resourceIteratorOf("a", "b", "c", "c", "b", "b", "b", "d") { isClosed = true }
        val iterator = source.distinct()
        val items = setOf("a", "b", "c", "d")
        var count = 0
        iterator.forEach {
            count++
            assertTrue(items.contains(it))
        }
        assertEquals(items.size, count)
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testConcatAndForEach() {
        var isClosed1 = false
        var isClosed2 = false
        var isClosed3 = false
        val source1 = resourceIteratorOf("a", "b") { isClosed1 = true }
        val source2 = resourceIteratorOf("c", "d") { isClosed2 = true }
        val source3 = resourceIteratorOf("e") { isClosed3 = true }
        val iterator = source1 + source2 + source3
        val actual = mutableListOf<String>()
        iterator.forEach {
            actual.add(it)
        }
        assertEquals(listOf("a", "b", "c", "d", "e"), actual)
        assertIsClosed(isClosed1, source1)
        assertIsClosed(isClosed2, source2)
        assertIsClosed(isClosed3, source3)
    }

    @Test
    fun testForCycle() {
        var isClosed1 = false
        val iterator1 = resourceIteratorOf("a", "b") { isClosed1 = true }
        var count1 = 0
        for (e in iterator1) {
            consume(e)
            count1++
        }
        assertEquals(2, count1)
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
        assertEquals(n, count2)
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testAnyForEach() {
        var isClosed1 = false
        val source1 = resourceIteratorOf(1, 2, 3) { isClosed1 = true }
        val res1 = source1.asResourceIterator().any { it > 1 }
        assertTrue(res1)
        assertTrue(isClosed1)

        var isClosed2 = false
        val source2 = resourceIteratorOf(1, 2, 3) { isClosed2 = true }
        val res2 = source2.asResourceIterator().any { it > 4 }
        assertFalse(res2)
        assertTrue(isClosed2)
    }

    @Test
    fun testEmptyIteratorAndPlusAndToList() {
        assertFalse(emptyResourceIterator<String>().map { 42 }.hasNext())
        assertFalse(emptyResourceIterator<String>().filter { true }.hasNext())
        assertFalse(emptyResourceIterator<String>().flatMap { resourceIteratorOf("a") }.hasNext())

        var isClosed1 = false
        val iterator1 = emptyResourceIterator<Int>() + resourceIteratorOf(42) { isClosed1 = true }
        assertEquals(listOf(42), iterator1.toList())
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(42) { isClosed2 = true } + emptyResourceIterator()
        assertEquals(listOf(42), iterator2.toList())
        assertIsClosed(isClosed2, iterator2)
    }

    @Test
    fun testCount() {
        var isClosed = false
        val iterator = resourceIteratorOf(1, 2, 3) { isClosed = true }
        assertEquals(3, iterator.count())
        assertTrue(isClosed)
    }

    @Test
    fun testAsSafeSequence() {
        var i = 0
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ if (i++ < 10) i else null }) { isClosed1 = true }
        val count1 = iterator1.asSafeSequence().count()
        assertEquals(10, count1)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(1, 2) { isClosed2 = true }
        val count2 =
            iterator2.asSafeSequence().filter { it < 5 }.map { it * it }.filter { it > 4 }.map { it + it }.count()
        assertEquals(0, count2)
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        var count3 = 0
        val iterator3 = resourceIteratorOf(1, 2, 3) { isClosed3 = true }
        iterator3.asSafeSequence().take(1).forEach { _ ->
            count3++
        }
        assertEquals(1, count3)
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun testIterateTwice() {
        var i = 0
        var isClosed1 = false
        val iterator1 = generateResourceIterator({ if (i++ < 10) i else null }) { isClosed1 = true }
        assertEquals(10, iterator1.toList().size)
        assertIsClosed(isClosed1, iterator1)
        assertEquals(0, iterator1.toList().size)
        assertIsClosed(isClosed1, iterator1)

        var isClosed2 = false
        val iterator2 = resourceIteratorOf(1, 2, 3, 4) { isClosed2 = true }
        assertEquals(4, iterator2.toList().size)
        assertIsClosed(isClosed2, iterator2)
        assertEquals(0, iterator2.toList().size)
        assertIsClosed(isClosed2, iterator2)

        var isClosed3 = false
        val iterator3 = listOf("a", "b").asResourceIterator { isClosed3 = true }
        assertEquals(2, iterator3.toList().size)
        assertIsClosed(isClosed3, iterator3)
        assertEquals(0, iterator3.toList().size)
        assertIsClosed(isClosed3, iterator3)
    }

    @Test
    fun testFlatMapAndFilterAndFindFirst() {
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
            assertEquals(21, it.firstOrNull())
        }
        assertEquals(1, closeOuter)
        assertEquals(1, closeInner)
    }

    @Test
    fun testNextOrNull() {
        assertNull(emptyResourceIterator<Any>().nextOrNull())
        assertNotNull(resourceIteratorOf(42).nextOrNull())
    }

    @Test
    fun testSequenceAsResourceIteratorAndCount() {
        var onClose = 0
        assertEquals(
            3,
            sequenceOf("a", "b", "c").asResourceIterator { onClose++ }.count()
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testMapAsResourceIteratorAndCount() {
        var onClose = 0
        assertEquals(
            3,
            mapOf("a" to "A", "b" to "B", "c" to "C").asResourceIterator { onClose++ }.count()
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testSortByToList() {
        val source = listOf(78, 2, 49, 8, 32, 42, 32, 3, 43, 34, 1, 42)
        val expected = source.map { it.toString() }.sorted().map { it.toInt() }

        val actual = source.asResourceIterator().sortedBy { it.toString() }.toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testSortedToList() {
        val source = listOf("h", "f", "D", "e", "m")
        val expected = source.sorted()
        var onClose = 0

        val actual = source.asResourceIterator { onClose++ }.sorted()
        assertEquals(expected, actual.toList())
        assertEquals(1, onClose)
    }

    @Test
    fun testMapNotNullAndToList() {
        var onClose = 0
        assertEquals(
            listOf("A"),
            resourceIteratorOf(1, 2) {
                onClose++
            }.mapNotNull { if (it == 1) null else "A" }.toList(),
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testGroupBy() {
        var onClose = 0
        val res = resourceIteratorOf(56, 2, 87, 99, 99, 99, 99, 2) { onClose++ }.groupBy { it.toString() }
        assertEquals(
            mapOf("56" to listOf(56), "2" to listOf(2, 2), "87" to listOf(87), "99" to listOf(99, 99, 99, 99)),
            res
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testAssociateBy() {
        var onClose = 0
        val res = resourceIteratorOf(4, 3, 5, 2, 6, 1, 7) { onClose++ }.associateBy { it.toString() }
        assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testAssociate() {
        var onClose = 0
        val res = resourceIteratorOf(4, 3, 5, 2, 6, 1, 7) { onClose++ }.associate { it.toString() to it }
        assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testToMap() {
        var onClose = 0
        val res = listOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7)
            .asResourceIterator { onClose++ }.toMap()
        assertEquals(
            mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7),
            res,
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testFold() {
        var onClose = 0
        val res = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }
            .fold(0) { a, b -> a + b }
        assertEquals(39, res)
        assertEquals(1, onClose)
    }

    @Test
    fun testAll() {
        var onClose = 0
        val res1 = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.all { it > 0 }
        assertTrue(res1)
        assertEquals(1, onClose)
        val res2 = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.all { it < 6 }
        assertFalse(res2)
        assertEquals(2, onClose)
    }

    @Test
    fun testOnEachAndCount() {
        var onClose = 0
        var count = 0
        val res = resourceIteratorOf(5, 6, 4, 3, 5, 2, 6, 1, 7) { onClose++ }.onEach {
            count++
        }.count()
        assertEquals(9, count)
        assertEquals(9, res)
        assertEquals(1, onClose)
    }

    @Test
    fun testAssociateByTo() {
        var onClose = 0
        val res: MutableMap<String, Int> = resourceIteratorOf(42, 42, 4242, 424242) { onClose++ }
            .associateByTo(mutableMapOf()) { it.toString() }
        assertEquals(
            mapOf(
                "42" to 42, "4242" to 4242, "424242" to 424242
            ),
            res
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testAssociateTo() {
        var onClose = 0
        val res: MutableMap<Int, Int> = resourceIteratorOf(42, 42, 4242, 424242) { onClose++ }
            .associateTo(mutableMapOf()) { a -> a / 2 to a * 2 }
        assertEquals(
            mapOf(
                21 to 84,
                2121 to 8484,
                212121 to 848484,
            ),
            res
        )
        assertEquals(1, onClose)
    }

    @Test
    fun testChunked() {
        var isClosed = false
        val iterator = (1..42).asResourceIterator { isClosed = true }
        val res = iterator.chunked(5).toList()
        assertEquals(9, res.size)
        res.forEachIndexed { index, list ->
            if (index == res.size - 1) {
                assertEquals(2, list.size)
            } else {
                assertEquals(5, list.size)
            }
        }
        assertIsClosed(isClosed, iterator)
    }

    @Test
    fun testToCollection() {
        var isClosed = false
        val res = (1..42).asResourceIterator { isClosed = true }.toCollection(hashSetOf())
        assertEquals(42, res.size)
        assertTrue(isClosed)
    }
}