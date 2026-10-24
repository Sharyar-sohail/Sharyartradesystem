package com.example.binanceterminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.engine.BackgroundMarketScanner
import com.example.binanceterminal.engine.PaperTradingEngine
import com.example.binanceterminal.engine.SignalRepository
import com.example.binanceterminal.model.TradeSignal
import com.example.binanceterminal.theme.*
import kotlinx.coroutines.launch

@Composable
fun RadarScreen(
    onInspectChart: (TradeSignal) -> Unit
) {
    val signals by SignalRepository.signals.collectAsState()
    val stats by SignalRepository.stats.collectAsState()
    val isScanning by BackgroundMarketScanner.isScanning.collectAsState()
    val universeCount by BackgroundMarketScanner.universeSize.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedDir by remember { mutableStateOf("ALL") }
    var selectedHorizon by remember { mutableStateOf("ALL") }
    var selectedStatus by remember { mutableStateOf("ALL") }
    var activeModalSignal by remember { mutableStateOf<TradeSignal?>(null) }

    // Filter signals
    val filteredSignals = signals.filter { sig ->
        val dirMatch = (selectedDir == "ALL" || sig.direction == selectedDir)
        val horizonMatch = when (selectedHorizon) {
            "90%+ ACCURACY" -> sig.aiConfidencePct >= 90.0
            "50%-500% MEGA" -> sig.isMegaMove || kotlin.math.abs(sig.expectedMovePct) >= 50.0
            else -> true
        }
        val statusMatch = when (selectedStatus) {
            "ACTIVE" -> sig.status == "ACTIVE"
            "RESOLVED" -> sig.status != "ACTIVE"
            else -> true
        }
        dirMatch && horizonMatch && statusMatch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚡ SHARYAR TRADING RADAR",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "Sharyar Trading System • ${universeCount} Pairs",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Surface(
                color = if (isScanning) TerminalGreen.copy(alpha = 0.15f) else TerminalGold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isScanning) TerminalGreen else TerminalGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "SCANNING" else "PAUSED",
                        color = if (isScanning) TerminalGreen else TerminalGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                title = "ACCURACY",
                value = "${stats.liveAccuracyPct}%",
                sub = "${stats.hit10pct} Hits / ${stats.stoppedOut} SL",
                color = TerminalGreen,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "ACTIVE",
                value = "${stats.activeSignals}",
                sub = "${stats.highAccuracyCount} High Conf",
                color = TerminalCyan,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "MEGA 50%+",
                value = "${stats.megaHits}",
                sub = "Explosions",
                color = TerminalPurple,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "AVG R:R",
                value = "1:${stats.avgRiskReward}",
                sub = "Max +${stats.avgMaxGainPct}%",
                color = TerminalGold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChipItem(label = "All Dirs", isSelected = selectedDir == "ALL") { selectedDir = "ALL" }
            }
            item {
                FilterChipItem(label = "🟢 Longs", isSelected = selectedDir == "LONG") { selectedDir = "LONG" }
            }
            item {
                FilterChipItem(label = "🔴 Shorts", isSelected = selectedDir == "SHORT") { selectedDir = "SHORT" }
            }
            item {
                FilterChipItem(label = "🏆 90%+ Win", isSelected = selectedHorizon == "90%+ ACCURACY") {
                    selectedHorizon = if (selectedHorizon == "90%+ ACCURACY") "ALL" else "90%+ ACCURACY"
                }
            }
            item {
                FilterChipItem(label = "💥 50%+ Mega", isSelected = selectedHorizon == "50%-500% MEGA") {
                    selectedHorizon = if (selectedHorizon == "50%-500% MEGA") "ALL" else "50%-500% MEGA"
                }
            }
            item {
                FilterChipItem(label = "Active Only", isSelected = selectedStatus == "ACTIVE") {
                    selectedStatus = if (selectedStatus == "ACTIVE") "ALL" else "ACTIVE"
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Signal Cards Feed
        if (filteredSignals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📡 Scanning Global Markets Locally...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time algorithmic stream. Signals will appear here in real-time.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch { BackgroundMarketScanner.triggerInstantSweep(35) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "🚀 Trigger Immediate Sweep", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSignals, key = { it.id }) { sig ->
                    SignalCard(
                        signal = sig,
                        onClick = { activeModalSignal = sig },
                        onChartClick = { onInspectChart(sig) }
                    )
                }
            }
        }
    }

    // Modal Sheet
    activeModalSignal?.let { sig ->
        SignalDetailModal(
            signal = sig,
            onDismiss = { activeModalSignal = null },
            onInspectChart = { onInspectChart(it) }
        )
    }
}

