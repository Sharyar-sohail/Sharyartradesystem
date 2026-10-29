package com.example.binanceterminal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.binanceterminal.theme.*

/**
 * Integrated Market Modules for Sharyar Trading System
 * Supports 100% real-time Live Data Isolation across:
 * - 🚀 Crypto (Binance spot/futures, BTC/ETH/SOL)
 * - 💱 Forex (EUR/USD, GBP/USD, USD/JPY, DXY)
 * - 🪙 Gold & Metals (Spot Gold XAU/USD, Silver XAG/USD, Micro-Tight SL)
 * - 🇵🇰 Pakistan Stock Exchange (PSX KSE-100, KSE-30, Listed Equities in PKR)
 * - 🌐 All Modules Gateway
 * - 🎓 Trading Academy Masterclass
 */
enum class WebModule(
    val title: String,
    val shortName: String,
    val badge: String,
    val accentColor: Color,
    val url: String
) {
    ALL(
        title = "🌐 All Modules Hub",
        shortName = "All",
        badge = "GATEWAY",
        accentColor = TerminalCyan,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/"
    ),
    CRYPTO(
        title = "🚀 Crypto Engine",
        shortName = "Crypto",
        badge = "BINANCE SPOT/FUTURES",
        accentColor = TerminalCyan,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/?lane=crypto"
    ),
    FOREX(
        title = "💱 Forex Currency",
        shortName = "Forex",
        badge = "MAJOR FX PAIRS",
        accentColor = TerminalGreen,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/?lane=forex"
    ),
    GOLD(
        title = "🪙 Gold & Metals",
        shortName = "Gold (XAU)",
        badge = "SPOT METALS",
        accentColor = TerminalGold,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/?lane=gold"
    ),
    PSX(
        title = "🇵🇰 PSX Equities",
        shortName = "PSX Stocks",
        badge = "KSE-100 • PKR",
        accentColor = TerminalGreen,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/?lane=psx"
    ),
    ACADEMY(
        title = "🎓 Trading Academy",
        shortName = "Academy",
        badge = "MASTERCLASS",
        accentColor = TerminalPurple,
        url = "https://sharyartradesystem-mdzrbd7htyfxjkvvrsgm6k.streamlit.app/?academy=1"
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebTerminalScreen(
    initialModule: WebModule = WebModule.ALL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeModule by remember { mutableStateOf(initialModule) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Intercept hardware Android back button when inside web history
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top High-Density Module Switcher & Web Navigation Toolbar
        Surface(
            color = SurfaceDark,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Horizontal Module Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebModule.entries.forEach { mod ->
                        val isSelected = activeModule == mod
                        Surface(
                            color = if (isSelected) mod.accentColor.copy(alpha = 0.22f) else SurfaceCard,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) mod.accentColor else CardBorder
                            ),
                            modifier = Modifier
                                .clickable {
                                    if (activeModule != mod) {
                                        activeModule = mod
                                        hasError = false
                                        isLoading = true
                                        webViewRef?.loadUrl(mod.url)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = mod.title,
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Surface(
                                    color = mod.accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = mod.badge,
                                        color = mod.accentColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub-Toolbar with URL Telemetry and Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isLoading) TerminalGold else TerminalGreen,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = if (isLoading) "SYNCING: ${activeModule.badge} (${loadProgress}%)" else "LIVE: ${activeModule.badge}",
                            color = if (isLoading) TerminalGold else TerminalGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back Navigation
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) TerminalCyan else TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Forward Navigation
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) TerminalCyan else TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Reload Button
                        IconButton(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewRef?.reload()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = TerminalCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Open in External Browser
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeModule.url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "Open External",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Smooth Progress Bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = TerminalCyan,
                        trackColor = SurfaceCard
                    )
                }
            }
        }

        // Web Content Viewport & Error State Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        // Cookie acceptance
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // Enterprise WebSettings configuration for Streamlit SPAs
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = userAgentString + " Mobile SharyarTradingSystem/1.0"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val urlStr = request?.url?.toString() ?: ""
                                if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                                    return false
                                }
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, request?.url)
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    hasError = true
                                    errorMessage = error?.description?.toString() ?: "Connection to Trading Engine Failed"
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        webViewRef = this
                        loadUrl(activeModule.url)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )

            // High-Contrast Cyberpunk Connection Error Overlay
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalRed.copy(alpha = 0.5f)),
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = TerminalRed.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = TerminalRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "TRADING ENGINE OFFLINE",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = errorMessage.ifEmpty { "Unable to connect to Sharyar Institutional Cloud Service. Please verify your mobile internet connection." },
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = {
                                    hasError = false
                                    isLoading = true
                                    webViewRef?.loadUrl(activeModule.url)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = BgDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECONNECT TO ${activeModule.badge}",
                                    color = BgDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
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
