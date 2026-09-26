package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DigitalQrView
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val customerPhone by viewModel.customerPhone.collectAsStateWithLifecycle()
    val amountTendered by viewModel.amountTendered.collectAsStateWithLifecycle()

    val tenderedVal = amountTendered.toDoubleOrNull() ?: 0.0
    val changeDue = (tenderedVal - total).coerceAtLeast(0.0)

    val methods = listOf(
        PaymentOption("Cash", Icons.Default.Money, "Cash at counter"),
        PaymentOption("UPI", Icons.Default.QrCode, "GPay, PhonePe, Paytm QR"),
        PaymentOption("Card", Icons.Default.CreditCard, "Debit / Credit Card POS"),
        PaymentOption("Split Payment", Icons.Default.Payments, "Cash + Online Split")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment & Checkout", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1E36))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big Total Amount Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Payable Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF93C5FD)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatCurrency(total),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981) // High contrast emerald
                    )
                }
            }

            // Payment Methods Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    methods.forEach { opt ->
                        val isSelected = paymentMethod == opt.name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.paymentMethod.value = opt.name },
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = opt.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.paymentMethod.value = opt.name },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                                )
                            }
                        }
                    }

                    // If UPI selected: show live dynamic UPI QR code!
                    if (paymentMethod == "UPI") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Scan with any UPI App",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                val upiPayload = "upi://pay?pa=vastrapos@upi&pn=VastraPOS+Clothing&am=$total&cu=INR"
                                DigitalQrView(
                                    data = upiPayload,
                                    size = 170.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Amount: ${formatCurrency(total)} • GPay, PhonePe, Paytm",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // If Cash selected: show cash tender and change calculator
                    if (paymentMethod == "Cash") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = amountTendered,
                                onValueChange = { viewModel.amountTendered.value = it },
                                label = { Text("Cash Received (₹)") },
                                placeholder = { Text(total.toInt().toString()) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            if (tenderedVal > 0) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Change to Return:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    Text(
                                        text = formatCurrency(changeDue),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (changeDue >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Info (Optional) Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customer Details (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.customerName.value = it },
                        label = { Text("Customer Name") },
                        placeholder = { Text("e.g. Priya Sharma") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { viewModel.customerPhone.value = it },
                        label = { Text("WhatsApp / Mobile Number") },
                        placeholder = { Text("e.g. +91 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            // Complete Sale Button
            Button(
                onClick = {
                    viewModel.completeSale { saleId ->
                        Toast.makeText(context, "Sale Completed! Stock Reduced.", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(Screen.Invoice(saleId))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_complete_sale"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Complete Sale (${formatCurrency(total)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class PaymentOption(
    val name: String,
    val icon: ImageVector,
    val subtitle: String
)
