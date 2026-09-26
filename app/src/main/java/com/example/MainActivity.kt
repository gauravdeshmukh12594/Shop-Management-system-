package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AddProductScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IdentifyProductScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoiceScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductSavedScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.VastraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VastraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VastraAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VastraAppContent(viewModel: VastraViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val totalCartCount = cartItems.sumOf { it.quantity }

    // Intercept back button to navigate screen stack or return to dashboard
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        viewModel.navigateBack()
    }

    val isTopLevelScreen = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.Inventory ||
            currentScreen is Screen.Billing ||
            currentScreen is Screen.Reports

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevelScreen) {
                VastraBottomNavigationBar(
                    currentScreen = currentScreen,
                    cartCount = totalCartCount,
                    onNavigate = { target ->
                        viewModel.navigateTo(target)
                    }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                is Screen.Inventory -> InventoryScreen(viewModel = viewModel)
                is Screen.AddProduct -> AddProductScreen(viewModel = viewModel)
                is Screen.ProductSavedSuccess -> ProductSavedScreen(productId = screen.productId, viewModel = viewModel)
                is Screen.ProductDetail -> ProductDetailScreen(productId = screen.productId, viewModel = viewModel)
                is Screen.IdentifyProduct -> IdentifyProductScreen(viewModel = viewModel)
                is Screen.Billing -> BillingScreen(viewModel = viewModel)
                is Screen.Payment -> PaymentScreen(viewModel = viewModel)
                is Screen.Invoice -> InvoiceScreen(saleId = screen.saleId, viewModel = viewModel)
                is Screen.Reports -> ReportsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val title: String,
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun VastraBottomNavigationBar(
    currentScreen: Screen,
    cartCount: Int,
    onNavigate: (Screen) -> Unit
) {
    val items = listOf(
        NavItem("Home", Screen.Dashboard, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavItem("Inventory", Screen.Inventory, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
        NavItem("Billing", Screen.Billing, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_billing"),
        NavItem("Reports", Screen.Reports, Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        items.forEach { item ->
            val isSelected = when (item.screen) {
                is Screen.Dashboard -> currentScreen is Screen.Dashboard
                is Screen.Inventory -> currentScreen is Screen.Inventory
                is Screen.Billing -> currentScreen is Screen.Billing
                is Screen.Reports -> currentScreen is Screen.Reports
                else -> false
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    if (item.screen is Screen.Billing && cartCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ) {
                                    Text("$cartCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E3A8A),
                    selectedTextColor = Color(0xFF1E3A8A),
                    indicatorColor = Color(0xFFDBEAFE),
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
