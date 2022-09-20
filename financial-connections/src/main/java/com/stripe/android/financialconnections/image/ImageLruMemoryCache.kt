package com.stripe.android.financialconnections.image

import android.graphics.Bitmap
import android.util.LruCache

internal class ImageLruMemoryCache {

    @Suppress("MagicNumber")
    private val lruCache = object : LruCache<String, Bitmap>(
        // Use 1/8th of the available memory for this memory cache.
        (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        if (lruCache.get(key.toKey()) == null) {
            lruCache.put(key.toKey(), bitmap)
            // debug("Image cached: $key")
        }
    }

    fun getBitmap(key: String): Bitmap? {
        return lruCache.get(key.toKey())
    }

    fun clear() {
        lruCache.evictAll()
    }

    private fun String.toKey() = hashCode().toString()
}
