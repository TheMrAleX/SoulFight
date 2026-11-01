package com.fightforge.engine

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Stable ordering and timestamps preserve LAN cache keys across connections. */
object AssetArchive {
    fun encode(files: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, bytes) in files.toSortedMap()) {
                require(!name.startsWith("/") && name.split('/').none { it == ".." })
                zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        require(output.size() <= 8 * 1024 * 1024) { "Paquete demasiado grande (máximo 8 MB)" }
        return output.toByteArray()
    }
}
