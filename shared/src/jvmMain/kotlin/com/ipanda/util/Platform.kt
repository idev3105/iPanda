package com.ipanda.util

import java.io.File

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private val cacheDir: File by lazy {
    val tempDir = System.getProperty("java.io.tmpdir")
    File(tempDir, "ipanda_cache").apply { if (!exists()) mkdirs() }
}

actual fun writeCache(key: String, text: String) {
    File(cacheDir, key).writeText(text)
}

actual fun readCache(key: String): String? {
    val file = File(cacheDir, key)
    return if (file.exists()) file.readText() else null
}

actual fun clearAllCache() {
    cacheDir.listFiles()?.forEach { it.delete() }
}
