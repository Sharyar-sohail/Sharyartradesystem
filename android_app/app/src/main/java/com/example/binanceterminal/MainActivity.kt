package com.example.binanceterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binanceterminal.engine.BackgroundMarketScanner
import com.example.binanceterminal.engine.PaperTradingEngine
import com.example.binanceterminal.model.TradeSignal
import com.example.binanceterminal.theme.*
import com.example.binanceterminal.ui.AlphaLabScreen
import com.example.binanceterminal.ui.MarketScreen
import com.example.binanceterminal.ui.PaperTradingScreen
import com.example.binanceterminal.ui.ProChartScreen
import com.example.binanceterminal.ui.RadarScreen
import com.example.binanceterminal.ui.ManualTradeRiskScreen
import com.example.binanceterminal.ui.TelemetryScreen
import com.example.binanceterminal.ui.UniversalEngineScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically start standalone mobile background scanner
        BackgroundMarketScanner.start()

        setContent {
            BinanceTerminalTheme {
                MainAppScaffold()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not kill scanner if user wants background continuation
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    RADAR("AI Radar", Icons.Default.Radar),
    UNIVERSAL("Universal", Icons.Default.TrackChanges),
    PRO_CHART("Bubbles", Icons.Default.Analytics),
    ALPHA_LAB("Alpha Lab", Icons.Default.Star),
    PAPER_TRADING("Demo", Icons.Default.AccountBalanceWallet),
    RISK_MGR("Risk Mgr", Icons.Default.Security),
    MARKET("Cross-Ex", Icons.Default.Equalizer),
    TELEMETRY("System", Icons.Default.Settings)
}

@Composable
fun MainAppScaffold() {
    var selectedTab by remember { mutableStateOf(NavigationTab.RADAR) }
    var chartSignal by remember { mutableStateOf<TradeSignal?>(null) }
    var chartSymbol by remember { mutableStateOf("BTC/USDT") }

    val account by PaperTradingEngine.account.collectAsState()
    val openPositions by PaperTradingEngine.openPositions.collectAsState()

    Scaffold(
        topBar = {
            // Global Persistent Auto-Trading Command Bar with Sharyar Trading System Branding
            Surface(
                color = SurfaceDark,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (account.autoTradeEnabled) TerminalGreen.copy(alpha = 0.2f) else SurfaceCard,
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (account.autoTradeEnabled) TerminalGreen else TextMuted
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = "Auto Trade Daemon",
                                    tint = if (account.autoTradeEnabled) TerminalGreen else TextMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "SHARYAR TRADING SYSTEM",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = if (account.autoTradeEnabled) "• AUTO ON" else "• PAUSED",
                                    color = if (account.autoTradeEnabled) TerminalGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = if (account.autoTradeEnabled)
                                    "Slots: ${openPositions.size}/10 • \$${account.tradeSizeUsd.toInt()}/trade • Bal: \$${String.format("%.0f", account.balanceUsd)}"
                                else
                                    "Autonomous AI Multi-Strategy & Mega-Move Engine",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Master Auto-Trade Toggle Switch
                    Switch(
                        checked = account.autoTradeEnabled,
                        onCheckedChange = { isEnabled ->
                            PaperTradingEngine.setAutoTradeSettings(enabled = isEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TerminalGreen,
                            checkedTrackColor = TerminalGreen.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceCard
                        )
                    )
                }
            }
        },
        bottomBar = {
            // Floating macOS-Style Dock Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = SurfaceCard.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan.copy(alpha = 0.28f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                color = if (isSelected) TerminalCyan.copy(alpha = 0.18f) else Color.Transparent,
                                shape = RoundedCornerShape(18.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TerminalCyan.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.clickable { selectedTab = tab }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) TerminalCyan else TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        fontSize = 8.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TerminalCyan else TextMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark)
        ) {
            when (selectedTab) {
                NavigationTab.RADAR -> {
                    RadarScreen(
                        onInspectChart = { sig ->
                            chartSignal = sig
                            chartSymbol = sig.symbol
                            selectedTab = NavigationTab.PRO_CHART
                        }
                    )
                }
                NavigationTab.UNIVERSAL -> {
                    UniversalEngineScreen(
                        onInspectChart = { sym ->
                            chartSymbol = sym
                            selectedTab = NavigationTab.PRO_CHART
                        }
                    )
                }
                NavigationTab.PRO_CHART -> {
                    ProChartScreen(
                        initialSignal = chartSignal,
                        initialSymbol = chartSymbol
                    )
                }
                NavigationTab.ALPHA_LAB -> {
                    AlphaLabScreen(
                        onInspectChart = { sym ->
                            chartSymbol = sym
                            selectedTab = NavigationTab.PRO_CHART
                        }
                    )
                }
                NavigationTab.PAPER_TRADING -> {
                    PaperTradingScreen(
                        onInspectChart = { sym ->
                            chartSymbol = sym
                            selectedTab = NavigationTab.PRO_CHART
                        }
                    )
                }
                NavigationTab.RISK_MGR -> {
                    ManualTradeRiskScreen()
                }
                NavigationTab.MARKET -> {
                    MarketScreen(
                        onSelectPair = { sym ->
                            chartSymbol = sym
                            selectedTab = NavigationTab.PRO_CHART
                        }
                    )
                }
                NavigationTab.TELEMETRY -> {
                    TelemetryScreen()
                }
            }
        }
    }
}
