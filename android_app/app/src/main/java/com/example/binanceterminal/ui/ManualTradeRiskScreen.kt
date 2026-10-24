package com.example.binanceterminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.api.BinanceApiClient
import com.example.binanceterminal.strategy.ManualRiskAnalysisResult
import com.example.binanceterminal.strategy.TradingStrategies
import com.example.binanceterminal.theme.*
import kotlinx.coroutines.launch

@Composable
fun ManualTradeRiskScreen() {
    val scope = rememberCoroutineScope()

    var allPairs by remember {
        mutableStateOf(
            listOf(
                "BTC/USDT", "ETH/USDT", "SOL/USDT", "BNB/USDT", "DOGE/USDT", "XRP/USDT",
                "ADA/USDT", "AVAX/USDT", "NEAR/USDT", "LINK/USDT", "SUI/USDT", "PEPE/USDT"
            )
        )
    }
    var pairSearchQuery by remember { mutableStateOf("") }

    var symbol by remember { mutableStateOf("BTC/USDT") }
    var direction by remember { mutableStateOf("LONG") }
    var entryPriceStr by remember { mutableStateOf("") }
    var stopLossStr by remember { mutableStateOf("") }
    var takeProfitStr by remember { mutableStateOf("") }
    var positionSizeStr by remember { mutableStateOf("100.0") } // $100 default margin/size

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ManualRiskAnalysisResult?>(null) }
    val portfolioList = remember { mutableStateListOf<ManualRiskAnalysisResult>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dynamic Full Binance Pair Loading (Zero Hardcoding)
    LaunchedEffect(Unit) {
        val loaded = BinanceApiClient.getAllActivePairs("USDT")
        if (loaded.isNotEmpty()) {
            allPairs = loaded
        }
        val p = BinanceApiClient.getTickerPrice("BTC/USDT")
        if (p != null) {
            entryPriceStr = String.format("%.4f", p)
            stopLossStr = String.format("%.4f", p * 0.97)
            takeProfitStr = String.format("%.4f", p * 1.10)
        }
    }

    val filteredPairs = remember(pairSearchQuery, allPairs) {
        if (pairSearchQuery.isBlank()) {
            allPairs.take(15)
        } else {
            val q = pairSearchQuery.trim().uppercase()
            allPairs.filter { it.contains(q) }.take(30)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Text(
                text = "🛡️ Manual Trade Risk & Direction Analyzer",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dynamic 500+ Binance Pairs • Risk Projections (1:2 to 1:500) • Right/Wrong Side Momentum",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // Pair Search & Selection
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "DYNAMIC BINANCE PAIR SELECTOR (${allPairs.size} ACTIVE PAIRS)",
                        color = TerminalCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pairSearchQuery,
                        onValueChange = { pairSearchQuery = it },
                        placeholder = { Text("Search 500+ active pairs (e.g. BTC, SOL, PEPE, SUI)...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredPairs) { sym ->
                            val isSelected = symbol == sym
                            Surface(
                                color = if (isSelected) TerminalCyan.copy(alpha = 0.25f) else SurfaceCard,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) TerminalCyan else CardBorder
                                ),
                                modifier = Modifier.clickable {
                                    symbol = sym
                                    scope.launch {
                                        val p = BinanceApiClient.getTickerPrice(sym)
                                        if (p != null) {
                                            entryPriceStr = String.format("%.4f", p)
                                            val sl = if (direction == "LONG") p * 0.97 else p * 1.03
                                            val tp = if (direction == "LONG") p * 1.10 else p * 0.90
                                            stopLossStr = String.format("%.4f", sl)
                                            takeProfitStr = String.format("%.4f", tp)
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = sym,
                                    color = if (isSelected) TerminalCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Position Parameters Card
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "POSITION PARAMETERS & MARGIN SIZING",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Direction Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                direction = "LONG"
                                val p = entryPriceStr.toDoubleOrNull() ?: 1.0
                                stopLossStr = String.format("%.4f", p * 0.97)
                                takeProfitStr = String.format("%.4f", p * 1.10)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "LONG") TerminalGreen else SurfaceCard
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🟢 LONG",
                                color = if (direction == "LONG") BgDark else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                direction = "SHORT"
                                val p = entryPriceStr.toDoubleOrNull() ?: 1.0
                                stopLossStr = String.format("%.4f", p * 1.03)
                                takeProfitStr = String.format("%.4f", p * 0.90)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "SHORT") TerminalRed else SurfaceCard
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔴 SHORT",
                                color = if (direction == "SHORT") BgDark else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Symbol & Position Size / Margin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it.uppercase() },
                            label = { Text("Coin Pair", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.2f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = positionSizeStr,
                            onValueChange = { positionSizeStr = it },
                            label = { Text("Margin / Size ($)", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Entry, SL, TP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = entryPriceStr,
                            onValueChange = { entryPriceStr = it },
                            label = { Text("Entry Price", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Live Price", fontSize = 10.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stopLossStr,
                            onValueChange = { stopLossStr = it },
                            label = { Text("Stop Loss (SL)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = TerminalRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = takeProfitStr,
                            onValueChange = { takeProfitStr = it },
                            label = { Text("Take Profit (TP)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Evaluate Button
                    Button(
                        onClick = {
                            scope.launch {
                                isAnalyzing = true
                                errorMessage = null
                                try {
                                    val candles = BinanceApiClient.fetchOhlcv(symbol, timeframe = "15m", limit = 75)
                                    if (candles.isEmpty()) {
                                        errorMessage = "Failed to load candle data for $symbol"
                                    } else {
                                        val entryP = entryPriceStr.toDoubleOrNull() ?: candles.last().close
                                        val sl = stopLossStr.toDoubleOrNull()
                                        val tp = takeProfitStr.toDoubleOrNull()
                                        val sizeUsd = positionSizeStr.toDoubleOrNull() ?: 100.0

                                        val res = TradingStrategies.analyzeManualTradeRisk(
                                            symbol = symbol,
                                            direction = direction,
                                            entryPrice = entryP,
                                            currentSl = sl,
                                            currentTp = tp,
                                            positionSizeUsd = sizeUsd,
                                            candles = candles
                                        )
                                        if (res != null) {
                                            analysisResult = res
                                            portfolioList.removeAll { it.symbol == res.symbol }
                                            portfolioList.add(0, res)
                                        } else {
                                            errorMessage = "Could not compute risk metrics."
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Analysis error: ${e.message}"
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAnalyzing
                    ) {
                        Text(
                            text = if (isAnalyzing) "⏳ Analyzing Market Structure..." else "⚡ Deep Analyze Risk, Momentum & R:R",
                            color = BgDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Active Portfolio Summary
        if (portfolioList.isNotEmpty()) {
            item {
                Text(
                    text = "📋 ACTIVE MANUAL TRADES PORTFOLIO (${portfolioList.size})",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(portfolioList) { itemTrade ->
                val isSelected = analysisResult?.symbol == itemTrade.symbol
                Surface(
                    color = if (isSelected) SurfaceCard else SurfaceDark,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TerminalCyan else CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { analysisResult = itemTrade }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = itemTrade.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = itemTrade.direction, color = if (itemTrade.direction == "LONG") TerminalGreen else TerminalRed, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Text(text = "Entry: $${String.format("%.4f", itemTrade.entryPrice)} | SL: $${String.format("%.4f", itemTrade.suggestedSl)}", color = TextMuted, fontSize = 10.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val isRight = itemTrade.momentumSide == "RIGHT_SIDE"
                            Text(
                                text = if (isRight) "🟢 RIGHT SIDE" else "🔴 WRONG SIDE",
                                color = if (isRight) TerminalGreen else TerminalRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${String.format("%+.2f", itemTrade.currentPnlPct)}%",
                                color = if (itemTrade.currentPnlPct >= 0) TerminalGreen else TerminalRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Error Banner
        errorMessage?.let { err ->
            item {
                Surface(
                    color = TerminalRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $err",
                        color = TerminalRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Detailed Analysis Results
        analysisResult?.let { res ->
            // Mark Price & Live PnL Card
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = res.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(text = "${res.direction} Position • Size: $${String.format("%.2f", res.positionSizeUsd)}", color = TextMuted, fontSize = 11.sp)
                            }
                            val pnlColor = if (res.currentPnlPct >= 0) TerminalGreen else TerminalRed
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format("%+.2f", res.currentPnlPct)}%",
                                    color = pnlColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$${String.format("%+.4f", res.currentPnlUsd)} USDT",
                                    color = pnlColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Right Side vs Wrong Side Momentum Badge
                        val isRightSide = res.momentumSide == "RIGHT_SIDE"
                        Surface(
                            color = if (isRightSide) TerminalGreen.copy(alpha = 0.15f) else TerminalRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isRightSide) "🟢 ON THE RIGHT SIDE OF MARKET MOMENTUM" else "🔴 ON THE WRONG SIDE OF MARKET MOMENTUM",
                                    color = if (isRightSide) TerminalGreen else TerminalRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res.momentumExplanation,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                if (!isRightSide) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val counterDir = if (res.direction == "LONG") "SHORT" else "LONG"
                                    val counterSl = if (counterDir == "SHORT") res.currentPrice * 1.018 else res.currentPrice * 0.982
                                    val counterTp = if (counterDir == "SHORT") res.currentPrice * 0.94 else res.currentPrice * 1.06
                                    Surface(
                                        color = SurfaceDark,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "🔄 AI OPTIMAL RIGHT-SIDE COUNTER TRADE",
                                                color = TerminalCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Recommended: $counterDir @ $${String.format("%.4f", res.currentPrice)} | Corrected SL: $${String.format("%.4f", counterSl)} | Target TP (1:4.2 R:R): $${String.format("%.4f", counterTp)}",
                                                color = TextGold,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Action: Close/mitigate wrong-side position and reposition with prevailing institutional flow.",
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Position Risk & Profit Projections Card
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚖️ POSITION RISK EXPOSURE & PROFIT PROJECTIONS",
                            color = TextGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Defined Risk (SL):", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "-$${String.format("%.2f", res.riskExposureUsd)} (-${String.format("%.2f", res.riskExposurePct)}%)",
                                    color = TerminalRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Planned Profit (TP):", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "+$${String.format("%.2f", res.userPotentialProfitUsd)} (+${String.format("%.2f", res.userPotentialProfitPct)}%)",
                                    color = TerminalGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Calculated R:R Ratio:", color = TextSecondary, fontSize = 12.sp)
                            Text(text = res.userRrRatio, color = TerminalCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Strategy Confluence:", color = TextSecondary, fontSize = 12.sp)
                            Text(text = res.confluenceScore, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Confluence Breakdown Chips
                        if (res.strategyConfluence.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                res.strategyConfluence.forEach { (stratName, stratState) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stratName, color = TextMuted, fontSize = 10.sp)
                                        Text(text = stratState, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Universal R:R Spectrum Profit Projections (1:2 to 1:500 Mega-Move)
            if (res.rrProjections.isNotEmpty()) {
                item {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🚀 UNIVERSAL R:R PROFIT PROJECTIONS (1:2 TO 1:500)",
                                color = TerminalCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Theoretical profit achievable across R:R expansion tiers based on unit risk:",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(res.rrProjections) { proj ->
                                    Surface(
                                        color = SurfaceCard,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (proj.multiplier >= 100) TerminalPurple else (if (proj.multiplier >= 10) TerminalCyan else CardBorder)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "🎯 Tier ${proj.tier}",
                                                color = if (proj.multiplier >= 100) TerminalPurple else TerminalCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Target: $${String.format("%.4f", proj.targetPrice)}",
                                                color = TextPrimary,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Move: +${String.format("%.1f", proj.movePct)}%",
                                                color = TextGold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Profit: +$${String.format("%.2f", proj.profitUsd)}",
                                                color = TerminalGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recalculated Suggested SL & TP Card
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🎯 RECALCULATED SUGGESTED SL & TP (SMC / VOLATILITY)",
                            color = TerminalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Suggested Stop Loss (SL):", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.suggestedSl)}", color = TerminalRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Suggested TP1 (Liquidity):", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.suggestedTp1)}", color = TerminalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Suggested TP2 (Expansion):", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.suggestedTp2)}", color = TerminalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Mega TP (Moonshot):", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.suggestedTpMega)}", color = TerminalPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Loss-Minimization Exit Card
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🚪 OPTIMAL EXIT POINTS (LOSS MINIMIZATION)",
                            color = TextGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Relief Exit Target:", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.exitPrice)}", color = TextGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Hard Invalidation Cut:", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "$${String.format("%.4f", res.hardInvalidationCut)}", color = TerminalRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res.exitRecommendation,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Optimal Re-Entry Card
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔄 OPTIMAL RE-ENTRY ZONE",
                            color = TerminalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Target Entry Zone:", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "$${String.format("%.4f", res.reentryPriceMin)} - $${String.format("%.4f", res.reentryPriceMax)}",
                                color = TerminalCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Re-Entry SL / TP:", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "SL: $${String.format("%.4f", res.reentrySl)} | TP: $${String.format("%.4f", res.reentryTp)}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res.reentryRecommendation,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
