package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StockBadge
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel
import com.example.util.ImageStorageUtil
import com.example.util.VisualMatchResult
import com.example.util.VisualMatcherUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val discount by viewModel.discountAmount.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val allProducts by viewModel.productsList.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var discountInput by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var showReviewBillSheet by remember { mutableStateOf(false) }
    var isBreakdownExpanded by remember { mutableStateOf(false) }

    // Map of productId to quantity in cart for instant selection checks
    val cartQuantityMap by remember(cartItems) {
        derivedStateOf {
            cartItems.associate { it.product.id to it.quantity }
        }
    }

    val totalItemCount by remember(cartItems) {
        derivedStateOf {
            cartItems.sumOf { it.quantity }
        }
    }

    // Filtered catalog list to display in the billing screen
    val filteredCatalog by remember(searchQuery, selectedCategory, allProducts) {
        derivedStateOf {
            allProducts.filter { product ->
                val matchesCategory = selectedCategory == "All" ||
                        product.category.equals(selectedCategory, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        product.name.contains(searchQuery, ignoreCase = true) ||
                        product.sku.contains(searchQuery, ignoreCase = true) ||
                        product.fabric.contains(searchQuery, ignoreCase = true) ||
                        product.colour.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }
    }

    // Photo matching state for instant billing
    var showCameraXCapture by remember { mutableStateOf(false) }
    var currentMatchResults by remember { mutableStateOf<List<VisualMatchResult>?>(null) }
    var capturedPhotoForMatch by remember { mutableStateOf<String?>(null) }

    fun processCapturedPhoto(path: String) {
        capturedPhotoForMatch = path
        val matches = VisualMatcherUtil.findMatchingProducts(path, allProducts)
        currentMatchResults = matches
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageStorageUtil.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                processCapturedPhoto(savedPath)
            }
        }
    }

    val categories = listOf("All", "Saree", "Kurti", "Shirt", "Pant", "Lehenga", "Suit")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Sale & Billing", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text(
                            text = if (totalItemCount == 0) "Select items from catalog below" else "$totalItemCount item(s) selected",
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Quick button to snap saree photo
                    IconButton(onClick = { showCameraXCapture = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera Photo Match", tint = Color(0xFFFBBF24))
                    }

                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Bill", tint = Color(0xFFF87171))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1E36))
            )
        },
        bottomBar = {
            // Running Total Sticky Footer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("running_total_footer"),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Expandable discount / subtotal details
                    AnimatedVisibility(
                        visible = isBreakdownExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal (${totalItemCount} garments)", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                                Text(formatCurrency(subtotal), fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Discount (₹)", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = {
                                        discountInput = it
                                        val d = it.toDoubleOrNull() ?: 0.0
                                        viewModel.setDiscount(d)
                                    },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(44.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    // Main Running Total Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.clickable { isBreakdownExpanded = !isBreakdownExpanded }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Running Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Icon(
                                    imageVector = if (isBreakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = formatCurrency(total),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                modifier = Modifier.testTag("running_total_text")
                            )
                            if (totalItemCount > 0) {
                                Text(
                                    text = "$totalItemCount item(s) selected",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (cartItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showReviewBillSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bill ($totalItemCount)", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = {
                                    if (cartItems.isEmpty()) {
                                        Toast.makeText(context, "Please select catalog items first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.navigateTo(Screen.Payment)
                                    }
                                },
                                enabled = cartItems.isNotEmpty(),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_proceed_payment"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Pay", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search catalog to add to bill...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_catalog"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2563EB),
                        unfocusedIndicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Catalog Header & Quick Photo Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catalog Items (${filteredCatalog.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.clickable { showCameraXCapture = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                        Text("Photo Match", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Catalog List Display
            if (filteredCatalog.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching catalog items", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Text("Try a different search term or category", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("catalog_billing_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCatalog, key = { it.id }) { product ->
                        val qtyInCart = cartQuantityMap[product.id] ?: 0
                        BillingCatalogItemRow(
                            product = product,
                            selectedQuantity = qtyInCart,
                            onSelect = {
                                viewModel.addToCart(product, 1)
                            },
                            onIncrement = {
                                viewModel.updateCartQuantity(product.id, qtyInCart + 1)
                            },
                            onDecrement = {
                                viewModel.updateCartQuantity(product.id, qtyInCart - 1)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet to Review and Edit the Current Active Bill
    if (showReviewBillSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReviewBillSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Bill Items ($totalItemCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Clear All", color = Color(0xFFDC2626))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductThumbnail(
                                    imagePath = item.product.imagePath,
                                    category = item.product.category,
                                    size = 50.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("MRP: ${formatCurrency(item.product.sellingPrice)}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        formatCurrency(item.subtotal),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF047857),
                                        fontSize = 13.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp).background(Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp).background(Color(0xFFDBEAFE), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        showReviewBillSheet = false
                        viewModel.navigateTo(Screen.Payment)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Proceed to Checkout (${formatCurrency(total)})", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Cloth Recognized Dialog after CameraX or Gallery Photo Upload
    if (currentMatchResults != null && currentMatchResults!!.isNotEmpty()) {
        val topMatch = currentMatchResults!!.first()
        val p = topMatch.product

        AlertDialog(
            onDismissRequest = { currentMatchResults = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cloth Identified!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = topMatch.matchDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductThumbnail(
                            imagePath = p.imagePath ?: capturedPhotoForMatch,
                            category = p.category,
                            size = 64.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${p.category} • ${p.fabric} • ${p.colour}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "SKU: ${p.sku}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1E3A8A),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatCurrency(p.sellingPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                                StockBadge(stock = p.stock)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addToCart(p, 1)
                        Toast.makeText(context, "Added ${p.name} to bill!", Toast.LENGTH_SHORT).show()
                        currentMatchResults = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Add to Bill (${formatCurrency(p.sellingPrice)})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { currentMatchResults = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCameraXCapture) {
        CameraCaptureScreen(
            onPhotoCaptured = { photoFile ->
                processCapturedPhoto(photoFile.absolutePath)
                showCameraXCapture = false
            },
            onDismiss = { showCameraXCapture = false },
            promptTitle = "Photograph Saree to Bill",
            promptSubtitle = "Position saree pattern in the frame to fetch price & barcode"
        )
    }
}

/**
 * Single item row in the Billing Catalog List.
 * Highlights whether the item is selected in the current bill,
 * and provides one-tap quantity steppers that calculate the running total in real time.
 */
@Composable
private fun BillingCatalogItemRow(
    product: ProductEntity,
    selectedQuantity: Int,
    onSelect: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isSelected = selectedQuantity > 0
    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
        label = "cardBorderColor"
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF0F7FF) else Color.White,
        label = "cardBgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .clickable {
                if (!isSelected) onSelect()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Garment Image via Coil ProductThumbnail
            ProductThumbnail(
                imagePath = product.imagePath,
                category = product.category,
                size = 58.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Garment Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2563EB)
                        ) {
                            Text(
                                text = "Selected",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${product.category} • ${product.fabric} • ${product.colour}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatCurrency(product.sellingPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                    Text("•", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                    Text(
                        text = "Stock: ${product.stock}",
                        fontSize = 11.sp,
                        color = if (product.stock <= product.lowStockThreshold) Color(0xFFDC2626) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Select or Quantity Stepper
            if (!isSelected) {
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Stepper: [-] [qty] [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEFF6FF), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = "$selectedQuantity",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF2563EB), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
