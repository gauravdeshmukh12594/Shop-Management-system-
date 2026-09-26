package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.DigitalBarcodeCard
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel
import com.example.util.BarcodeUtil
import com.example.util.ImageStorageUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val name by viewModel.addName.collectAsStateWithLifecycle()
    val category by viewModel.addCategory.collectAsStateWithLifecycle()
    val brand by viewModel.addBrand.collectAsStateWithLifecycle()
    val colour by viewModel.addColour.collectAsStateWithLifecycle()
    val fabric by viewModel.addFabric.collectAsStateWithLifecycle()
    val purchasePrice by viewModel.addPurchasePrice.collectAsStateWithLifecycle()
    val sellingPrice by viewModel.addSellingPrice.collectAsStateWithLifecycle()
    val stock by viewModel.addStock.collectAsStateWithLifecycle()
    val notes by viewModel.addNotes.collectAsStateWithLifecycle()
    val imagePath by viewModel.addImagePath.collectAsStateWithLifecycle()
    val sku by viewModel.addSku.collectAsStateWithLifecycle()
    val errorMessage by viewModel.addErrorMessage.collectAsStateWithLifecycle()
    val editingProductId by viewModel.editingProductId.collectAsStateWithLifecycle()

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var showCameraXCapture by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null && tempPhotoFile!!.exists()) {
            viewModel.setAddProductImage(tempPhotoFile!!.absolutePath)
        }
    }

    // Gallery photo picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageStorageUtil.saveUriToInternalStorage(context, uri)
            viewModel.setAddProductImage(savedPath)
        }
    }

    val categories = listOf("Saree", "Kurti", "Shirt", "Pant", "Lehenga", "Suit", "Dress", "Fabric/Cloth", "Dupatta", "Accessories")
    val fabrics = listOf("Pure Silk", "Banarasi Silk", "Cotton", "Cotton Silk", "Georgette", "Chiffon", "Rayon", "Linen", "Velvet", "Organza")
    val colors = listOf("Crimson Red", "Royal Blue", "Emerald Green", "Pastel Yellow", "Maroon", "Golden", "Rose Pink", "Crisp White", "Charcoal Black", "Purple")

    var categoryExpanded by remember { mutableStateOf(false) }
    var fabricExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingProductId != null) "Edit Product Details" else "Add Product (Cloth & Barcode)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1E36)
                )
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
            // 1. Photo Capture Section
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1. Click Saree or Cloth Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Take a clear photo of the garment/pattern for easy identification",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!imagePath.isNullOrEmpty() && File(imagePath!!).exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = File(imagePath!!),
                                contentDescription = "Captured Cloth Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.setAddProductImage(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Photo", tint = Color.White)
                            }
                        }
                    } else {
                        // Empty photo placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No Photo Selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showCameraXCapture = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_take_photo"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_choose_gallery"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Gallery")
                        }
                    }
                }
            }

            // 2. Product Details Card
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Product Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Product Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.addName.value = it },
                        label = { Text("Product Name *") },
                        placeholder = { Text("e.g. Banarasi Silk Saree, Chanderi Saree") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.onCategoryChanged(cat)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Fabric & Brand Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = fabricExpanded,
                            onExpandedChange = { fabricExpanded = !fabricExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = fabric,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Fabric") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fabricExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = fabricExpanded,
                                onDismissRequest = { fabricExpanded = false }
                            ) {
                                fabrics.forEach { fab ->
                                    DropdownMenuItem(
                                        text = { Text(fab) },
                                        onClick = {
                                            viewModel.addFabric.value = fab
                                            fabricExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = brand,
                            onValueChange = { viewModel.addBrand.value = it },
                            label = { Text("Brand / Weaver") },
                            placeholder = { Text("e.g. Shree") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Colour selection
                    OutlinedTextField(
                        value = colour,
                        onValueChange = { viewModel.addColour.value = it },
                        label = { Text("Colour / Shade") },
                        placeholder = { Text("e.g. Crimson Red, Golden Mustard") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Quick colour suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        colors.take(4).forEach { col ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (colour == col) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { viewModel.addColour.value = col }
                            ) {
                                Text(
                                    text = col.take(7),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Pricing Row (Buy Price & Sell Price)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { viewModel.addPurchasePrice.value = it },
                            label = { Text("Buy Price (₹)") },
                            placeholder = { Text("2800") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_buy_price"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { viewModel.addSellingPrice.value = it },
                            label = { Text("Sell Price / MRP * (₹)") },
                            placeholder = { Text("4499") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sell_price"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Initial Stock
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { viewModel.addStock.value = it },
                        label = { Text("Initial Stock (Quantity)") },
                        placeholder = { Text("5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stock"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // 3. Digital Barcode Generation Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF1E3A8A))
                            Text(
                                text = "3. Digital Barcode (Code 128)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextSeq = System.currentTimeMillis() % 900000 + 100000
                                viewModel.addSku.value = BarcodeUtil.generateSku(category, nextSeq)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Barcode", tint = Color(0xFF2563EB))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Text(
                                text = "100% Digital - No physical barcode stickers needed on clothes! Stored in app and identified via saree photo during billing.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }

                    // Live digital barcode preview
                    DigitalBarcodeCard(
                        code = sku.ifBlank { "SAR-000125" },
                        productName = name.ifBlank { "Preview Item" },
                        price = sellingPrice.toDoubleOrNull(),
                        showActions = false
                    )
                }
            }

            // Error display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFDC2626))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFB91C1C),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Save Product Button
            Button(
                onClick = {
                    viewModel.saveProduct { savedId ->
                        if (editingProductId != null) {
                            Toast.makeText(context, "Product details updated successfully!", Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo(Screen.ProductDetail(savedId))
                        } else {
                            Toast.makeText(context, "Product & Barcode Saved Successfully!", Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo(Screen.ProductSavedSuccess(savedId))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_save_product"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editingProductId != null) Color(0xFF0F766E) else Color(0xFF2563EB)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingProductId != null) "Save & Update Product Info" else "Save Product & Generate Barcode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showCameraXCapture) {
            CameraCaptureScreen(
                onPhotoCaptured = { photoFile ->
                    viewModel.setAddProductImage(photoFile.absolutePath)
                    showCameraXCapture = false
                },
                onDismiss = { showCameraXCapture = false },
                promptTitle = "Photograph Clothing Item",
                promptSubtitle = "Position saree or fabric within the frame for clean catalog photos"
            )
        }
    }
}
