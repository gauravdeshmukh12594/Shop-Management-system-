package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SaleEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class RevenueDataPoint(
    val id: String,
    val label: String,        // e.g. "Mon 22", "Sep 26"
    val fullDateLabel: String,// e.g. "Sat, 26 Sep 2026"
    val amount: Double,
    val transactionCount: Int
)

enum class RevenueTimeframe {
    DAILY_7_DAYS,
    DAILY_14_DAYS,
    MONTHLY_YEAR
}

enum class ChartType {
    BAR_CHART,
    AREA_CHART
}

/**
 * Recharts-inspired Interactive Revenue Chart for Jetpack Compose.
 * Supports:
 * - Daily vs Monthly timeframe aggregation
 * - Recharts BarChart and AreaChart visual rendering
 * - Dashed Cartesian Grid with Y-axis currency scales
 * - Average Revenue ReferenceLine
 * - Touch & Hover Tooltip displaying point details, revenue and transaction counts
 */
@Composable
fun RechartsRevenueChart(
    sales: List<SaleEntity>,
    modifier: Modifier = Modifier
) {
    var timeframe by remember { mutableStateOf(RevenueTimeframe.DAILY_7_DAYS) }
    var chartType by remember { mutableStateOf(ChartType.BAR_CHART) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Aggregate sales data based on selected timeframe
    val chartData = remember(sales, timeframe) {
        aggregateSalesData(sales, timeframe)
    }

    val maxRevenue = remember(chartData) {
        (chartData.maxOfOrNull { it.amount } ?: 1000.0).coerceAtLeast(1000.0)
    }

    val totalPeriodRevenue = remember(chartData) {
        chartData.sumOf { it.amount }
    }

    val avgPeriodRevenue = remember(chartData, totalPeriodRevenue) {
        if (chartData.isNotEmpty()) totalPeriodRevenue / chartData.size else 0.0
    }

    val peakDataPoint = remember(chartData) {
        chartData.maxByOrNull { it.amount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_revenue_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header: Title, Metric & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Revenue Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatCurrency(totalPeriodRevenue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = when (timeframe) {
                            RevenueTimeframe.DAILY_7_DAYS -> "Past 7 Days Total"
                            RevenueTimeframe.DAILY_14_DAYS -> "Past 14 Days Total"
                            RevenueTimeframe.MONTHLY_YEAR -> "Annual Revenue"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                // Chart Type Switcher (Bars vs Area)
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (chartType == ChartType.BAR_CHART) Color.White else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { chartType = ChartType.BAR_CHART },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bar Chart",
                            tint = if (chartType == ChartType.BAR_CHART) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (chartType == ChartType.AREA_CHART) Color.White else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { chartType = ChartType.AREA_CHART },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Area Chart",
                            tint = if (chartType == ChartType.AREA_CHART) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Timeframe Selector (7 Days / 14 Days / Monthly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeframeTab(
                    label = "7 Days",
                    isSelected = timeframe == RevenueTimeframe.DAILY_7_DAYS,
                    onClick = {
                        timeframe = RevenueTimeframe.DAILY_7_DAYS
                        selectedIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
                TimeframeTab(
                    label = "14 Days",
                    isSelected = timeframe == RevenueTimeframe.DAILY_14_DAYS,
                    onClick = {
                        timeframe = RevenueTimeframe.DAILY_14_DAYS
                        selectedIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
                TimeframeTab(
                    label = "Monthly",
                    isSelected = timeframe == RevenueTimeframe.MONTHLY_YEAR,
                    onClick = {
                        timeframe = RevenueTimeframe.MONTHLY_YEAR
                        selectedIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive Tooltip Card (Recharts-style popover)
            if (selectedIndex in chartData.indices) {
                val point = chartData[selectedIndex]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = point.fullDateLabel,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF38BDF8), CircleShape)
                                )
                                Text(
                                    text = "Revenue: ${formatCurrency(point.amount)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3338BDF8)
                        ) {
                            Text(
                                text = "${point.transactionCount} bills",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Hint to interact
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Touch any bar or point for detailed metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp, 2.dp)
                                .background(Color(0xFFF59E0B))
                        )
                        Text(
                            text = "Avg: ${formatCurrency(avgPeriodRevenue)}",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Recharts Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartData) {
                            detectTapGestures { offset ->
                                val count = chartData.size
                                if (count > 0) {
                                    val slotWidth = size.width / count
                                    val index = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                    selectedIndex = if (selectedIndex == index) -1 else index
                                }
                            }
                        }
                        .pointerInput(chartData) {
                            detectDragGestures { change, _ ->
                                val count = chartData.size
                                if (count > 0) {
                                    val slotWidth = size.width / count
                                    val index = (change.position.x / slotWidth).toInt().coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 26f
                    val topPadding = 18f
                    val chartHeight = height - bottomPadding - topPadding

                    val itemCount = chartData.size
                    if (itemCount == 0) return@Canvas

                    // --- A. Cartesian Grid (Dashed Horizontal Lines) ---
                    val gridSteps = 4
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    for (i in 0..gridSteps) {
                        val y = topPadding + (chartHeight * i / gridSteps)
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // --- B. Average Reference Line (Recharts <ReferenceLine />) ---
                    if (maxRevenue > 0 && avgPeriodRevenue > 0) {
                        val avgY = topPadding + chartHeight * (1f - (avgPeriodRevenue.toFloat() / maxRevenue.toFloat())).coerceIn(0f, 1f)
                        val avgDashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = Offset(0f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 2f,
                            pathEffect = avgDashEffect
                        )
                    }

                    val slotWidth = width / itemCount

                    // --- C. Drawing Mode: BAR_CHART ---
                    if (chartType == ChartType.BAR_CHART) {
                        val barWidth = (slotWidth * 0.55f).coerceAtLeast(10f).coerceAtMost(36f)

                        chartData.forEachIndexed { i, item ->
                            val isSelected = i == selectedIndex
                            val fraction = (item.amount / maxRevenue).toFloat().coerceIn(0.04f, 1f)
                            val barHeight = chartHeight * fraction
                            val x = (i * slotWidth) + (slotWidth - barWidth) / 2f
                            val y = topPadding + (chartHeight - barHeight)

                            // Gradient Fill
                            val barBrush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                                    startY = y,
                                    endY = topPadding + chartHeight
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB)),
                                    startY = y,
                                    endY = topPadding + chartHeight
                                )
                            }

                            // Rounded top bar
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )

                            // Selection highlight outline
                            if (isSelected) {
                                drawRoundRect(
                                    color = Color(0xFF1E3A8A),
                                    topLeft = Offset(x - 2f, y - 2f),
                                    size = Size(barWidth + 4f, barHeight + 4f),
                                    cornerRadius = CornerRadius(barWidth / 2f + 2f, barWidth / 2f + 2f),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }

                    // --- D. Drawing Mode: AREA_CHART (Smooth spline + filled gradient) ---
                    if (chartType == ChartType.AREA_CHART) {
                        val points = chartData.mapIndexed { i, item ->
                            val fraction = (item.amount / maxRevenue).toFloat().coerceIn(0f, 1f)
                            val x = (i * slotWidth) + (slotWidth / 2f)
                            val y = topPadding + (chartHeight * (1f - fraction))
                            Offset(x, y)
                        }

                        if (points.size >= 2) {
                            val linePath = Path()
                            val fillPath = Path()

                            linePath.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(points.first().x, topPadding + chartHeight)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }

                            fillPath.lineTo(points.last().x, topPadding + chartHeight)
                            fillPath.close()

                            // Soft gradient beneath area curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0x662563EB), Color(0x052563EB)),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )

                            // Stroke line on top
                            drawPath(
                                path = linePath,
                                color = Color(0xFF2563EB),
                                style = Stroke(width = 4f, cap = StrokeCap.Round)
                            )

                            // Data point dots
                            points.forEachIndexed { idx, pt ->
                                val isSelected = idx == selectedIndex
                                drawCircle(
                                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF2563EB),
                                    radius = if (isSelected) 7f else 4.5f,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 3.5f else 2f,
                                    center = pt
                                )
                            }
                        }
                    }

                    // --- E. Cursor Line for Selected Point ---
                    if (selectedIndex in 0 until itemCount) {
                        val cursorX = (selectedIndex * slotWidth) + (slotWidth / 2f)
                        drawLine(
                            color = Color(0xFF2563EB),
                            start = Offset(cursorX, topPadding),
                            end = Offset(cursorX, topPadding + chartHeight),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }
            }

            // 5. X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                chartData.forEachIndexed { i, item ->
                    val isSelected = i == selectedIndex
                    Text(
                        text = item.label,
                        fontSize = if (timeframe == RevenueTimeframe.DAILY_14_DAYS) 9.sp else 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Summary Footer Metrics (Peak Day, Daily Avg, Bills Count)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Peak Revenue",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formatCurrency(peakDataPoint?.amount ?: 0.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = peakDataPoint?.label ?: "-",
                        fontSize = 10.sp,
                        color = Color(0xFF2563EB)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Avg per Period",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formatCurrency(avgPeriodRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Benchmark",
                        fontSize = 10.sp,
                        color = Color(0xFFD97706)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${chartData.sumOf { it.transactionCount }} Bills",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Recorded",
                        fontSize = 10.sp,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeframeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}

/**
 * Aggregates database sales into structured data points for daily and monthly chart buckets.
 */
private fun aggregateSalesData(
    sales: List<SaleEntity>,
    timeframe: RevenueTimeframe
): List<RevenueDataPoint> {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()

    return when (timeframe) {
        RevenueTimeframe.DAILY_7_DAYS -> {
            // Past 7 Days: Today - 6 to Today
            val result = mutableListOf<RevenueDataPoint>()
            val dayFormat = SimpleDateFormat("EEE d", Locale.getDefault())
            val fullFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

            for (i in 6 downTo 0) {
                calendar.timeInMillis = now - (i * 86400000L)
                val year = calendar.get(Calendar.YEAR)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

                val daySales = sales.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
                    cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == dayOfYear
                }

                val total = daySales.sumOf { it.totalAmount }
                result.add(
                    RevenueDataPoint(
                        id = "d7_$i",
                        label = dayFormat.format(calendar.time),
                        fullDateLabel = fullFormat.format(calendar.time),
                        amount = total,
                        transactionCount = daySales.size
                    )
                )
            }
            result
        }

        RevenueTimeframe.DAILY_14_DAYS -> {
            // Past 14 Days
            val result = mutableListOf<RevenueDataPoint>()
            val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
            val fullFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())

            for (i in 13 downTo 0) {
                calendar.timeInMillis = now - (i * 86400000L)
                val year = calendar.get(Calendar.YEAR)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

                val daySales = sales.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
                    cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == dayOfYear
                }

                val total = daySales.sumOf { it.totalAmount }
                result.add(
                    RevenueDataPoint(
                        id = "d14_$i",
                        label = dayFormat.format(calendar.time),
                        fullDateLabel = fullFormat.format(calendar.time),
                        amount = total,
                        transactionCount = daySales.size
                    )
                )
            }
            result
        }

        RevenueTimeframe.MONTHLY_YEAR -> {
            // 12 Months of the current year (or rolling 6-8 months)
            val result = mutableListOf<RevenueDataPoint>()
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val fullFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Look back up to 7 months up to current
            for (mOffset in 6 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth - mOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val m = cal.get(Calendar.MONTH)
                val y = cal.get(Calendar.YEAR)

                val monthSales = sales.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
                    c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                }

                val total = monthSales.sumOf { it.totalAmount }
                result.add(
                    RevenueDataPoint(
                        id = "m_${y}_$m",
                        label = monthFormat.format(cal.time),
                        fullDateLabel = fullFormat.format(cal.time),
                        amount = total,
                        transactionCount = monthSales.size
                    )
                )
            }
            result
        }
    }
}
