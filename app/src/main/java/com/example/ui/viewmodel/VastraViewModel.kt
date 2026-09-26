package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VastraApp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.util.BarcodeUtil
import com.example.util.ImageStorageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    object Dashboard : Screen()
    object Inventory : Screen()
    object AddProduct : Screen()
    data class ProductDetail(val productId: Long) : Screen()
    data class ProductSavedSuccess(val productId: Long) : Screen()
    object IdentifyProduct : Screen()
    object Billing : Screen()
    object Payment : Screen()
    data class Invoice(val saleId: Long) : Screen()
    object Reports : Screen()
}

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Double get() = product.sellingPrice * quantity
    val totalCost: Double get() = product.purchasePrice * quantity
}

class VastraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VastraApp).repository

    // Current navigation screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Screen back-stack for seamless navigation
    private val screenStack = mutableListOf<Screen>(Screen.Dashboard)

    fun navigateTo(screen: Screen) {
        screenStack.add(screen)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    // Top dashboard stats
    val productCount = repository.productCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val totalStockCount = repository.totalStockCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val lowStockCount = repository.lowStockCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val todaySalesCount = repository.todaySalesCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val todaySalesTotal = repository.todaySalesTotal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0
    )

    // Inventory Search & Filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    val productsList: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        _searchQuery,
        _selectedCategoryFilter
    ) { all, query, category ->
        var filtered = all
        if (category == "Low Stock") {
            filtered = filtered.filter { it.stock <= it.lowStockThreshold }
        } else if (category != "All") {
            filtered = filtered.filter { product ->
                when (category) {
                    "Shirts" -> product.category.equals("Shirt", ignoreCase = true) || 
                                product.category.equals("Shirts", ignoreCase = true) ||
                                product.name.contains("Shirt", ignoreCase = true)
                    "Pants" -> product.category.equals("Pant", ignoreCase = true) || 
                               product.category.equals("Pants", ignoreCase = true) ||
                               product.category.equals("Jeans", ignoreCase = true) ||
                               product.category.equals("Trouser", ignoreCase = true) ||
                               product.category.equals("Trousers", ignoreCase = true) ||
                               product.name.contains("Pant", ignoreCase = true) ||
                               product.name.contains("Jeans", ignoreCase = true)
                    "Sarees" -> product.category.equals("Saree", ignoreCase = true) ||
                                product.category.equals("Sarees", ignoreCase = true) ||
                                product.name.contains("Saree", ignoreCase = true)
                    "Kurtis" -> product.category.equals("Kurti", ignoreCase = true) ||
                                product.category.equals("Kurtis", ignoreCase = true) ||
                                product.category.equals("Kurta", ignoreCase = true) ||
                                product.name.contains("Kurti", ignoreCase = true) ||
                                product.name.contains("Kurta", ignoreCase = true)
                    "Lehengas" -> product.category.equals("Lehenga", ignoreCase = true) ||
                                  product.category.equals("Lehengas", ignoreCase = true) ||
                                  product.name.contains("Lehenga", ignoreCase = true)
                    "Accessories" -> product.category.equals("Accessory", ignoreCase = true) ||
                                     product.category.equals("Accessories", ignoreCase = true) ||
                                     product.category.equals("Dupatta", ignoreCase = true) ||
                                     product.category.equals("Shawl", ignoreCase = true) ||
                                     product.name.contains("Dupatta", ignoreCase = true) ||
                                     product.name.contains("Shawl", ignoreCase = true) ||
                                     product.name.contains("Stole", ignoreCase = true) ||
                                     product.name.contains("Scarf", ignoreCase = true)
                    "Suits" -> product.category.equals("Suit", ignoreCase = true) ||
                               product.category.equals("Suits", ignoreCase = true) ||
                               product.name.contains("Suit", ignoreCase = true) ||
                               product.name.contains("Salwar", ignoreCase = true)
                    "Dresses" -> product.category.equals("Dress", ignoreCase = true) ||
                                 product.category.equals("Dresses", ignoreCase = true) ||
                                 product.name.contains("Dress", ignoreCase = true) ||
                                 product.name.contains("Gown", ignoreCase = true)
                    else -> product.category.contains(category, ignoreCase = true) ||
                            product.name.contains(category, ignoreCase = true)
                }
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.barcode.lowercase().contains(q) ||
                it.brand.lowercase().contains(q) ||
                it.fabric.lowercase().contains(q) ||
                it.colour.lowercase().contains(q)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = repository.lowStockProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allTransactions = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSales = repository.allSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSaleItems = repository.allSaleItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // -------------------------------------------------------------
    // Add Product Form State
    // -------------------------------------------------------------
    val addName = MutableStateFlow("")
    val addCategory = MutableStateFlow("Saree")
    val addBrand = MutableStateFlow("")
    val addColour = MutableStateFlow("")
    val addFabric = MutableStateFlow("Silk")
    val addPurchasePrice = MutableStateFlow("")
    val addSellingPrice = MutableStateFlow("")
    val addStock = MutableStateFlow("1")
    val addNotes = MutableStateFlow("")
    val addImagePath = MutableStateFlow<String?>(null)
    val addSku = MutableStateFlow("")
    val addLowStockThreshold = MutableStateFlow("3")
    val addErrorMessage = MutableStateFlow<String?>(null)
    val editingProductId = MutableStateFlow<Long?>(null)

    fun prepareAddProduct() {
        editingProductId.value = null
        val nextSeq = System.currentTimeMillis() % 900000 + 100000
        val cat = addCategory.value.ifEmpty { "Saree" }
        val generatedSku = BarcodeUtil.generateSku(cat, nextSeq)
        addName.value = ""
        addBrand.value = ""
        addColour.value = ""
        addPurchasePrice.value = ""
        addSellingPrice.value = ""
        addStock.value = "5"
        addNotes.value = ""
        addImagePath.value = null
        addSku.value = generatedSku
        addLowStockThreshold.value = "3"
        addErrorMessage.value = null
    }

    fun prepareEditProduct(product: ProductEntity) {
        editingProductId.value = product.id
        addName.value = product.name
        addCategory.value = product.category
        addBrand.value = product.brand
        addColour.value = product.colour
        addFabric.value = product.fabric
        addPurchasePrice.value = if (product.purchasePrice > 0) product.purchasePrice.toString() else ""
        addSellingPrice.value = if (product.sellingPrice > 0) product.sellingPrice.toString() else ""
        addStock.value = product.stock.toString()
        addNotes.value = product.notes
        addImagePath.value = product.imagePath
        addSku.value = product.sku
        addLowStockThreshold.value = product.lowStockThreshold.toString()
        addErrorMessage.value = null
    }

    fun onCategoryChanged(newCat: String) {
        addCategory.value = newCat
        if (editingProductId.value == null) {
            val nextSeq = System.currentTimeMillis() % 900000 + 100000
            addSku.value = BarcodeUtil.generateSku(newCat, nextSeq)
        }
    }

    fun setAddProductImage(path: String?) {
        addImagePath.value = path
    }

    fun saveProduct(onSuccess: (Long) -> Unit) {
        val name = addName.value.trim()
        if (name.isEmpty()) {
            addErrorMessage.value = "Please enter product name (e.g. Banarasi Silk Saree)"
            return
        }

        val purchase = addPurchasePrice.value.toDoubleOrNull() ?: 0.0
        val selling = addSellingPrice.value.toDoubleOrNull() ?: 0.0
        if (selling <= 0.0) {
            addErrorMessage.value = "Please enter valid Selling Price / MRP"
            return
        }

        val stockQty = addStock.value.toIntOrNull() ?: 0
        val threshold = addLowStockThreshold.value.toIntOrNull() ?: 3
        val currentEditId = editingProductId.value

        val sku = addSku.value.ifBlank {
            BarcodeUtil.generateSku(addCategory.value, System.currentTimeMillis() % 1000000)
        }

        viewModelScope.launch {
            if (currentEditId != null) {
                // Update existing product
                val updatedProduct = ProductEntity(
                    id = currentEditId,
                    sku = sku,
                    barcode = sku,
                    name = name,
                    category = addCategory.value,
                    brand = addBrand.value.ifBlank { "Store Brand" },
                    colour = addColour.value.ifBlank { "Standard" },
                    fabric = addFabric.value.ifBlank { "Cotton" },
                    purchasePrice = purchase,
                    sellingPrice = selling,
                    stock = stockQty,
                    lowStockThreshold = threshold,
                    imagePath = addImagePath.value,
                    notes = addNotes.value
                )
                repository.updateProduct(updatedProduct)
                editingProductId.value = null
                onSuccess(currentEditId)
            } else {
                // Create new product
                val product = ProductEntity(
                    sku = sku,
                    barcode = sku,
                    name = name,
                    category = addCategory.value,
                    brand = addBrand.value.ifBlank { "Store Brand" },
                    colour = addColour.value.ifBlank { "Standard" },
                    fabric = addFabric.value.ifBlank { "Cotton" },
                    purchasePrice = purchase,
                    sellingPrice = selling,
                    stock = stockQty,
                    lowStockThreshold = threshold,
                    imagePath = addImagePath.value,
                    notes = addNotes.value
                )
                val newId = repository.addProduct(product)
                onSuccess(newId)
            }
        }
    }

    // -------------------------------------------------------------
    // POS Cart & Billing State
    // -------------------------------------------------------------
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    val cartSubtotal: StateFlow<Double> = combine(_cartItems) { items ->
        items.first().sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = combine(cartSubtotal, _discountAmount) { subtotal, discount ->
        (subtotal - discount).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addToCart(product: ProductEntity, qty: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + qty
            current[index] = existing.copy(quantity = newQty)
        } else {
            current.add(CartItem(product = product, quantity = qty))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Long, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = newQty)
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun setDiscount(amount: Double) {
        _discountAmount.value = amount.coerceAtLeast(0.0)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountAmount.value = 0.0
    }

    // -------------------------------------------------------------
    // Payment & Invoice Creation
    // -------------------------------------------------------------
    val paymentMethod = MutableStateFlow("Cash") // Cash, UPI, Card, Split
    val customerName = MutableStateFlow("")
    val customerPhone = MutableStateFlow("")
    val amountTendered = MutableStateFlow("")

    fun completeSale(onSuccess: (Long) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val subtotal = items.sumOf { it.subtotal }
        val discount = _discountAmount.value
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val invoiceNo = repository.generateNextInvoiceNumber()

        viewModelScope.launch {
            val saleEntity = SaleEntity(
                invoiceNumber = invoiceNo,
                subtotal = subtotal,
                discount = discount,
                totalAmount = total,
                paymentMethod = paymentMethod.value,
                customerName = customerName.value.trim(),
                customerPhone = customerPhone.value.trim()
            )

            val saleItems = items.map { cartItem ->
                SaleItemEntity(
                    saleId = 0, // Handled in repository
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    productSku = cartItem.product.sku,
                    unitPrice = cartItem.product.sellingPrice,
                    purchasePrice = cartItem.product.purchasePrice,
                    quantity = cartItem.quantity,
                    totalPrice = cartItem.subtotal,
                    productImagePath = cartItem.product.imagePath
                )
            }

            val saleId = repository.completeSale(saleEntity, saleItems)
            clearCart()
            customerName.value = ""
            customerPhone.value = ""
            amountTendered.value = ""
            onSuccess(saleId)
        }
    }

    // -------------------------------------------------------------
    // Product Detail & Stock Adjustments
    // -------------------------------------------------------------
    suspend fun getProduct(id: Long): ProductEntity? = repository.getProductById(id)

    fun adjustProductStock(productId: Long, delta: Int, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, reason)
        }
    }

    suspend fun getSaleDetails(saleId: Long): Pair<SaleEntity?, List<SaleItemEntity>> {
        return repository.getSaleWithItems(saleId)
    }

    // -------------------------------------------------------------
    // Barcode Identification & Camera Match
    // -------------------------------------------------------------
    suspend fun identifyProductByBarcode(barcode: String): ProductEntity? {
        return repository.getProductByBarcode(barcode)
    }
}
