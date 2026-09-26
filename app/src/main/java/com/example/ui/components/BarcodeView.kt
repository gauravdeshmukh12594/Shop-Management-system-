package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.util.BarcodeUtil
import java.io.File
import java.io.FileOutputStream

@Composable
fun DigitalBarcodeCard(
    code: String,
    productName: String = "",
    price: Double? = null,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    showActions: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val modules = remember(code) { BarcodeUtil.encodeCode128(code) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (productName.isNotEmpty()) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // The Barcode Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(height - 12.dp)) {
                    val totalModules = modules.size
                    val moduleWidth = size.width / totalModules
                    for (i in modules.indices) {
                        if (modules[i]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(i * moduleWidth, 0f),
                                size = Size(moduleWidth, size.height)
                            )
                        }
                    }
                }
            }

            // Monospace SKU Display
            Text(
                text = code,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 6.dp)
            )

            if (price != null) {
                Text(
                    text = "MRP: ₹${String.format("%.2f", price)} (Inc. of all taxes)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (showActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1E3A8A)
                        )
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy SKU",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy SKU")
                    }

                    FilledTonalButton(
                        onClick = {
                            shareBarcodeBitmap(context, code, productName, price)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEFF6FF),
                            contentColor = Color(0xFF1D4ED8)
                        )
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Barcode",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Barcode")
                    }
                }
            }
        }
    }
}

private fun shareBarcodeBitmap(
    context: Context,
    code: String,
    productName: String,
    price: Double?
) {
    try {
        val bitmap = BarcodeUtil.createBarcodeBitmap(code)
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "barcode_$code.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val caption = buildString {
            append("VastraPOS Digital Barcode\n")
            if (productName.isNotEmpty()) append("Product: $productName\n")
            append("SKU: $code\n")
            if (price != null) append("Price: ₹$price\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Digital Barcode"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
