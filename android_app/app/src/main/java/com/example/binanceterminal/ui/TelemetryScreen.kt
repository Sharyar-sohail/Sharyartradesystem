package com.example.binanceterminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.api.BinanceApiClient
import com.example.binanceterminal.engine.BackgroundMarketScanner
import com.example.binanceterminal.engine.SignalRepository
import com.example.binanceterminal.theme.*
import kotlinx.coroutines.launch

@Composable
fun TelemetryScreen() {
    val isScanning by BackgroundMarketScanner.isScanning.collectAsState()
    val cycles by BackgroundMarketScanner.scannedCycles.collectAsState()
    val universeCount by BackgroundMarketScanner.universeSize.collectAsState()
    val lastScan by BackgroundMarketScanner.lastScanTime.collectAsState()
    val stats by SignalRepository.stats.collectAsState()
    val isPcSynced by BackgroundMarketScanner.isPcSynced.collectAsState()
    val batteryMode by BackgroundMarketScanner.batteryProtectionMode.collectAsState()
    val scope = rememberCoroutineScope()

    var pingLatency by remember { mutableStateOf<Long?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (ok, lat) = BinanceApiClient.testConnection()
        if (ok) pingLatency = lat
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "🖥️ SYSTEM TELEMETRY & CONTROLLER",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Sharyar Trading System • Standalone Mobile Unit",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Binance Direct Connection Card
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "BINANCE DIRECT API HEALTH", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🟢", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pingLatency != null) "Connected (${pingLatency} ms)" else "Testing...",
                            color = TerminalGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isPinging = true
                                val (ok, lat) = BinanceApiClient.testConnection()
                                if (ok) pingLatency = lat
                                isPinging = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (isPinging) "Pinging..." else "Ping", color = TerminalCyan, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Desktop PC Sync & Battery Protection Card
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "DESKTOP PC SYNC & BATTERY PROTECTION", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPcSynced) "🟢 PC Companion Mode (Connected)" else "🔋 Standalone Native Engine",
                            color = if (isPcSynced) TerminalGreen else TerminalCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = batteryMode,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isPcSynced) TerminalGreen.copy(alpha = 0.15f) else TerminalGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isPcSynced) "WI-FI SYNC" else "STANDALONE",
                            color = if (isPcSynced) TerminalGreen else TextGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scanner Daemon Controller
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LOCAL BACKGROUND SCANNER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isScanning) "Active Daemon Running" else "Scanner Paused",
                            color = if (isScanning) TerminalGreen else TerminalGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Universe: $universeCount pairs | Cycles: $cycles",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Last batch scan: $lastScan",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isScanning) {
                                BackgroundMarketScanner.pause()
                            } else {
                                BackgroundMarketScanner.start()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) TerminalRed.copy(alpha = 0.2f) else TerminalGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isScanning) "Pause" else "Start",
                            color = if (isScanning) TerminalRed else BgDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch { BackgroundMarketScanner.triggerInstantSweep(35) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "🚀 Trigger Immediate Multi-Pair Sweep", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Local Storage & Stats Card
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LOCAL STORAGE & PERFORMANCE STATS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                StatLine("Total Signals Logged", "${stats.totalSignals}")
                StatLine("Active Signal Setups", "${stats.activeSignals}")
                StatLine("10% Targets Hit", "${stats.hit10pct}")
                StatLine("20% Targets Hit", "${stats.hit20pct}")
                StatLine("50%+ Mega Explosions", "${stats.megaHits}")
                StatLine("Stopped Out", "${stats.stoppedOut}")
                StatLine("Strategy Accuracy Rate", "${stats.liveAccuracyPct}%")
                StatLine("Average Risk:Reward Ratio", "1:${stats.avgRiskReward}")

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { SignalRepository.clearAll() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalRed)
                ) {
                    Text(text = "Clear Signal Cache", fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}
