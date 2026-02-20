package com.ipanda.util

import java.io.File

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * On Android, this must be initialized in the Application or Activity before use.
 */
var androidCacheDir: File? = null
var androidContext: android.content.Context? = null

actual fun writeCache(key: String, text: String) {
    val dir = androidCacheDir ?: return
    if (!dir.exists()) dir.mkdirs()
    File(dir, key).writeText(text)
}

actual fun readCache(key: String): String? {
    val dir = androidCacheDir ?: return null
    val file = File(dir, key)
    return if (file.exists()) file.readText() else null
}

actual fun clearAllCache() {
    val dir = androidCacheDir ?: return
    dir.listFiles()?.forEach { it.delete() }
}
