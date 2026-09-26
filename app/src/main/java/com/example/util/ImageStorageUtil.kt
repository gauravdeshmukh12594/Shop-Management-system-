package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageUtil {

    fun createTempPictureUri(context: Context): Pair<Uri, File> {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "prod_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(uri, file)
    }

    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            val destFile = File(imagesDir, "prod_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "prod"): String? {
        return try {
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            val destFile = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a rich, decorative patterned swatch bitmap for clothes when default sample products are seeded.
     */
    fun createSampleFabricSwatch(
        context: Context,
        title: String,
        category: String,
        colorHex1: Int,
        colorHex2: Int
    ): String? {
        val width = 400
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient base
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colorHex1, colorHex2, Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative Indian textile zari / embroidery patterns
        val goldPaint = Paint().apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        // Border zari frame
        canvas.drawRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), goldPaint)
        canvas.drawRect(28f, 28f, (width - 28).toFloat(), (height - 28).toFloat(), goldPaint)

        // Motif dots / floral diamonds
        val dotPaint = Paint().apply {
            color = 0x66FFFFFF
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        for (x in 60..340 step 45) {
            for (y in 60..340 step 45) {
                canvas.drawCircle(x.toFloat(), y.toFloat(), 5f, dotPaint)
            }
        }

        // Subtle label overlay
        val textBgPaint = Paint().apply {
            color = 0xAA000000.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 320f, width.toFloat(), 400f, textBgPaint)

        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title.take(22), width / 2f, 355f, textPaint)

        val catPaint = Paint().apply {
            color = 0xFFFBBF24.toInt()
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(category.uppercase(), width / 2f, 385f, catPaint)

        return saveBitmapToInternalStorage(context, bitmap, "sample_${category.lowercase()}")
    }
}
