package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.ui.components.DigitalQrView
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel
import com.example.util.PdfReceiptGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    saleId: Long,
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sale by remember { mutableStateOf<SaleEntity?>(null) }
    var items by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    LaunchedEffect(saleId) {
        val (s, itemList) = viewModel.getSaleDetails(saleId)
        sale = s
        items = itemList
    }

    fun handleSharePdf() {
        val currentSale = sale ?: return
        if (items.isEmpty()) return

        isGeneratingPdf = true
        coroutineScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReceiptGenerator.generatePdfReceipt(context, currentSale, items)
                }
                isGeneratingPdf = false
                PdfReceiptGenerator.sharePdfReceipt(context, pdfFile, currentSale)
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Could not generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleOpenPdf() {
        val currentSale = sale ?: return
        if (items.isEmpty()) return

        isGeneratingPdf = true
        coroutineScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReceiptGenerator.generatePdfReceipt(context, currentSale, items)
                }
                isGeneratingPdf = false
                PdfReceiptGenerator.openPdfReceipt(context, pdfFile)
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Could not open PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Invoice", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(
                        onClick = { handleSharePdf() },
                        enabled = !isGeneratingPdf && sale != null
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = Color(0xFFFBBF24))
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1E36))
            )
        }
    ) { paddingValues ->
        if (sale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else {
            val s = sale!!
            val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(s.createdAt))

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF1F5F9))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // The White Printable Bill Receipt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Brand Logo Icon & Name
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF1E3A8A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "VastraPOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Clothing & Saree Boutique",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Invoice # & Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("INVOICE NO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(
                                    s.invoiceNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DATE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(dateFormatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (s.customerName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("CUSTOMER: ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(
                                    "${s.customerName} ${if (s.customerPhone.isNotEmpty()) "(${s.customerPhone})" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Itemized List Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("ITEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("QTY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                            Text("PRICE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(item.productSku, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                Text("${item.quantity}", textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.weight(0.7f))
                                Text(formatCurrency(item.totalPrice), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.3f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtotal & Discounts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text(formatCurrency(s.subtotal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        if (s.discount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount", color = Color(0xFFDC2626), fontSize = 13.sp)
                                Text("- ${formatCurrency(s.discount)}", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                formatCurrency(s.totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Payment Status & Stock confirmation
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paid via ${s.paymentMethod} • Stock Automatically Reduced",
                                    color = Color(0xFF065F46),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Digital QR Code on invoice
                        DigitalQrView(
                            data = "INVOICE:${s.invoiceNumber};AMOUNT:${s.totalAmount};STORE:VastraPOS",
                            size = 120.dp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Thank You! Visit Again",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // -------------------------------------------------------------
                // ACTION BUTTONS: PDF RECEIPT GENERATION & SHARING
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Share PDF Receipt (WhatsApp, Email, Telegram, Messages)
                    Button(
                        onClick = { handleSharePdf() },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_share_pdf_receipt"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating PDF Receipt...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share PDF Receipt (Email / WhatsApp)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Preview / View PDF in native viewer
                        OutlinedButton(
                            onClick = { handleOpenPdf() },
                            enabled = !isGeneratingPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_view_pdf"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View PDF", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        // Share Quick Text Summary (WhatsApp direct)
                        Button(
                            onClick = { shareInvoiceText(context, s, items) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_share_text"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Text Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    // Done / Return to Dashboard
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done / Back to Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun shareInvoiceText(
    context: Context,
    sale: SaleEntity,
    items: List<SaleItemEntity>
) {
    val sb = StringBuilder()
    sb.append("👗 *VastraPOS Clothing Store*\n")
    sb.append("🧾 *Tax Invoice*: ${sale.invoiceNumber}\n")
    val dateStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))
    sb.append("📅 Date: $dateStr\n")
    if (sale.customerName.isNotEmpty()) {
        sb.append("👤 Customer: ${sale.customerName}\n")
    }
    sb.append("----------------------------\n")
    for (it in items) {
        sb.append("• ${it.productName} (x${it.quantity}) - ₹${String.format(Locale.getDefault(), "%.2f", it.totalPrice)}\n")
        sb.append("  SKU: ${it.productSku}\n")
    }
    sb.append("----------------------------\n")
    sb.append("Subtotal: ₹${String.format(Locale.getDefault(), "%.2f", sale.subtotal)}\n")
    if (sale.discount > 0) {
        sb.append("Discount: -₹${String.format(Locale.getDefault(), "%.2f", sale.discount)}\n")
    }
    sb.append("💳 *Total Paid*: ₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)} (${sale.paymentMethod})\n\n")
    sb.append("🙏 *Thank you for shopping with us! Visit again.*")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Invoice Summary"))
}
