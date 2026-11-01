package com.davidgrath.expensetrackerbackend.utils

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.UUID

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
}