package com.example.binanceterminal.engine

import com.example.binanceterminal.api.BinanceApiClient
import com.example.binanceterminal.strategy.TradingStrategies
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Autonomous Background Market Scanner for Mobile.
 * Runs independently on the device with zero dependency on the PC.
 * Connects directly to Binance Public APIs and executes strategy evaluation.
 * Includes Wi-Fi PC Companion Offload & Battery Protection Mode.
 */
object BackgroundMarketScanner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCycles = MutableStateFlow(0)
    val scannedCycles: StateFlow<Int> = _scannedCycles.asStateFlow()

    private val _universeSize = MutableStateFlow(0)
    val universeSize: StateFlow<Int> = _universeSize.asStateFlow()

    private val _lastScanTime = MutableStateFlow("Pending")
    val lastScanTime: StateFlow<String> = _lastScanTime.asStateFlow()

    private val _lastDetectedSignalSummary = MutableStateFlow<String?>(null)
    val lastDetectedSignalSummary: StateFlow<String?> = _lastDetectedSignalSummary.asStateFlow()

    private val _isPcSynced = MutableStateFlow(false)
    val isPcSynced: StateFlow<Boolean> = _isPcSynced.asStateFlow()

    private val _batteryProtectionMode = MutableStateFlow("Adaptive Standalone")
    val batteryProtectionMode: StateFlow<String> = _batteryProtectionMode.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val pcBackendUrls = listOf(
        "http://192.168.1.199:8000",
        "http://10.0.2.2:8000",
        "http://localhost:8000"
    )

    private var marketUniverse: List<String> = listOf(
        "BTC/USDT", "ETH/USDT", "SOL/USDT", "BNB/USDT", "XRP/USDT",
        "DOGE/USDT", "ADA/USDT", "AVAX/USDT", "LINK/USDT", "SUI/USDT"
    )
    private var currentScanIdx = 0
    private val batchSize = 4
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun start() {
        if (_isScanning.value) return
        _isScanning.value = true

        scanJob = scope.launch {
            refreshUniverse()

            var universeTimer = System.currentTimeMillis()

            while (isActive) {
                try {
                    // 1. Check Wi-Fi PC Backend Offload for Battery Protection
                    val pcOnline = trySyncWithPcBackend()
                    if (pcOnline) {
                        _isPcSynced.value = true
                        _batteryProtectionMode.value = "⚡ Wi-Fi Offloaded (0% Extra Mobile Battery)"
                        _lastScanTime.value = timeFormat.format(Date()) + " (PC Synchronized)"
                        delay(4000) // Sleep peacefully while PC handles 24/7 scanning
                        continue
                    } else {
                        _isPcSynced.value = false
                        _batteryProtectionMode.value = "🔋 Adaptive Throttling (Conserve Mobile Battery)"
                    }

                    // Refresh market universe every 15 minutes
                    if (System.currentTimeMillis() - universeTimer > 15 * 60 * 1000) {
                        refreshUniverse()
                        universeTimer = System.currentTimeMillis()
                    }

                    if (marketUniverse.isEmpty()) {
                        delay(3000)
                        continue
                    }

                    _lastScanTime.value = timeFormat.format(Date())

                    // Scan next batch
                    val start = currentScanIdx
                    val end = minOf(start + batchSize, marketUniverse.size)
                    val batch = marketUniverse.subList(start, end)
                    currentScanIdx = if (end < marketUniverse.size) end else 0

                    for (symbol in batch) {
                        scanSymbol(symbol)
                        _scannedCycles.value = _scannedCycles.value + 1
                        delay(120) // Rate limit safety
                    }

                    // Verify active signals accuracy
                    verifyActiveSignals()

                } catch (_: Exception) {
                    // Resilient backoff
                }
                delay(3000)
            }
        }
    }

    fun pause() {
        _isScanning.value = false
        scanJob?.cancel()
        scanJob = null
    }

    suspend fun triggerInstantSweep(count: Int = 30) = withContext(Dispatchers.IO) {
        refreshUniverse()
        val targets = marketUniverse.take(count)
        for (symbol in targets) {
            scanSymbol(symbol)
            delay(80)
        }
        verifyActiveSignals()
    }

    private suspend fun trySyncWithPcBackend(): Boolean = withContext(Dispatchers.IO) {
        for (base in pcBackendUrls) {
            try {
                val req = Request.Builder().url("$base/api/health").build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        syncSignalsFromPc(base)
                        return@withContext true
                    }
                }
            } catch (_: Exception) {
            }
        }
        false
    }

    private fun syncSignalsFromPc(base: String) {
        try {
            val req = Request.Builder().url("$base/api/signals?limit=60").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return
                    val root = JSONObject(body)
                    val arr = root.optJSONArray("signals") ?: return
                    val syncedSignals = mutableListOf<com.example.binanceterminal.model.TradeSignal>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        syncedSignals.add(
                            com.example.binanceterminal.model.TradeSignal(
                                id = obj.optLong("id", (i + 1).toLong()),
                                timestamp = obj.optString("timestamp", ""),
                                symbol = obj.optString("symbol", ""),
                                direction = obj.optString("direction", "LONG"),
                                strategyName = obj.optString("strategy_name", "Bollinger Squeeze"),
                                entryPrice = obj.optDouble("entry_price", 0.0),
                                currentPrice = obj.optDouble("current_price", 0.0),
                                stopLoss = obj.optDouble("stop_loss", 0.0),
                                target10pct = obj.optDouble("target_10pct", 0.0),
                                target20pct = obj.optDouble("target_20pct", 0.0),
                                targetMega = obj.optDouble("target_mega", 0.0),
                                riskRewardRatio = obj.optDouble("risk_reward_ratio", 3.0),
                                expectedMovePct = obj.optDouble("expected_move_pct", 20.0),
                                aiConfidencePct = obj.optDouble("ai_confidence_pct", 90.0),
                                isMegaMove = obj.optInt("is_mega_move", 0) == 1,
                                maxFavorablePct = obj.optDouble("max_favorable_pct", 0.0),
                                status = obj.optString("status", "ACTIVE"),
                                bandwidth = obj.optDouble("bandwidth", 0.04),
                                volSurge = obj.optDouble("vol_surge", 2.0),
                                timeframe = obj.optString("timeframe", "15m"),
                                signalReason = obj.optString("signal_reason", ""),
                                updatedAt = obj.optString("updated_at", ""),
                                targetRr2 = obj.optDouble("target_rr_2", 0.0),
                                targetRr5 = obj.optDouble("target_rr_5", 0.0),
                                targetRr10 = obj.optDouble("target_rr_10", 0.0),
                                targetRr50 = obj.optDouble("target_rr_50", 0.0),
                                targetRr100 = obj.optDouble("target_rr_100", 0.0),
                                targetRr500 = obj.optDouble("target_rr_500", 0.0),
                                rrHitTier = obj.optString("rr_hit_tier", "NONE")
                            )
                        )
                    }
                    if (syncedSignals.isNotEmpty()) {
                        SignalRepository.syncSignalsFromPc(syncedSignals)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun refreshUniverse() {
        try {
            val tickers = BinanceApiClient.fetch24HrTickers()
            if (tickers.isNotEmpty()) {
                val sortedByVol = tickers.sortedByDescending { it.quoteVolume }
                val sortedByGainers = tickers.sortedByDescending { it.priceChangePercent }
                val sortedByLosers = tickers.sortedBy { it.priceChangePercent }

                val universe = mutableListOf<String>()
                val seen = mutableSetOf<String>()

                fun addSym(sym: String) {
                    if (seen.add(sym)) universe.add(sym)
                }

                // Prioritize top 25 gainers, top 25 losers, top 50 high volume
                sortedByGainers.take(25).forEach { addSym(it.symbol) }
                sortedByLosers.take(25).forEach { addSym(it.symbol) }
                sortedByVol.take(50).forEach { addSym(it.symbol) }
                sortedByVol.drop(50).take(60).forEach { addSym(it.symbol) }

                marketUniverse = universe
                _universeSize.value = universe.size
            }
        } catch (_: Exception) {
            // Keep existing universe
        }
    }

    private suspend fun scanSymbol(symbol: String) {
        try {
            val candles = BinanceApiClient.fetchOhlcv(symbol, timeframe = "15m", limit = 75)
            if (candles.size < 30) return

            val setup = TradingStrategies.evaluateMarketWideSetup(symbol, candles) ?: return
            val newSig = SignalRepository.addSignalFromSetup(symbol, setup)

            if (newSig != null) {
                _lastDetectedSignalSummary.value = "${newSig.direction} ${newSig.symbol} (${newSig.strategyName})"
                PaperTradingEngine.processSignalForAutoTrade(newSig)
            }
        } catch (_: Exception) {
            // Ignored
        }
    }

    private suspend fun verifyActiveSignals() {
        val activeSignals = SignalRepository.signals.value.filter { it.status == "ACTIVE" }
        for (sig in activeSignals) {
            val currentPrice = BinanceApiClient.getTickerPrice(sig.symbol) ?: continue
            SignalRepository.updateSignalPrice(sig.id, currentPrice)
            PaperTradingEngine.updateSymbolPrice(sig.symbol, currentPrice)
        }

        // Also check any open paper trading positions not in active signals
        val openPositions = PaperTradingEngine.openPositions.value
        for (pos in openPositions) {
            if (activeSignals.none { it.symbol == pos.symbol }) {
                val price = BinanceApiClient.getTickerPrice(pos.symbol) ?: continue
                PaperTradingEngine.updateSymbolPrice(pos.symbol, price)
            }
        }
    }
}
