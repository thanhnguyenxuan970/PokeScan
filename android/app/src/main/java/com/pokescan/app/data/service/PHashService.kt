package com.snapdex.app.data.service

import android.content.Context
import android.graphics.Bitmap
import com.snapdex.app.domain.model.SetEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sqrt

@Singleton
class PHashService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val setHashes: Map<String, Long> by lazy { loadHashes() }

    private fun loadHashes(): Map<String, Long> {
        return try {
            val json = context.assets.open("set_phashes.json").bufferedReader().readText()
            val obj = JSONObject(json)
            buildMap {
                obj.keys().forEach { key ->
                    val hex = obj.getString(key)
                    put(key, java.lang.Long.parseUnsignedLong(hex, 16))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun computeHash(bitmap: Bitmap): Long? {
        if (bitmap.isRecycled) return null
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = FloatArray(32 * 32)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val px = scaled.getPixel(x, y)
                val r = (px shr 16 and 0xFF) / 255f
                val g = (px shr 8 and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                // BT.601 luminance
                pixels[y * 32 + x] = 0.299f * r + 0.587f * g + 0.114f * b
            }
        }
        scaled.recycle()

        // 8x8 DCT over 32x32 block — take top-left 8x8 frequencies
        val dct = Array(8) { FloatArray(8) }
        val piOver64 = Math.PI / 64.0
        val inv4 = 0.25
        val inv2 = 1.0 / sqrt(2.0)
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                var sum = 0.0
                for (x in 0 until 32) {
                    for (y in 0 until 32) {
                        sum += pixels[y * 32 + x] *
                            cos((2 * x + 1) * u * piOver64) *
                            cos((2 * y + 1) * v * piOver64)
                    }
                }
                val cu = if (u == 0) inv2 else 1.0
                val cv = if (v == 0) inv2 else 1.0
                dct[u][v] = (sum * cu * cv * inv4).toFloat()
            }
        }

        // Mean of 63 AC coefficients (exclude DC [0,0])
        var acSum = 0f
        for (u in 0 until 8) for (v in 0 until 8) if (u != 0 || v != 0) acSum += dct[u][v]
        val mean = acSum / 63f

        // Build 64-bit hash
        var hash = 0L
        var bit = 0
        for (u in 0 until 8) {
            for (v in 0 until 8) {
                if (dct[u][v] > mean) hash = hash or (1L shl bit)
                bit++
            }
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun findBestMatch(bitmap: Bitmap, candidates: List<SetEntry>): String? {
        if (setHashes.isEmpty()) return null
        val queryHash = computeHash(bitmap) ?: return null
        var bestCode: String? = null
        var bestDist = Int.MAX_VALUE
        for (candidate in candidates) {
            val candidateHash = setHashes[candidate.setCode] ?: continue
            val dist = hammingDistance(queryHash, candidateHash)
            if (dist < bestDist) {
                bestDist = dist
                bestCode = candidate.setCode
            }
        }
        return if (bestDist <= 10) bestCode else null
    }
}
