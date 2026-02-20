package com.ipanda.util

expect fun currentTimeMillis(): Long
expect fun writeCache(key: String, text: String)
expect fun readCache(key: String): String?
expect fun clearAllCache()
