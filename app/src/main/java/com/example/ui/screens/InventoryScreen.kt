package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.DigitalBarcodeCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StockBadge
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.productsList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    var showBarcodeModalProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isGridView by remember { mutableStateOf(true) } // Default to Grid view for clothing catalog

    val categories = listOf(
        "All",
        "Shirts",
        "Pants",
        "Sarees",
        "Kurtis",
        "Lehengas",
        "Accessories",
        "Suits",
        "Dresses",
        "Low Stock"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clothing Catalog", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    // Toggle between Grid View and List View
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1E36))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.prepareAddProduct()
                    viewModel.navigateTo(Screen.AddProduct)
                },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(26.dp))
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search catalog by name, SKU, fabric, colour...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_inventory"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2563EB),
                        unfocusedIndicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (cat == "Low Stock") Color(0xFFFEE2E2) else Color(0xFF1E3A8A),
                            selectedLabelColor = if (cat == "Low Stock") Color(0xFFDC2626) else Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Products Count subtitle and View Mode indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${products.size} Clothing Items in Catalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (isGridView) "Grid Catalog" else "List Catalog",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search or add a new cloth item.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else if (isGridView) {
                // -------------------------------------------------------------
                // LAZY VERTICAL GRID DISPLAY (Coil Image Catalog)
                // -------------------------------------------------------------
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("catalog_lazy_vertical_grid"),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ClothingItemGridCard(
                            product = product,
                            onCardClick = { viewModel.navigateTo(Screen.ProductDetail(product.id)) },
                            onShowBarcode = { showBarcodeModalProduct = product },
                            onEditClick = {
                                viewModel.prepareEditProduct(product)
                                viewModel.navigateTo(Screen.AddProduct)
                            },
                            onAddToCart = {
                                viewModel.addToCart(product, 1)
                                viewModel.navigateTo(Screen.Billing)
                            }
                        )
                    }
                }
            } else {
                // List View fallback
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            onCardClick = { viewModel.navigateTo(Screen.ProductDetail(product.id)) },
                            onShowBarcode = { showBarcodeModalProduct = product },
                            onEditClick = {
                                viewModel.prepareEditProduct(product)
                                viewModel.navigateTo(Screen.AddProduct)
                            },
                            onAddToCart = {
                                viewModel.addToCart(product, 1)
                                viewModel.navigateTo(Screen.Billing)
                            }
                        )
                    }
                }
            }
        }
    }

    // Barcode Display Dialog
    if (showBarcodeModalProduct != null) {
        val p = showBarcodeModalProduct!!
        Dialog(onDismissRequest = { showBarcodeModalProduct = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Digital Barcode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DigitalBarcodeCard(
                        code = p.sku,
                        productName = p.name,
                        price = p.sellingPrice,
                        showActions = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showBarcodeModalProduct = null },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Grid Card for displaying clothing items in the LazyVerticalGrid.
 * Loads high quality images using Coil from database file paths.
 */
@Composable
fun ClothingItemGridCard(
    product: ProductEntity,
    onCardClick: () -> Unit,
    onShowBarcode: () -> Unit,
    onEditClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Photo Area (Loaded via Coil from product.imagePath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square crop for sarees & garments
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imagePath.isNullOrEmpty() && File(product.imagePath).exists()) {
                    AsyncImage(
                        model = File(product.imagePath),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback pattern when no photo exists
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Subtle gradient overlay at bottom of image for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x66000000))
                            )
                        )
                )

                // Category pill on Top-Left
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xD90F1E36),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Stock badge on Top-Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    StockBadge(
                        stock = product.stock,
                        lowStockThreshold = product.lowStockThreshold
                    )
                }
            }

            // 2. Garment Details Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Price
                Text(
                    text = formatCurrency(product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF047857)
                )

                // Product Name
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Fabric & Colour
                Text(
                    text = "${product.fabric} • ${product.colour}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // SKU / Barcode indicator
                Text(
                    text = product.sku,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3A8A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons Row (Barcode, Edit & Add to Cart)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShowBarcode,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Digital Barcode",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryProductCard(
    product: ProductEntity,
    onCardClick: () -> Unit,
    onShowBarcode: () -> Unit,
    onEditClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imagePath = product.imagePath,
                category = product.category,
                size = 68.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.category} • ${product.fabric} • ${product.colour}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(product.sellingPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669)
                    )
                    StockBadge(stock = product.stock, lowStockThreshold = product.lowStockThreshold)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShowBarcode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "View Barcode",
                        tint = Color(0xFF1E3A8A)
                    )
                }
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEFF6FF), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFECFDF5), CircleShape)
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Add to Cart",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
