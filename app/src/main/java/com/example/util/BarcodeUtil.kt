package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Utility for SKU generation, Code 128 Barcode module encoding, and Barcode Bitmaps.
 */
object BarcodeUtil {

    /**
     * Generates a standardized, professional SKU based on clothing category and unique ID.
     */
    fun generateSku(category: String, sequenceNumber: Long): String {
        val prefix = when (category.trim().lowercase()) {
            "saree" -> "SAR"
            "kurti" -> "KRT"
            "shirt" -> "SHR"
            "pant", "pants", "trouser" -> "PNT"
            "lehenga" -> "LHG"
            "suit", "salwar" -> "SUT"
            "dress" -> "DRS"
            "dupatta" -> "DPT"
            "t-shirt", "tshirt" -> "TSH"
            else -> "VAS"
        }
        val paddedSeq = sequenceNumber.toString().padStart(6, '0')
        return "$prefix-$paddedSeq"
    }

    /**
     * Code 128 (Character Set B) patterns.
     * Each pattern is 11 bits (1 = black bar, 0 = white space), except stop pattern (13 bits).
     */
    private val CODE_128_PATTERNS = arrayOf(
        "11011001100", "11001101100", "11001100110", "10010011000", "10010001100", // 0-4
        "10001001100", "10011001000", "10011000100", "10001100100", "11001001000", // 5-9
        "11001000100", "11000100100", "10110011100", "10011011100", "10011001110", // 10-14
        "10111001100", "10011101100", "10011100110", "11001110010", "11001011100", // 15-19
        "11001001110", "11011100100", "11001110100", "11101101110", "11101001100", // 20-24
        "11100101100", "11100100110", "11101100100", "11100110100", "11100110010", // 25-29
        "11011011000", "11011000110", "11000110110", "10100011000", "10001011000", // 30-34
        "10001000110", "10110001000", "10001101000", "10001100010", "11010001000", // 35-39
        "11000101000", "11000100010", "10110111000", "10110001110", "10001101110", // 40-44
        "10111011000", "10111000110", "10001110110", "11101110110", "11010001110", // 45-49
        "11000101110", "11011101000", "11011100010", "11011101110", "11101011000", // 50-54
        "11101000110", "11100010110", "11101101000", "11101100010", "11100011010", // 55-59
        "11101111010", "11001000010", "11110001010", "10100110000", "10100001100", // 60-64
        "10010110000", "10010000110", "10000101100", "10000100110", "10110010000", // 65-69
        "10110000100", "10011010000", "10011000010", "10000110100", "10000110010", // 70-74
        "11000010010", "11001010000", "11110111010", "11000010100", "10001111010", // 75-79
        "10100111100", "10010111100", "10010011110", "10111100100", "10011110100", // 80-84
        "10011110010", "11110100100", "11110010100", "11110010010", "11011011110", // 85-89
        "11011110110", "11110110110", "10101111000", "10100011110", "10001011110", // 90-94
        "10111101000", "10111100010", "11110101000", "11110100010", "10111011110", // 95-99
        "10111101110", "11101011110", "11110101110", "11010000100", "11010010000", // 100-104 (104 is Start B)
        "11010011100", "1100011101011" // 105 (Start C), 106 (Stop)
    )

    private const val START_CODE_B = 104
    private const val STOP_CODE = 106

    /**
     * Encodes a string into a boolean array of modules (true = black bar, false = white space)
     * using Code 128 Subset B.
     */
    fun encodeCode128(text: String): BooleanArray {
        val cleanText = text.ifEmpty { "000000" }
        val values = mutableListOf<Int>()
        values.add(START_CODE_B)

        var checksum = START_CODE_B
        for (i in cleanText.indices) {
            val charCode = cleanText[i].code
            val value = charCode - 32 // Code 128 Subset B maps ASCII 32..127 to values 0..95
            val safeValue = if (value in 0..95) value else 0
            values.add(safeValue)
            checksum += (i + 1) * safeValue
        }
        val checkValue = checksum % 103
        values.add(checkValue)
        values.add(STOP_CODE)

        // Build binary string
        val sb = StringBuilder()
        // 10 quiet modules at start
        repeat(10) { sb.append('0') }

        for (v in values) {
            val pattern = if (v in CODE_128_PATTERNS.indices) CODE_128_PATTERNS[v] else CODE_128_PATTERNS[0]
            sb.append(pattern)
        }

        // 10 quiet modules at end
        repeat(10) { sb.append('0') }

        val result = BooleanArray(sb.length)
        for (i in sb.indices) {
            result[i] = sb[i] == '1'
        }
        return result
    }

    /**
     * Renders a barcode to an Android Bitmap with text label underneath, suitable for sharing or printing.
     */
    fun createBarcodeBitmap(
        code: String,
        width: Int = 600,
        height: Int = 220,
        showText: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val modules = encodeCode128(code)
        val totalModules = modules.size
        val moduleWidth = width.toFloat() / totalModules

        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val textHeight = if (showText) 45f else 0f
        val barHeight = height - textHeight - 20f

        for (i in modules.indices) {
            if (modules[i]) {
                val left = i * moduleWidth
                val right = (i + 1) * moduleWidth
                canvas.drawRect(left, 15f, right, 15f + barHeight, barPaint)
            }
        }

        if (showText) {
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText(code, width / 2f, height - 12f, textPaint)
        }

        return bitmap
    }
}
