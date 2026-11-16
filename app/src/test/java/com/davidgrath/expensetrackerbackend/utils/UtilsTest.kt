package com.davidgrath.expensetrackerbackend.utils

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import java.math.BigInteger
import java.util.UUID
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

class UtilsTest {
    @Test
    fun uuidToBytesTest() {
        val expectedBytes = listOf<Int>(0xfa, 0xe0, 0x75, 0xe1, 0xd5, 0x4a, 0x48, 0x57, 0xa7, 0x6b, 0xb3, 0xea, 0xdf, 0x88, 0xd6, 0x02).map { it.toByte() }.toTypedArray().toByteArray()
        println("" + Byte.MAX_VALUE + " " + Byte.MIN_VALUE)
        val uuid = UUID.fromString("fae075e1-d54a-4857-a76b-b3eadf88d602")
        assertEquals("fae075e1-d54a-4857-a76b-b3eadf88d602", uuid.toString())
        val bytes = uuidToBytes(uuid)
        assertEquals(expectedBytes.toList(), bytes.toList())
    }

    @Test
    fun bytesToUuidTest() {
        val expectedUuid = UUID.fromString("fae075e1-d54a-4857-a76b-b3eadf88d602")
        val bytes = listOf<Int>(0xfa, 0xe0, 0x75, 0xe1, 0xd5, 0x4a, 0x48, 0x57, 0xa7, 0x6b, 0xb3, 0xea, 0xdf, 0x88, 0xd6, 0x02).map { it.toByte() }.toTypedArray().toByteArray()
        assertEquals("fae075e1-d54a-4857-a76b-b3eadf88d602", expectedUuid.toString())
        val uuid = bytesToUUID(bytes)
        assertEquals(expectedUuid, uuid)
    }


    /**
     * Unicode TR44
     */
    @Test
    fun validateGenericSingleLineStringTest() {
        //TODO Emoji are a complicated case, but they're allowed for now. Normalisation, hehe
        val controlChar = "\u0001"
        /**
         * TODO I should probably extract these Test values shared with [com.davidgrath.expensetrackerbackend.GeneralConstraintsTest] into a dedicated file
         */
        val mark = "\u0300"
        val separators = " \u2028\u2029"
        val softHyphen = '\u00ad'
        val format = "$softHyphen"
        val completeNonsenseString = "$mark$separators$format"
        val controlCharsWhitespace = "\r\n\t"
        val queryWithMark = "query$mark"
        val queryWithControlCharsAndSeparators = "query$controlChar$separators"
        val queryWithControlCharsWhitespace = "query$controlCharsWhitespace"
        val emoji = "🙂"
        assertTrue(validateGenericSingleLineString("search"))
        assertTrue(validateGenericSingleLineString(emoji))
        assertTrue(validateGenericSingleLineString(queryWithMark))

        assertFalse(validateGenericSingleLineString(queryWithControlCharsWhitespace))
        assertFalse(validateGenericSingleLineString(""))
        assertFalse(validateGenericSingleLineString(completeNonsenseString))
        assertFalse(validateGenericSingleLineString(queryWithControlCharsAndSeparators))
        assertFalse(validateGenericSingleLineString(controlCharsWhitespace))
    }

