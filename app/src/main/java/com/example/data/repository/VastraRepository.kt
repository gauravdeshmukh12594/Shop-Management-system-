package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.util.BarcodeUtil
import com.example.util.ImageStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VastraRepository(private val database: AppDatabase) {

    private val productDao = database.productDao()
    private val saleDao = database.saleDao()
    private val stockTransactionDao = database.stockTransactionDao()

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val productCount: Flow<Int> = productDao.getProductCount()
    val totalStockCount: Flow<Int?> = productDao.getTotalStockCount()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()
    val allTransactions: Flow<List<StockTransactionEntity>> = stockTransactionDao.getAllTransactions()
    val allSaleItems: Flow<List<SaleItemEntity>> = saleDao.getAllSaleItems()

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    val todaySalesCount: Flow<Int> = saleDao.getTodaySalesCount(getStartOfDay())
    val todaySalesTotal: Flow<Double?> = saleDao.getTodaySalesTotal(getStartOfDay())
    val allTimeSalesTotal: Flow<Double?> = saleDao.getAllTimeSalesTotal()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
        productDao.getProductsByCategory(category)

    suspend fun getProductById(id: Long): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductByIdDirect(id)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        val trimmed = barcode.trim()
        productDao.getProductByBarcode(trimmed) ?: productDao.getProductBySku(trimmed)
    }

    suspend fun addProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        // Record initial stock transaction
        if (product.stock > 0) {
            stockTransactionDao.insertTransaction(
                StockTransactionEntity(
                    productId = id,
                    productName = product.name,
                    type = "PURCHASE",
                    quantityChange = product.stock,
                    previousStock = 0,
                    newStock = product.stock,
                    referenceId = product.sku,
                    notes = "Initial Stock Added"
                )
            )
        }
        id
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun adjustStock(productId: Long, quantityChange: Int, reason: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductByIdDirect(productId) ?: return@withContext
        val oldStock = product.stock
        val newStock = (oldStock + quantityChange).coerceAtLeast(0)
        productDao.updateStock(productId, newStock)

        val txType = if (quantityChange >= 0) "ADJUSTMENT (+)" else "ADJUSTMENT (-)"
        stockTransactionDao.insertTransaction(
            StockTransactionEntity(
                productId = productId,
                productName = product.name,
                type = txType,
                quantityChange = quantityChange,
                previousStock = oldStock,
                newStock = newStock,
                referenceId = product.sku,
                notes = reason
            )
        )
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    /**
     * Completes a POS sale:
     * 1. Inserts the SaleEntity
     * 2. Inserts all SaleItemEntity records
     * 3. Automatically decrements inventory stock
     * 4. Logs a StockTransaction for each item
     */
    suspend fun completeSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): Long = withContext(Dispatchers.IO) {
        val saleId = saleDao.insertSale(sale)
        val linkedItems = items.map { it.copy(saleId = saleId) }
        saleDao.insertSaleItems(linkedItems)

        // Decrement stock for each item & record audit trail
        for (item in linkedItems) {
            val product = productDao.getProductByIdDirect(item.productId)
            if (product != null) {
                val previousStock = product.stock
                val newStock = (previousStock - item.quantity).coerceAtLeast(0)
                productDao.updateStock(product.id, newStock)

                stockTransactionDao.insertTransaction(
                    StockTransactionEntity(
                        productId = product.id,
                        productName = product.name,
                        type = "SALE",
                        quantityChange = -item.quantity,
                        previousStock = previousStock,
                        newStock = newStock,
                        referenceId = sale.invoiceNumber,
                        notes = "Sold via ${sale.paymentMethod} - Invoice: ${sale.invoiceNumber}"
                    )
                )
            }
        }
        saleId
    }

    suspend fun getSaleWithItems(saleId: Long): Pair<SaleEntity?, List<SaleItemEntity>> =
        withContext(Dispatchers.IO) {
            val sale = saleDao.getSaleByIdDirect(saleId)
            val items = if (sale != null) saleDao.getItemsForSaleDirect(saleId) else emptyList()
            Pair(sale, items)
        }

    suspend fun getSaleByInvoice(invoiceNumber: String): Pair<SaleEntity?, List<SaleItemEntity>> =
        withContext(Dispatchers.IO) {
            val sale = saleDao.getSaleByInvoice(invoiceNumber)
            val items = if (sale != null) saleDao.getItemsForSaleDirect(sale.id) else emptyList()
            Pair(sale, items)
        }

    fun generateNextInvoiceNumber(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        return "INV-$dateStr-$randomSuffix"
    }

    /**
     * Pre-populates the database with realistic clothing shop items and photos swatches
     * if the database is currently empty.
     */
    suspend fun seedSampleProductsIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val currentCount = productDao.getProductCount().first()
        if (currentCount > 0) return@withContext

        val sampleData = listOf(
            ProductEntity(
                sku = "SAR-000125",
                barcode = "SAR-000125",
                name = "Banarasi Silk Saree",
                category = "Saree",
                brand = "Shree Textiles",
                colour = "Crimson Red",
                fabric = "Pure Silk",
                purchasePrice = 2800.0,
                sellingPrice = 4499.0,
                stock = 12,
                lowStockThreshold = 3,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Banarasi Silk Saree", "Saree", 0xFF991B1B.toInt(), 0xFFDC2626.toInt()
                ),
                notes = "Heavy golden zari work, includes unstitched blouse piece"
            ),
            ProductEntity(
                sku = "SAR-000126",
                barcode = "SAR-000126",
                name = "Kanjeevaram Pattu Saree",
                category = "Saree",
                brand = "Valli Silks",
                colour = "Royal Blue",
                fabric = "Kanjeevaram Silk",
                purchasePrice = 3200.0,
                sellingPrice = 5299.0,
                stock = 2,
                lowStockThreshold = 3,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Kanjeevaram Saree", "Saree", 0xFF1E3A8A.toInt(), 0xFF2563EB.toInt()
                ),
                notes = "Traditional temple border weaving"
            ),
            ProductEntity(
                sku = "SAR-000127",
                barcode = "SAR-000127",
                name = "Chanderi Cotton Saree",
                category = "Saree",
                brand = "Crafts of India",
                colour = "Pastel Yellow",
                fabric = "Cotton Silk",
                purchasePrice = 850.0,
                sellingPrice = 1499.0,
                stock = 8,
                lowStockThreshold = 2,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Chanderi Saree", "Saree", 0xFFD97706.toInt(), 0xFFFBBF24.toInt()
                ),
                notes = "Lightweight summer festive collection"
            ),
            ProductEntity(
                sku = "KRT-000128",
                barcode = "KRT-000128",
                name = "Embroidered Anarkali Kurti",
                category = "Kurti",
                brand = "Rivaaz Fashion",
                colour = "Emerald Green",
                fabric = "Rayon",
                purchasePrice = 650.0,
                sellingPrice = 1299.0,
                stock = 15,
                lowStockThreshold = 4,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Anarkali Kurti", "Kurti", 0xFF065F46.toInt(), 0xFF059669.toInt()
                ),
                notes = "Mirror work on yoke, 3/4th sleeves"
            ),
            ProductEntity(
                sku = "KRT-000129",
                barcode = "KRT-000129",
                name = "Printed Cotton Straight Kurti",
                category = "Kurti",
                brand = "Biba Style",
                colour = "Rose Pink",
                fabric = "Pure Cotton",
                purchasePrice = 450.0,
                sellingPrice = 899.0,
                stock = 1,
                lowStockThreshold = 3,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Cotton Kurti", "Kurti", 0xFFBE185D.toInt(), 0xFFF43F5E.toInt()
                ),
                notes = "Daily office & casual wear"
            ),
            ProductEntity(
                sku = "SHR-000130",
                barcode = "SHR-000130",
                name = "Men's Premium Linen Shirt",
                category = "Shirt",
                brand = "Raymond Club",
                colour = "Crisp White",
                fabric = "100% Pure Linen",
                purchasePrice = 900.0,
                sellingPrice = 1799.0,
                stock = 7,
                lowStockThreshold = 2,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Pure Linen Shirt", "Shirt", 0xFF475569.toInt(), 0xFF94A3B8.toInt()
                ),
                notes = "Slim fit, breathable collar"
            ),
            ProductEntity(
                sku = "PNT-000131",
                barcode = "PNT-000131",
                name = "Formal Trouser Slim Fit",
                category = "Pant",
                brand = "Park Avenue",
                colour = "Charcoal Black",
                fabric = "Poly-Viscose",
                purchasePrice = 750.0,
                sellingPrice = 1599.0,
                stock = 10,
                lowStockThreshold = 3,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Formal Trouser", "Pant", 0xFF1E293B.toInt(), 0xFF334155.toInt()
                ),
                notes = "Wrinkle-resistant fabric"
            ),
            ProductEntity(
                sku = "LHG-000132",
                barcode = "LHG-000132",
                name = "Bridal Embroidered Lehenga",
                category = "Lehenga",
                brand = "Kalamandir",
                colour = "Wine Maroon",
                fabric = "Velvet & Net",
                purchasePrice = 8500.0,
                sellingPrice = 14999.0,
                stock = 2,
                lowStockThreshold = 2,
                imagePath = ImageStorageUtil.createSampleFabricSwatch(
                    context, "Bridal Lehenga", "Lehenga", 0xFF831843.toInt(), 0xFF9F1239.toInt()
                ),
                notes = "Heavy zardozi embroidery with matching dupatta"
            )
        )

        for (item in sampleData) {
            addProduct(item)
        }

        seedSampleSalesIfEmpty()
    }

    suspend fun seedSampleSalesIfEmpty() = withContext(Dispatchers.IO) {
        val count = saleDao.getAllTimeSalesCount().first()
        if (count > 0) return@withContext

        val now = System.currentTimeMillis()
        val oneDayMs = 86400000L
        val oneMonthMs = 30L * oneDayMs

        // Sample sales distributed across past 14 days and past 6 months
        val sampleSales = listOf(
            // Past Months
            Triple(now - 5 * oneMonthMs, 32490.0, "Card"),
            Triple(now - 4 * oneMonthMs, 48200.0, "UPI"),
            Triple(now - 3 * oneMonthMs, 61500.0, "Cash"),
            Triple(now - 2 * oneMonthMs, 54800.0, "UPI"),
            Triple(now - 1 * oneMonthMs, 72300.0, "UPI"),

            // Past 14 Days
            Triple(now - 13 * oneDayMs, 4499.0, "UPI"),
            Triple(now - 12 * oneDayMs, 8998.0, "Cash"),
            Triple(now - 11 * oneDayMs, 5299.0, "Card"),
            Triple(now - 10 * oneDayMs, 14999.0, "UPI"),
            Triple(now - 8 * oneDayMs, 7398.0, "Cash"),
            Triple(now - 7 * oneDayMs, 11498.0, "UPI"),
            Triple(now - 6 * oneDayMs, 6299.0, "UPI"),
            Triple(now - 5 * oneDayMs, 18499.0, "Card"),
            Triple(now - 4 * oneDayMs, 9198.0, "UPI"),
            Triple(now - 3 * oneDayMs, 14299.0, "Cash"),
            Triple(now - 2 * oneDayMs, 12498.0, "UPI"),
            Triple(now - 1 * oneDayMs, 16897.0, "UPI"),
            Triple(now - 2 * 3600000L, 8998.0, "UPI") // Today
        )

        sampleSales.forEachIndexed { idx, (timestamp, amount, method) ->
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
            val inv = "INV-$dateStr-${1000 + idx}"
            val sale = SaleEntity(
                invoiceNumber = inv,
                subtotal = amount,
                discount = if (amount > 10000) 500.0 else 0.0,
                totalAmount = amount - if (amount > 10000) 500.0 else 0.0,
                paymentMethod = method,
                customerName = if (idx % 2 == 0) "Walk-in Customer" else "Priya Sharma",
                customerPhone = if (idx % 2 == 0) "" else "9876543210",
                createdAt = timestamp
            )
            val saleId = saleDao.insertSale(sale)

            val cat = when {
                amount >= 14000 -> "Lehenga"
                amount >= 4000 -> "Saree"
                amount >= 2000 -> "Kurti"
                else -> "Shirt"
            }
            val item = SaleItemEntity(
                saleId = saleId,
                productId = 1,
                productName = if (cat == "Saree") "Banarasi Silk Saree" else "$cat Festive Collection",
                productSku = if (cat == "Saree") "SAR-000125" else "KRT-000128",
                quantity = 1,
                purchasePrice = amount * 0.6,
                unitPrice = amount,
                totalPrice = amount,
                productImagePath = null
            )
            saleDao.insertSaleItems(listOf(item))
        }
    }
}
