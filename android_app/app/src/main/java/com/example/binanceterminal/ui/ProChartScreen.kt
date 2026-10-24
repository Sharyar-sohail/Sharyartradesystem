package com.example.binanceterminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.api.BinanceApiClient
import com.example.binanceterminal.engine.SignalRepository
import com.example.binanceterminal.model.Candle
import com.example.binanceterminal.model.TradeSignal
import com.example.binanceterminal.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class BubbleOpportunity(
    val symbol: String,
    val direction: String, // "LONG" or "SHORT"
    val strategyName: String,
    val expectedMovePct: Double,
    val riskReward: Double,
    val confidence: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target10pct: Double,
    val target20pct: Double,
    val targetMega: Double
)

@Composable
fun ProChartScreen(
    initialSignal: TradeSignal? = null,
    initialSymbol: String? = null
) {
    var symbol by remember(initialSignal, initialSymbol) {
        mutableStateOf(initialSignal?.symbol ?: initialSymbol ?: "PEPE/USDT")
    }
    var timeframe by remember { mutableStateOf("15m") }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStrategyName by remember { mutableStateOf(initialSignal?.strategyName ?: "👑 Candle King Geometry") }

    val liveSignals by SignalRepository.signals.collectAsState()

    // 1. Build a Rich, Dense Market Universe of Crypto Bubbles (25-35 Coins)
    val bubbleList = remember(liveSignals) {
        val list = mutableListOf<BubbleOpportunity>()
        val seen = mutableSetOf<String>()

        liveSignals.forEach { s ->
            if (s.symbol !in seen) {
                seen.add(s.symbol)
                list.add(
                    BubbleOpportunity(
                        symbol = s.symbol,
                        direction = s.direction,
                        strategyName = s.strategyName,
                        expectedMovePct = s.expectedMovePct,
                        riskReward = s.riskRewardRatio,
                        confidence = s.aiConfidencePct,
                        entryPrice = s.entryPrice,
                        stopLoss = s.stopLoss,
                        target10pct = s.target10pct,
                        target20pct = s.target20pct,
                        targetMega = s.targetMega
                    )
                )
            }
        }

        // Expanded baseline universe to form a dense, organic crypto bubble cloud
        val standardUniverse = listOf(
            BubbleOpportunity("PEPE/USDT", "LONG", "👑 Candle King Geometry", 145.0, 9.2, 97.5, 0.0000095, 0.0000088, 0.0000105, 0.0000114, 0.0000215),
            BubbleOpportunity("SOL/USDT", "LONG", "🏛️ Smart Money Concepts (SMC)", 28.5, 4.5, 94.0, 182.0, 172.0, 200.0, 218.0, 280.0),
            BubbleOpportunity("BTC/USDT", "LONG", "⚡ Bollinger Squeeze Volatility Coil", 18.0, 3.8, 95.0, 82050.0, 79200.0, 90250.0, 98460.0, 125000.0),
            BubbleOpportunity("DOGE/USDT", "LONG", "💥 Master Mega-Pump Base Ignition", 72.0, 6.8, 93.5, 0.185, 0.168, 0.205, 0.222, 0.320),
            BubbleOpportunity("ETH/USDT", "SHORT", "🎯 ICT Inner Circle Trader", -21.0, 4.1, 91.5, 2405.0, 2520.0, 2165.0, 1924.0, 1450.0),
            BubbleOpportunity("SUI/USDT", "LONG", "👑 Candle King Geometry", 88.0, 7.5, 96.0, 2.15, 1.95, 2.38, 2.58, 4.10),
            BubbleOpportunity("XRP/USDT", "SHORT", "📊 Volume Spread Analysis (VSA)", -16.5, 3.2, 89.5, 0.52, 0.55, 0.47, 0.42, 0.28),
            BubbleOpportunity("ARB/USDT", "LONG", "⚡ Bollinger Squeeze Breakout", 24.0, 4.2, 93.0, 0.58, 0.54, 0.64, 0.70, 0.95),
            BubbleOpportunity("AERO/USDT", "LONG", "👑 Candle King Geometry", 55.0, 6.0, 95.5, 1.25, 1.15, 1.38, 1.50, 2.10),
            BubbleOpportunity("NEAR/USDT", "LONG", "🏛️ Smart Money Concepts (SMC)", 32.0, 5.1, 92.5, 4.85, 4.50, 5.35, 5.82, 7.50),
            BubbleOpportunity("AVAX/USDT", "SHORT", "📊 Volume Spread Analysis (VSA)", -19.0, 3.5, 90.0, 26.50, 28.0, 23.85, 21.46, 16.50),
            BubbleOpportunity("LINK/USDT", "LONG", "⚡ Bollinger Squeeze Volatility Coil", 22.0, 4.0, 93.5, 11.80, 11.10, 12.98, 14.16, 18.50),
            BubbleOpportunity("FLM/USDT", "LONG", "👑 Candle King Geometry", 42.0, 5.5, 94.0, 0.1143, 0.1060, 0.1257, 0.1372, 0.1850),
            BubbleOpportunity("REN/USDT", "SHORT", "🎯 ICT Inner Circle Trader", -20.0, 3.8, 91.0, 0.0450, 0.0485, 0.0405, 0.0360, 0.0250),
            BubbleOpportunity("ELF/USDT", "LONG", "🏛️ Smart Money Concepts (SMC)", 26.0, 4.4, 92.0, 0.4120, 0.3850, 0.4530, 0.4944, 0.6500),
            BubbleOpportunity("DOT/USDT", "LONG", "⚡ Bollinger Squeeze Breakout", 19.5, 3.6, 91.5, 4.10, 3.88, 4.51, 4.92, 6.50),
            BubbleOpportunity("INJ/USDT", "LONG", "👑 Candle King Geometry", 38.0, 5.2, 95.0, 21.40, 19.80, 23.54, 25.68, 34.0),
            BubbleOpportunity("PUMP/USDT", "SHORT", "📊 Volume Spread Analysis (VSA)", -25.0, 4.2, 89.0, 0.0035, 0.0039, 0.0031, 0.0028, 0.0018),
            BubbleOpportunity("DASH/USDT", "SHORT", "🎯 ICT Inner Circle Trader", -18.0, 3.4, 90.5, 24.20, 25.80, 21.78, 19.84, 15.00),
            BubbleOpportunity("ZRO/USDT", "SHORT", "📊 Volume Spread Analysis (VSA)", -15.0, 3.1, 89.5, 3.85, 4.10, 3.46, 3.27, 2.50)
        )

        standardUniverse.forEach { d ->
            if (d.symbol !in seen) {
                seen.add(d.symbol)
                list.add(d)
            }
        }
        list
    }

    // Keep activeBubble and symbol strictly synchronized
    val activeBubble = remember(symbol, bubbleList) {
        bubbleList.firstOrNull { it.symbol == symbol } ?: bubbleList.first()
    }

    LaunchedEffect(bubbleList) {
        if (bubbleList.isNotEmpty() && bubbleList.none { it.symbol == symbol }) {
            symbol = bubbleList.first().symbol
            selectedStrategyName = bubbleList.first().strategyName
        }
    }

    LaunchedEffect(symbol, timeframe) {
        isLoading = true
        candles = BinanceApiClient.fetchOhlcv(symbol, timeframe = timeframe, limit = 50)
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen Header
        item {
            Text(
                text = "📊 PRO CANDLESTICK CHART & STRATEGY ENGINE",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Real-time institutional candlesticks • SMC order blocks • Target corridors • Dynamic R:R",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        // 1. Sleek Asset Selector Strip (Minimalist & Clean)
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "SELECT ASSET PAIR",
                        color = TerminalCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bubbleList.take(16).forEach { b ->
                            val isSelected = b.symbol == symbol
                            Surface(
                                color = if (isSelected) TerminalCyan.copy(alpha = 0.2f) else SurfaceCard,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) TerminalCyan else CardBorder
                                ),
                                modifier = Modifier.clickable {
                                    symbol = b.symbol
                                    selectedStrategyName = b.strategyName
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (b.direction == "LONG") "🟢" else "🔴",
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = b.symbol,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${if (b.expectedMovePct >= 0) "+" else ""}${b.expectedMovePct.toInt()}%",
                                        color = if (b.direction == "LONG") TerminalGreen else TerminalRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. STRATEGY TRANSPARENCY CARD (PRE-LOADED FOR ACTIVE BUBBLE)
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (activeBubble.direction == "LONG") TerminalGreen.copy(alpha = 0.2f) else TerminalRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (activeBubble.direction == "LONG") TerminalGreen else TerminalRed)
                                ) {
                                    Text(
                                        text = activeBubble.direction,
                                        color = if (activeBubble.direction == "LONG") TerminalGreen else TerminalRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = symbol,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Strategy: ${activeBubble.strategyName}",
                                color = TerminalCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "MOVE TARGET",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (activeBubble.expectedMovePct >= 0) "+" else ""}${String.format("%.1f", activeBubble.expectedMovePct)}%",
                                color = if (activeBubble.direction == "LONG") TerminalGreen else TerminalRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Strategy Key Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LevelCard(title = "ENTRY", value = "$${formatPrice(activeBubble.entryPrice)}", color = TextGold, modifier = Modifier.weight(1f))
                        LevelCard(title = "STOP LOSS", value = "$${formatPrice(activeBubble.stopLoss)}", color = TerminalRed, modifier = Modifier.weight(1f))
                        LevelCard(title = "TP1 (+10%)", value = "$${formatPrice(activeBubble.target10pct)}", color = TerminalGreen, modifier = Modifier.weight(1f))
                        LevelCard(title = "TP2 (+20%)", value = "$${formatPrice(activeBubble.target20pct)}", color = TerminalGreen, modifier = Modifier.weight(1f))
                        LevelCard(title = "R:R", value = "1:${String.format("%.1f", activeBubble.riskReward)}", color = TerminalCyan, modifier = Modifier.weight(0.9f))
                    }
                }
            }
        }

        // Timeframe Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("15m", "1h", "4h", "1d").forEach { tf ->
                    val isTfSelected = timeframe == tf
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { timeframe = tf },
                        color = if (isTfSelected) TerminalCyan else SurfaceDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTfSelected) TerminalCyan else CardBorder
                        )
                    ) {
                        Text(
                            text = tf.uppercase(),
                            color = if (isTfSelected) BgDark else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. CANDLESTICK CHART VIEW (FIXED ASPECT RATIO & BALANCED VERTICAL FILL)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TerminalCyan)
                    }
                } else if (candles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Loading candles for $symbol...", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    CandlestickCanvasWithOverlays(
                        candles = candles,
                        bubble = activeBubble,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(text = title, color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = value, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun CandlestickCanvasWithOverlays(
    candles: List<Candle>,
    bubble: BubbleOpportunity,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val chartHeight = h * 0.80f
        val volumeHeight = h * 0.16f

        // Display recent 35 candles so candle bodies have bold, distinct thickness
        val displayCandles = if (candles.size > 35) candles.takeLast(35) else candles
        if (displayCandles.isEmpty()) return@Canvas

        val candleLow = displayCandles.minOf { it.low }
        val candleHigh = displayCandles.maxOf { it.high }
        val candleSpan = max(candleHigh - candleLow, candleLow * 0.005)

        // Healthily scaled Y-bounds focused directly on price action so candles occupy 80% of canvas height
        // Distant targets will NOT compress candlesticks into flat lines!
        val pad = candleSpan * 0.12
        val minPrice = candleLow - pad
        val maxPrice = candleHigh + pad
        val priceRange = max(maxPrice - minPrice, 1e-6)
        val maxVolume = max(displayCandles.maxOf { it.volume }, 1.0)

        fun priceToY(price: Double): Float {
            val normalized = (price - minPrice) / priceRange
            return (chartHeight - (normalized * chartHeight)).toFloat()
        }

        val candleCount = displayCandles.size
        val candleSpacing = w / candleCount.toFloat()
        val candleBodyWidth = max(candleSpacing * 0.70f, 6f)

        // Shaded Risk Corridor between Entry and Stop Loss (only if within view)
        if (bubble.stopLoss in (minPrice - candleSpan * 0.5)..(maxPrice + candleSpan * 0.5)) {
            val entryY = priceToY(bubble.entryPrice).coerceIn(0f, chartHeight)
            val slY = priceToY(bubble.stopLoss).coerceIn(0f, chartHeight)
            drawRect(
                color = TerminalRed.copy(alpha = 0.08f),
                topLeft = Offset(0f, min(entryY, slY)),
                size = Size(w, max(abs(slY - entryY), 2f))
            )
        }

        // Shaded Profit Corridor
        if (bubble.target10pct in (minPrice - candleSpan * 0.5)..(maxPrice + candleSpan * 0.5)) {
            val entryY = priceToY(bubble.entryPrice).coerceIn(0f, chartHeight)
            val tpY = priceToY(bubble.target10pct).coerceIn(0f, chartHeight)
            drawRect(
                color = TerminalGreen.copy(alpha = 0.08f),
                topLeft = Offset(0f, min(entryY, tpY)),
                size = Size(w, max(abs(tpY - entryY), 2f))
            )
        }

        // Draw Volume Bars
        for (i in displayCandles.indices) {
            val c = displayCandles[i]
            val xCenter = i * candleSpacing + (candleSpacing / 2f)
            val vHeight = (c.volume / maxVolume * volumeHeight).toFloat()
            val vColor = if (c.close >= c.open) TerminalGreen.copy(alpha = 0.35f) else TerminalRed.copy(alpha = 0.35f)

            drawRect(
                color = vColor,
                topLeft = Offset(xCenter - (candleBodyWidth / 2f), h - vHeight),
                size = Size(candleBodyWidth, vHeight)
            )
        }

        // Draw Candlesticks (Wick + Body)
        for (i in displayCandles.indices) {
            val c = displayCandles[i]
            val xCenter = i * candleSpacing + (candleSpacing / 2f)
            val highY = priceToY(c.high)
            val lowY = priceToY(c.low)
            val openY = priceToY(c.open)
            val closeY = priceToY(c.close)

            val isGreen = c.close >= c.open
            val candleColor = if (isGreen) TerminalGreen else TerminalRed

            // Wick
            drawLine(
                color = candleColor,
                start = Offset(xCenter, highY),
                end = Offset(xCenter, lowY),
                strokeWidth = 1.8f
            )

            // Body
            val topBodyY = min(openY, closeY)
            val bodyHeight = max(abs(closeY - openY), 2.5f)
            drawRect(
                color = candleColor,
                topLeft = Offset(xCenter - (candleBodyWidth / 2f), topBodyY),
                size = Size(candleBodyWidth, bodyHeight)
            )
        }

        // Strategic Horizontal Overlays: Entry, SL, TP1
        val levels = listOf(
            Triple("ENTRY", bubble.entryPrice, TextGold),
            Triple("HARD SL", bubble.stopLoss, TerminalRed),
            Triple("TP1", bubble.target10pct, TerminalGreen)
        )

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

        for ((label, p, col) in levels) {
            if (p in minPrice..maxPrice) {
                val y = priceToY(p)
                drawLine(
                    color = col,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.6f,
                    pathEffect = if (label == "ENTRY") null else dashedEffect
                )
            }
        }
    }
}

private fun formatPrice(p: Double): String {
    return if (p < 0.001) String.format("%.6f", p)
    else if (p < 1.0) String.format("%.4f", p)
    else String.format("%.2f", p)
}
