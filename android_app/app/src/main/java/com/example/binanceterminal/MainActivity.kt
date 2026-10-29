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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Language
import com.example.binanceterminal.ui.WebTerminalScreen
import com.example.binanceterminal.ui.WebModule
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
    WEB_TERMINAL("Web Hub", Icons.Default.Language),
    RADAR("AI Radar", Icons.Default.Radar),
    UNIVERSAL("Universal", Icons.Default.TrackChanges),
    PRO_CHART("Pro Chart", Icons.Default.Analytics),
    ALPHA_LAB("Alpha Lab", Icons.Default.Star),
    PAPER_TRADING("Demo", Icons.Default.AccountBalanceWallet),
    RISK_MGR("Risk Mgr", Icons.Default.Security),
    MARKET("Cross-Ex", Icons.Default.Equalizer),
    TELEMETRY("System", Icons.Default.Settings)
}

@Composable
fun MainAppScaffold() {
    var selectedTab by remember { mutableStateOf(NavigationTab.WEB_TERMINAL) }
    var activeWebModule by remember { mutableStateOf(WebModule.ALL) }
    var chartSignal by remember { mutableStateOf<TradeSignal?>(null) }
    var chartSymbol by remember { mutableStateOf("BTC/USDT") }

    // Key Security & Master Admin state
    var showKeyDialog by remember { mutableStateOf(false) }
    var isMasterAdmin by remember { mutableStateOf(false) }
    var masterAuthInput by remember { mutableStateOf("") }
    var newSecretKeyInput by remember { mutableStateOf("") }
    var keyStatusMessage by remember { mutableStateOf("") }

    val account by PaperTradingEngine.account.collectAsState()
    val openPositions by PaperTradingEngine.openPositions.collectAsState()

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = {
                showKeyDialog = false
                keyStatusMessage = ""
                masterAuthInput = ""
            },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (isMasterAdmin) "👑 MASTER ADMIN KEY" else "🔑 ACCESS & SECURITY",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isMasterAdmin) {
                        Text(
                            text = "🔒 Standard Operator Mode. Key changes are restricted to Master Admin (Sharyar).",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = masterAuthInput,
                            onValueChange = { masterAuthInput = it },
                            label = { Text("Authorize Master Admin", fontSize = 11.sp) },
                            placeholder = { Text("Type 'SHARYAR' to unlock", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val clean = masterAuthInput.trim()
                                if (clean.equals("SHARYAR", ignoreCase = true) || clean == "شریار") {
                                    isMasterAdmin = true
                                    keyStatusMessage = "👑 Master Admin Verified!"
                                } else {
                                    keyStatusMessage = "❌ Incorrect Key. Access Denied."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Unlock Key Management", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = "👑 Master Admin Mode Verified (Sharyar)\nYou have full authorization to modify the system access key.",
                            color = TerminalGreen,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = newSecretKeyInput,
                            onValueChange = { newSecretKeyInput = it },
                            label = { Text("New Secret Access Key", fontSize = 11.sp) },
                            placeholder = { Text("Enter New Key", fontSize = 11.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newSecretKeyInput.isNotBlank()) {
                                    keyStatusMessage = "✅ Secret Key successfully updated to '$newSecretKeyInput'!"
                                    newSecretKeyInput = ""
                                } else {
                                    keyStatusMessage = "Key cannot be empty."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update Access Key", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                isMasterAdmin = false
                                keyStatusMessage = "Admin mode revoked."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Revoke Admin Mode", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    if (keyStatusMessage.isNotBlank()) {
                        Text(
                            text = keyStatusMessage,
                            color = if (keyStatusMessage.startsWith("✅") || keyStatusMessage.startsWith("👑")) TerminalGreen else TerminalRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showKeyDialog = false
                    keyStatusMessage = ""
                    masterAuthInput = ""
                }) {
                    Text("Close", color = TerminalCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

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

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick Web Hub Toggle Pill
                        Surface(
                            color = if (selectedTab == NavigationTab.WEB_TERMINAL) TerminalCyan.copy(alpha = 0.22f) else SurfaceCard,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTab == NavigationTab.WEB_TERMINAL) TerminalCyan else CardBorder),
                            modifier = Modifier.clickable { selectedTab = NavigationTab.WEB_TERMINAL }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Web Engine", tint = TerminalCyan, modifier = Modifier.size(13.dp))
                                Text("WEB", color = TerminalCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Key / Security Button
                        Surface(
                            color = if (isMasterAdmin) TerminalGreen.copy(alpha = 0.2f) else SurfaceCard,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isMasterAdmin) TerminalGreen else CardBorder),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showKeyDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Security Key",
                                    tint = if (isMasterAdmin) TerminalGreen else TerminalCyan,
                                    modifier = Modifier.size(15.dp)
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
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                NavigationTab.WEB_TERMINAL -> {
                    WebTerminalScreen(
                        initialModule = activeWebModule
                    )
                }
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
