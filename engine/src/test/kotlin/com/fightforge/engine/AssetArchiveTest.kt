package com.fightforge.engine

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class AssetArchiveTest {
    @Test fun cacheKeyIsIndependentOfMapOrderAndTime() {
        val one = AssetArchive.encode(linkedMapOf("character.json" to "fighter".toByteArray(), "assets/0.png" to byteArrayOf(1, 2, 3)))
        val two = AssetArchive.encode(linkedMapOf("assets/0.png" to byteArrayOf(1, 2, 3), "character.json" to "fighter".toByteArray()))
        assertArrayEquals(one, two)
        ZipInputStream(ByteArrayInputStream(one)).use { zip ->
            assertEquals("assets/0.png", zip.nextEntry.name)
            assertArrayEquals(byteArrayOf(1, 2, 3), zip.readBytes())
            assertEquals("character.json", zip.nextEntry.name)
            assertEquals("fighter", zip.readBytes().toString(Charsets.UTF_8))
        }
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsafeNames() { AssetArchive.encode(mapOf("../escape" to byteArrayOf(1))) }
}
