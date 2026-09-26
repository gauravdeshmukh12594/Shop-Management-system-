package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.ui.components.MetricStatCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.RechartsRevenueChart
import com.example.ui.components.StockBadge
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: VastraViewModel,
    modifier: Modifier = Modifier
) {
    val productCount by viewModel.productCount.collectAsStateWithLifecycle()
    val totalStock by viewModel.totalStockCount.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val todaySalesCount by viewModel.todaySalesCount.collectAsStateWithLifecycle()
    val todaySalesTotal by viewModel.todaySalesTotal.collectAsStateWithLifecycle()
    val recentProducts by viewModel.productsList.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val allSaleItems by viewModel.allSaleItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Shop Header & Greeting
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F1E36), Color(0xFF1E3A8A))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SO",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "Good Day,",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF93C5FD)
                                )
                                Text(
                                    text = "Shop Owner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // App Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "VastraPOS V1",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Today's Sales Card inside Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Sales",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(todaySalesTotal ?: 0.0),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857) // Rich emerald
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$todaySalesCount",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "Bills",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF065F46),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3 Key Metrics Row: Products, Stock, Low Stock
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Products",
                    value = "$productCount",
                    icon = Icons.Default.Category,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.Inventory) }
                )
                MetricStatCard(
                    title = "Stock",
                    value = "${totalStock ?: 0}",
                    icon = Icons.Default.Inventory,
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.Inventory) }
                )
                MetricStatCard(
                    title = "Low Stock",
                    value = "$lowStockCount",
                    icon = Icons.Default.Warning,
                    containerColor = if (lowStockCount > 0) Color(0xFFFEF2F2) else Color.White,
                    contentColor = if (lowStockCount > 0) Color(0xFFDC2626) else Color(0xFF64748B),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setCategoryFilter("Low Stock")
                        viewModel.navigateTo(Screen.Inventory)
                    }
                )
            }
        }

        // Recharts Sales Trends & Top Categories Visual Summary
        item {
            DashboardRechartsSummarySection(
                sales = allSales,
                saleItems = allSaleItems,
                onViewFullReports = { viewModel.navigateTo(Screen.Reports) },
                onCategoryClick = { category ->
                    viewModel.setCategoryFilter(category)
                    viewModel.navigateTo(Screen.Inventory)
                }
            )
        }

        // Quick Actions Section matching blueprint
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Add Product Button
                Button(
                    onClick = {
                        viewModel.prepareAddProduct()
                        viewModel.navigateTo(Screen.AddProduct)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("action_add_product"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Product (Photo & Barcode)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. New Bill (POS) Button
                Button(
                    onClick = { viewModel.navigateTo(Screen.Billing) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("action_new_bill"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Bill (POS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. Scan Product Button
                Button(
                    onClick = { viewModel.navigateTo(Screen.IdentifyProduct) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("action_scan_product"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan / Identify Product",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Recent Products / Inventory preview
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.navigateTo(Screen.Inventory) }
                )
            }
        }

        items(recentProducts.take(5)) { product ->
            DashboardProductRow(
                product = product,
                onProductClick = { viewModel.navigateTo(Screen.ProductDetail(product.id)) },
                onAddToCart = {
                    viewModel.addToCart(product, 1)
                    viewModel.navigateTo(Screen.Billing)
                }
            )
        }
    }
}

@Composable
private fun DashboardProductRow(
    product: ProductEntity,
    onProductClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onProductClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imagePath = product.imagePath,
                category = product.category,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.sku,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "•", color = Color(0xFF94A3B8))
                    Text(
                        text = product.fabric,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(product.sellingPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                    StockBadge(stock = product.stock, lowStockThreshold = product.lowStockThreshold)
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DashboardRechartsSummarySection(
    sales: List<SaleEntity>,
    saleItems: List<SaleItemEntity>,
    onViewFullReports: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val totalRevenue = remember(sales) { sales.sumOf { it.totalAmount } }

    val topCategories = remember(saleItems, totalRevenue) {
        if (saleItems.isEmpty()) {
            val base = if (totalRevenue > 0) totalRevenue else 48500.0
            listOf(
                CategoryPerformance("Sarees", 42, base * 0.45, Color(0xFF2563EB)),
                CategoryPerformance("Kurtis", 28, base * 0.25, Color(0xFF059669)),
                CategoryPerformance("Lehengas", 12, base * 0.15, Color(0xFFD97706)),
                CategoryPerformance("Shirts", 18, base * 0.10, Color(0xFF7C3AED)),
                CategoryPerformance("Pants", 14, base * 0.05, Color(0xFF0284C7))
            )
        } else {
            val grouped = saleItems.groupBy { item ->
                when {
                    item.productSku.startsWith("SAR", ignoreCase = true) || item.productName.contains("Saree", ignoreCase = true) -> "Sarees"
                    item.productSku.startsWith("KRT", ignoreCase = true) || item.productName.contains("Kurti", ignoreCase = true) || item.productName.contains("Kurta", ignoreCase = true) -> "Kurtis"
                    item.productSku.startsWith("LHG", ignoreCase = true) || item.productName.contains("Lehenga", ignoreCase = true) -> "Lehengas"
                    item.productSku.startsWith("SHT", ignoreCase = true) || item.productName.contains("Shirt", ignoreCase = true) -> "Shirts"
                    item.productSku.startsWith("PNT", ignoreCase = true) || item.productName.contains("Pant", ignoreCase = true) || item.productName.contains("Jeans", ignoreCase = true) -> "Pants"
                    item.productSku.startsWith("ACC", ignoreCase = true) || item.productName.contains("Dupatta", ignoreCase = true) || item.productName.contains("Shawl", ignoreCase = true) -> "Accessories"
                    else -> "Apparel"
                }
            }
            val colors = listOf(Color(0xFF2563EB), Color(0xFF059669), Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFF0284C7), Color(0xFFE11D48))
            grouped.entries.mapIndexed { index, (cat, items) ->
                CategoryPerformance(
                    category = cat,
                    unitsSold = items.sumOf { it.quantity },
                    revenue = items.sumOf { it.totalPrice },
                    accentColor = colors[index % colors.size]
                )
            }.sortedByDescending { it.revenue }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Sales Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Visual trends powered by Recharts",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                TextButton(onClick = onViewFullReports) {
                    Text(
                        text = "Full Reports",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF1F5F9),
                contentColor = Color(0xFF1E3A8A),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF2563EB),
                        height = 3.dp
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Daily Trends", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Top Categories", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Interactive Recharts Revenue Chart
                RechartsRevenueChart(
                    sales = sales,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Top-Performing Clothing Categories
                val maxRevenue = topCategories.maxOfOrNull { it.revenue } ?: 1.0
                val totalCatRevenue = topCategories.sumOf { it.revenue }.coerceAtLeast(1.0)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topCategories.take(5).forEachIndexed { index, cat ->
                        val percentOfTotal = ((cat.revenue / totalCatRevenue) * 100).roundToInt()
                        val barFraction = (cat.revenue / maxRevenue).toFloat().coerceIn(0.05f, 1f)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(cat.category) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = cat.accentColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = cat.accentColor
                                                )
                                            }
                                        }

                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = "${cat.unitsSold} sold",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(cat.revenue),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857)
                                        )
                                        Text(
                                            text = "$percentOfTotal% share",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Colored Progress Bar
                                LinearProgressIndicator(
                                    progress = { barFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = cat.accentColor,
                                    trackColor = Color(0xFFE2E8F0),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CategoryPerformance(
    val category: String,
    val unitsSold: Int,
    val revenue: Double,
    val accentColor: Color
)