    @Test
    fun earliestOccupancyTest() {
        val max = 5
        val filledUp = (1..5).toList().map { it.toShort() }.toShortArray()
        assertNull(findEarliestOccupancySlot(filledUp, max))

        val filledExceptLast = (1..4).toList().map { it.toShort() }.toShortArray()
        assertEquals(5.toShort(), findEarliestOccupancySlot(filledExceptLast, max))

        val empty = ShortArray(0)
        assertEquals(1.toShort(), findEarliestOccupancySlot(empty, 5))

        val filledUpExceptOneBetweenLastAndFirst = (arrayOf(1) + arrayOf(3,4,5)).map { it.toShort() }.toShortArray()
        assertEquals(2.toShort(), findEarliestOccupancySlot(filledUpExceptOneBetweenLastAndFirst, 5))

        val sparse = arrayOf(1,3,5).map { it.toShort() }.toShortArray()
        assertEquals(2.toShort(), findEarliestOccupancySlot(sparse, 5))

        val tooLarge = arrayOf(1,2,3,4,5,5).toList().map { it.toShort() }.toShortArray()
        Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            findEarliestOccupancySlot(tooLarge, max)
        })

        val elementTooBig = arrayOf(1,2,3,4,6).map { it.toShort() }.toShortArray()
        Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            findEarliestOccupancySlot(elementTooBig, max)
        })

        val elementTooSmall = arrayOf(0,2,3,4,5).map { it.toShort() }.toShortArray()
        Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            findEarliestOccupancySlot(elementTooSmall, max)
        })

        val invalidMax = 0
        Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            findEarliestOccupancySlot(ShortArray(0), invalidMax)
        })
    }


    class MapMover<T: Comparable<T>>(private val arrayOrDatabase: MutableMap<T, Short>): Mover<T> {

        override fun moveSortKey(id: T, sortKey: Short) {
            if(arrayOrDatabase.containsValue(sortKey)) {
                if(arrayOrDatabase[id] != sortKey) {
                    throw RuntimeException("Unique sort key constraint broken: $arrayOrDatabase; $id; $sortKey")
                }
            }
            arrayOrDatabase[id] = sortKey

        }
    }

    @Test
    fun basicRebalanceTest() {

        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (16..45).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_530L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..30) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }
        rebalance(1_000, BigInteger(60.toString()), placeholderCapacitySlots, MapMover(placeholderCapacitySlots))
        val ids = (1_501L..1_530L).shuffled(Random(seed)).toList().toTypedArray()
        val sortKeys = ids.map { placeholderCapacitySlots[it]!! }
        val stepSequence = ((2..60).step(2)).toList().map { it.toShort() }
        assertEquals(stepSequence, sortKeys)
    }


    @Test
    fun givenPopulationCountExceedsHalfOfSortKeySpaceWhenRebalanceThenItemsContiguous() {

        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (16..46).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_531L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..31) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }
        rebalance(1_000, BigInteger(60.toString()), placeholderCapacitySlots, MapMover(placeholderCapacitySlots))
        val ids = (1_501L..1_531L).toList().shuffled(Random(seed)).toTypedArray()
        val sortKeys = ids.map { placeholderCapacitySlots[it]!! }
        val sequence = ((1..31)).toList().map { it.toShort() }
        assertEquals(sequence, sortKeys)
    }

    @Test
    fun givenPopulationCountExceedsHalfOfSortKeySpaceWhenGetNextSortKeyThenException() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (1..31).toList().map { it.toShort() }.toTypedArray()
        println(placeHolderExistingSortKeys.toList())
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_531L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..31) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }

        Assert.assertThrows(RuntimeException::class.java) {
            getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(60.toString()))
        }
    }

    @Test
    fun givenPopulationCountExactlyHalfOfSortKeySpaceWhenGetNextSortKeyThenException() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (1..30).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_530L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..30) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }

        Assert.assertThrows(RuntimeException::class.java) {
            getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(60.toString()))
        }
    }

    @Test
    fun getNextSortKeyOkTest() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (1..29).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_529L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..29) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }
        val next = getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(60.toString()))
        assertEquals(60, next)

    }
    @Test
    fun getNextSortKeyOkTest2() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (3..87).step(3).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_529L).toList().shuffled(random).toTypedArray()

        //seed "database"
        for(i in 1..29) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }
        val next = getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(90.toString()), 3)
        assertEquals(90, next)

    }

    @Test
    fun givenGapFromLastKeyToLimitSmallerThanPreferredFactorWhenGetSortKeyThenRebalance() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = (6..84).step(6).toList().map { it.toShort() }.toTypedArray()
        val seed = 401L
        val random = Random(seed)
        val placeHolderIds = (1_501L..1_514L).toList().shuffled(random).toTypedArray()
        val preferredSpace: Short = 5
        //seed "database"
        for(i in 1..14) {
            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
        }

        val next = getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(90.toString()), preferredSpace)

        val ids = (1_501L..1_514L).shuffled(Random(seed)).toList().toTypedArray()
        val sortKeys = ids.map { placeholderCapacitySlots[it]!! }
        val stepSequence = ((5..70).step(5)).toList().map { it.toShort() }

        assertEquals(stepSequence, sortKeys)
        assertEquals(75, next)
    }

    @Test
    fun givenCollectionIsEmptyWhenGetNextSortKeyThenReturnPreferredSpace() {
        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
        val placeHolderExistingSortKeys = ShortArray(0)

        val placeHolderIds = LongArray(0)
        val preferredSpace: Short = 5
        //seed "database"


        val next = getNextSortKey(placeHolderExistingSortKeys.maxOrNull()?:-1, placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(90.toString()), preferredSpace)


        assertEquals(preferredSpace, next)
    }


//    @Test
//    fun givenPopulationCountExactlyHalfOfSortKeySpaceWhenGetNextSortKeyThenException() {
//        val placeholderCapacitySlots = mutableMapOf<Long, Short>()
//        //TODO Do I actually need to care when the elements are clumped up like this?
//        val placeHolderExistingSortKeys = (16..45).toList().map { it.toShort() }.toTypedArray()
//        val seed = 401L
//        val random = Random(seed)
//        val placeHolderIds = (1_501L..1_530L).toList().shuffled(random).toTypedArray()
//
//        //seed "database"
//        for(i in 1..30) {
//            placeholderCapacitySlots[placeHolderIds[i-1]] = placeHolderExistingSortKeys[i-1]
//        }
//
//        Assert.assertThrows(RuntimeException::class.java) {
//            getNextSortKey(placeHolderExistingSortKeys.max(), placeholderCapacitySlots, MapMover(placeholderCapacitySlots), BigInteger(60.toString()))
//        }
//    }

}
