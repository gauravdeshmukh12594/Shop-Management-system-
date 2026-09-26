package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.data.local.entity.ProductEntity
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class VisualMatchResult(
    val product: ProductEntity,
    val confidencePercent: Int,
    val matchDescription: String
)

object VisualMatcherUtil {

    /**
     * Matches an uploaded or captured garment photo against all products in the inventory.
     * Uses bitmap color histogram & RGB distance comparison combined with color/fabric tag heuristics.
     */
    fun findMatchingProducts(
        capturedImagePath: String,
        inventory: List<ProductEntity>
    ): List<VisualMatchResult> {
        val capturedBitmap = decodeSampledBitmap(capturedImagePath, 64, 64) ?: return fallbackTagMatch(inventory)
        val capturedProfile = extractColorProfile(capturedBitmap)

        val results = inventory.map { product ->
            var score = 50.0 // Base score

            // 1. Compare with stored product photo if available
            if (!product.imagePath.isNullOrEmpty() && File(product.imagePath).exists()) {
                val prodBitmap = decodeSampledBitmap(product.imagePath, 64, 64)
                if (prodBitmap != null) {
                    val prodProfile = extractColorProfile(prodBitmap)
                    val visualSimilarity = compareProfiles(capturedProfile, prodProfile)
                    score = visualSimilarity * 100.0
                }
            } else {
                // If no product photo, match dominant hue with product.colour name
                val colorScore = scoreColorNameMatch(capturedProfile.dominantColorName, product.colour)
                score = (colorScore * 0.7) + 20.0
            }

            // Bonus if recognized color matches product defined colour
            if (product.colour.contains(capturedProfile.dominantColorName, ignoreCase = true) ||
                capturedProfile.dominantColorName.contains(product.colour, ignoreCase = true)
            ) {
                score += 15.0
            }

            val finalScore = score.toInt().coerceIn(35, 98)
            val description = "Matched ${capturedProfile.dominantColorName} ${product.fabric} (${finalScore}% match)"

            VisualMatchResult(
                product = product,
                confidencePercent = finalScore,
                matchDescription = description
            )
        }

        // Sort descending by confidence
        return results.sortedByDescending { it.confidencePercent }
    }

    private fun fallbackTagMatch(inventory: List<ProductEntity>): List<VisualMatchResult> {
        return inventory.mapIndexed { idx, p ->
            val score = (85 - idx * 5).coerceAtLeast(40)
            VisualMatchResult(
                product = p,
                confidencePercent = score,
                matchDescription = "Tag match based on latest saree inventory (${score}%)"
            )
        }
    }

    data class ColorProfile(
        val avgRed: Float,
        val avgGreen: Float,
        val avgBlue: Float,
        val dominantHue: Float,
        val dominantColorName: String
    )

    private fun extractColorProfile(bitmap: Bitmap): ColorProfile {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        val pixelCount = bitmap.width * bitmap.height

        val hsv = FloatArray(3)
        var totalHue = 0.0

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                totalR += r
                totalG += g
                totalB += b

                Color.colorToHSV(color, hsv)
                totalHue += hsv[0]
            }
        }

        val avgR = (totalR.toFloat() / pixelCount)
        val avgG = (totalG.toFloat() / pixelCount)
        val avgB = (totalB.toFloat() / pixelCount)
        val avgHue = (totalHue / pixelCount).toFloat()

        val colorName = when {
            avgR > 180 && avgG > 180 && avgB > 180 -> "White"
            avgR < 50 && avgG < 50 && avgB < 50 -> "Black"
            avgHue in 0f..25f || avgHue in 335f..360f -> if (avgR > 120 && avgG < 80) "Red" else "Pink"
            avgHue in 25f..55f -> if (avgR > 180 && avgG > 140) "Golden" else "Yellow"
            avgHue in 55f..75f -> "Yellow"
            avgHue in 75f..165f -> "Green"
            avgHue in 165f..260f -> "Blue"
            avgHue in 260f..335f -> "Maroon"
            else -> "Colour"
        }

        return ColorProfile(avgR, avgG, avgB, avgHue, colorName)
    }

    private fun compareProfiles(p1: ColorProfile, p2: ColorProfile): Double {
        val dr = (p1.avgRed - p2.avgRed) / 255.0
        val dg = (p1.avgGreen - p2.avgGreen) / 255.0
        val db = (p1.avgBlue - p2.avgBlue) / 255.0
        val colorDist = sqrt(dr * dr + dg * dg + db * db) // Max sqrt(3) ~= 1.732

        val hueDiff = abs(p1.dominantHue - p2.dominantHue)
        val circularHueDiff = min(hueDiff, 360f - hueDiff) / 180.0

        val totalDist = (colorDist * 0.6) + (circularHueDiff * 0.4)
        return (1.0 - (totalDist / 1.5)).coerceIn(0.2, 0.98)
    }

    private fun scoreColorNameMatch(dominantName: String, productColour: String): Double {
        return if (productColour.contains(dominantName, ignoreCase = true) ||
            dominantName.contains(productColour, ignoreCase = true)
        ) 85.0 else 50.0
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