@Composable
private fun SignalCard(
    signal: TradeSignal,
    onClick: () -> Unit,
    onChartClick: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Symbol, Direction Badge, Status & Mark Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = signal.symbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (signal.direction == "LONG") TerminalGreen.copy(alpha = 0.2f) else TerminalRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = signal.direction,
                            color = if (signal.direction == "LONG") TerminalGreen else TerminalRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Live Mark & Current PnL displayed cleanly in Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${formatPrice(signal.currentPrice)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${String.format("%+.1f", signal.livePnlPct)}%)",
                        color = if (signal.livePnlPct >= 0) TerminalGreen else TerminalRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = signal.status)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Strategy Name
            Text(
                text = signal.strategyName,
                color = if (signal.isMegaMove) TerminalPurple else TerminalCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trade Metrics Grid: Entry, Hard SL, TP2, Mega TP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "ENTRY", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$${formatPrice(signal.entryPrice)}", color = TextGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text(text = "HARD SL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$${formatPrice(signal.stopLoss)}", color = TerminalRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text(text = "TP2 (20%)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$${formatPrice(signal.target20pct)}", color = TerminalGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                if (signal.targetMega > 0) {
                    Column {
                        Text(text = "MEGA TP", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$${formatPrice(signal.targetMega)}", color = TerminalPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges Row: Dedicated row with horizontal scroll to prevent any text wrapping
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgePill(text = "Vol: ${String.format("%.1f", if (signal.volSurge > 0) signal.volSurge else 2.4)}x", color = TerminalGreen)
                BadgePill(text = "Squeeze: ${String.format("%.1f", signal.bandwidth * 100)}%", color = TerminalCyan)
                BadgePill(text = "R:R 1:${signal.riskRewardRatio}", color = TerminalGold)
                BadgePill(text = "${signal.aiConfidencePct}% Win Rate", color = TerminalGreen)
                if (signal.isMegaMove) {
                    BadgePill(text = "💥 50%+ MEGA", color = TerminalPurple)
                }
                if (signal.rrHitTier != "NONE") {
                    BadgePill(text = "🎯 ${signal.rrHitTier}", color = TerminalGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Trade & Chart Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChartClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("📊 Chart", fontSize = 11.sp, color = TerminalGreen)
                }

                Button(
                    onClick = {
                        PaperTradingEngine.openManualTrade(signal)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBinance),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("⚡ Demo Trade", fontSize = 11.sp, color = BgDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = sub,
                color = TextSecondary,
                fontSize = 7.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) TerminalCyan.copy(alpha = 0.2f) else SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) TerminalCyan else CardBorder
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) TerminalCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (col, bg) = when (status) {
        "ACTIVE" -> TerminalGreen to TerminalGreen.copy(alpha = 0.15f)
        "HIT_10PCT", "HIT_20PCT", "HIT_MEGA" -> GoldBinance to GoldBinance.copy(alpha = 0.15f)
        "STOPPED_OUT" -> TerminalRed to TerminalRed.copy(alpha = 0.15f)
        else -> TextSecondary to SurfaceCard
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = status,
            color = col,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun formatPrice(p: Double): String {
    return if (p < 0.001) String.format("%.6f", p)
    else if (p < 1.0) String.format("%.4f", p)
    else String.format("%.2f", p)
}
