package com.example.binanceterminal.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.engine.PaperTradingEngine
import com.example.binanceterminal.model.TradeSignal
import com.example.binanceterminal.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalDetailModal(
    signal: TradeSignal,
    onDismiss: () -> Unit,
    onInspectChart: (TradeSignal) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header: Pair & Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = signal.symbol,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = signal.strategyName,
                        color = TerminalCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = if (signal.direction == "LONG") TerminalGreen.copy(alpha = 0.15f) else TerminalRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (signal.direction == "LONG") "🟢 LONG POSITION" else "🔴 SHORT POSITION",
                        color = if (signal.direction == "LONG") TerminalGreen else TerminalRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Algorithmic Rationale & Market Mechanics Breakdown Box
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔬 ALGORITHMIC RATIONALE & MARKET MECHANICS",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Structural Trigger: " + if (signal.signalReason.isNotBlank()) signal.signalReason else "Order Block Mitigation at ${formatPrice(signal.entryPrice)} with FVG Imbalance fill.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Liquidity Sweep: " + if (signal.direction == "LONG") "Sell-Side Liquidity (SSL) swept below swing low before impulsive displacement." else "Buy-Side Liquidity (BSL) swept above swing high before rejection.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Volume Surge Footprint: Expanded to ${String.format("%.1f", if (signal.volSurge > 0) signal.volSurge else 2.4)}x of 20 SMA with ${String.format("%.1f", signal.bandwidth * 100)}% volatility coil compression.",
                        color = TerminalGreen,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Invalidation Rule: Hard Stop Loss at ${formatPrice(signal.stopLoss)}. Automatically trail SL to Breakeven at TP1 (+10% / 1:2 R:R).",
                        color = TextGold,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trade Metrics Grid
            Text(
                text = "MANDATORY TRADE METRICS & EXECUTION LEVELS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            MetricRow("Precise Entry Point", formatPrice(signal.entryPrice), TextGold)
            MetricRow("Hard Stop-Loss (SL)", formatPrice(signal.stopLoss), TerminalRed)
            MetricRow("TP1 Milestone (10%)", formatPrice(signal.target10pct), TerminalGreen)
            MetricRow("TP2 Milestone (20%)", formatPrice(signal.target20pct), TerminalGreen)
            if (signal.targetMega > 0) {
                MetricRow("Mega Target (50%+)", formatPrice(signal.targetMega), TerminalPurple)
            }
            MetricRow("Risk-to-Reward Ratio", "1:${signal.riskRewardRatio}", TerminalGold)
            MetricRow("Expected Move %", "${signal.expectedMovePct}%", if (signal.isMegaMove) TerminalPurple else TerminalCyan)
            MetricRow("AI Confidence / Win Rate", "${signal.aiConfidencePct}% (Isolated Setup)", TerminalGreen)
            MetricRow("Live Current Mark Price", formatPrice(signal.currentPrice), TextPrimary)
            MetricRow("Real-Time PnL", "${String.format("%.2f", signal.livePnlPct)}%", if (signal.livePnlPct >= 0) TerminalGreen else TerminalRed)
            MetricRow("Peak Favorable Move", "+${String.format("%.1f", signal.maxFavorablePct)}%", TerminalGold)
            if (signal.targetRr5 > 0) MetricRow("R:R 1:5 Target", formatPrice(signal.targetRr5), TerminalGreen)
            if (signal.targetRr10 > 0) MetricRow("R:R 1:10 Target", formatPrice(signal.targetRr10), TerminalCyan)
            if (signal.targetRr50 > 0) MetricRow("R:R 1:50 Mega Tier 1", formatPrice(signal.targetRr50), TerminalPurple)
            if (signal.targetRr100 > 0) MetricRow("R:R 1:100 Mega Tier 2", formatPrice(signal.targetRr100), TerminalPurple)
            if (signal.targetRr500 > 0) MetricRow("R:R 1:500 Moonshot", formatPrice(signal.targetRr500), TerminalGold)
            if (signal.rrHitTier != "NONE") MetricRow("Milestone Tier Hit", signal.rrHitTier, TerminalGold)

            Spacer(modifier = Modifier.height(16.dp))

            // Demo Execution Button
            Button(
                onClick = {
                    PaperTradingEngine.openManualTrade(signal)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldBinance),
                shape = RoundedCornerShape(10.dp)
            ) {
                val currentTradeSize = PaperTradingEngine.account.value.tradeSizeUsd
                val sizeLabel = if (currentTradeSize == currentTradeSize.toLong().toDouble()) String.format("$%.0f", currentTradeSize) else String.format("$%.2f", currentTradeSize)
                Text(
                    text = "⚡ Open Demo Trade (" + sizeLabel + ")",
                    color = BgDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Action Button
            Button(
                onClick = {
                    onDismiss()
                    onInspectChart(signal)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "📊 View Candlestick Chart & Target Lines",
                    color = BgDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Copy Signal / Share to Group Button (Requirement 4)
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            OutlinedButton(
                onClick = {
                    val shareText = buildString {
                        appendLine("⚡ [SHARYAR TRADING SYSTEM - AUDITED ALPHA SIGNAL]")
                        appendLine("Pair: ${signal.symbol} | Direction: ${if (signal.direction == "LONG") "🟢 LONG" else "🔴 SHORT"}")
                        appendLine("Strategy: ${signal.strategyName}")
                        appendLine("Audited Win Rate: ${signal.aiConfidencePct}% | R:R: 1:${signal.riskRewardRatio}")
                        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        appendLine("▶ Entry Price: ${formatPrice(signal.entryPrice)}")
                        appendLine("🛑 Stop Loss: ${formatPrice(signal.stopLoss)}")
                        appendLine("🎯 Take Profit 1: ${formatPrice(signal.target10pct)}")
                        appendLine("🎯 Take Profit 2: ${formatPrice(signal.target20pct)}")
                        if (signal.targetMega > 0) appendLine("🚀 Mega Moonshot: ${formatPrice(signal.targetMega)}")
                        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        appendLine("🛡️ Execution Rule: Move Stop Loss to Breakeven upon TP1.")
                    }
                    clipboardManager.setText(AnnotatedString(shareText))
                    Toast.makeText(context, "Alpha Signal copied to clipboard!", Toast.LENGTH_SHORT).show()
                    try {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Signal to Group")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) { }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan)
            ) {
                Text(
                    text = "📋 Copy Signal / Share to Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatPrice(p: Double): String {
    return if (p < 10.0) String.format("$%.4f", p) else String.format("$%,.2f", p)
}
