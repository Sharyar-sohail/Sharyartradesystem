package com.example.binanceterminal.api

import com.example.binanceterminal.model.Candle
import com.example.binanceterminal.model.MarketTicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Direct Binance API Connector for Standalone Mobile Execution.
 * Connects directly to Binance Public REST API with automatic multi-mirror failover.
 * Zero dependency on PC / local server.
 */
object BinanceApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val endpoints = listOf(
        "https://api.binance.com",
        "https://data-api.binance.vision",
        "https://api1.binance.com",
        "https://api2.binance.com",
        "https://api3.binance.com"
    )

    private var activeEndpoint = endpoints[0]

    suspend fun testConnection(): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        for (ep in endpoints) {
            try {
                val req = Request.Builder()
                    .url("$ep/api/v3/ping")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        activeEndpoint = ep
                        val latency = System.currentTimeMillis() - start
                        return@withContext Pair(true, latency)
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }
        Pair(false, -1L)
    }

    suspend fun fetch24HrTickers(): List<MarketTicker> = withContext(Dispatchers.IO) {
        for (ep in endpoints) {
            try {
                val req = Request.Builder()
                    .url("$ep/api/v3/ticker/24hr")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val array = JSONArray(body)
                    val result = mutableListOf<MarketTicker>()

                    val ignored = listOf("UPUSDT", "DOWNUSDT", "BEARUSDT", "BULLUSDT", "BUSDUSDT", "TUSDUSDT", "USDCUSDT", "EURUSDT", "FDUSDUSDT", "AEURUSDT")

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val sym = obj.getString("symbol")
                        if (!sym.endsWith("USDT") || ignored.any { sym == it || sym.endsWith(it) }) {
                            continue
                        }
                        val quoteVol = obj.optDouble("quoteVolume", 0.0)
                        if (quoteVol >= 250000.0) {
                            val base = sym.removeSuffix("USDT")
                            result.add(
                                MarketTicker(
                                    symbol = "$base/USDT",
                                    lastPrice = obj.optDouble("lastPrice", 0.0),
                                    priceChangePercent = obj.optDouble("priceChangePercent", 0.0),
                                    quoteVolume = quoteVol,
                                    highPrice = obj.optDouble("highPrice", 0.0),
                                    lowPrice = obj.optDouble("lowPrice", 0.0)
                                )
                            )
                        }
                    }
                    if (result.isNotEmpty()) {
                        activeEndpoint = ep
                        return@withContext result
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }
        emptyList()
    }

    suspend fun fetchOhlcv(
        symbol: String,
        timeframe: String = "15m",
        limit: Int = 80
    ): List<Candle> = withContext(Dispatchers.IO) {
        val cleanSym = symbol.replace("/", "").replace("-", "")
        for (ep in endpoints) {
            try {
                val url = "$ep/api/v3/klines?symbol=$cleanSym&interval=$timeframe&limit=$limit"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val array = JSONArray(body)
                    val candles = ArrayList<Candle>(array.length())

                    for (i in 0 until array.length()) {
                        val k = array.getJSONArray(i)
                        candles.add(
                            Candle(
                                timestamp = k.getLong(0),
                                open = k.getString(1).toDouble(),
                                high = k.getString(2).toDouble(),
                                low = k.getString(3).toDouble(),
                                close = k.getString(4).toDouble(),
                                volume = k.getString(5).toDouble()
                            )
                        )
                    }
                    if (candles.isNotEmpty()) {
                        return@withContext candles
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }
        emptyList()
    }

    suspend fun fetchCrossExchangeTickers(): List<MarketTicker> = withContext(Dispatchers.IO) {
        val allTickers = mutableListOf<MarketTicker>()

        // 1. Binance
        val binanceList = fetch24HrTickers()
        allTickers.addAll(binanceList.take(20).map {
            it.copy(exchange = "Binance", exchangeBadge = "🟡 BINANCE", displayName = "${it.symbol} (Binance)")
        })

        // 2. Bybit
        try {
            val req = Request.Builder().url("https://api.bybit.com/v5/market/tickers?category=spot").get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (body != null) {
                        val obj = org.json.JSONObject(body)
                        val list = obj.optJSONObject("result")?.optJSONArray("list")
                        if (list != null) {
                            for (i in 0 until list.length()) {
                                if (allTickers.count { it.exchange == "Bybit" } >= 20) break
                                val item = list.getJSONObject(i)
                                val sym = item.optString("symbol")
                                if (sym.endsWith("USDT") && !sym.contains("UP") && !sym.contains("DOWN")) {
                                    val base = sym.removeSuffix("USDT")
                                    val p = item.optDouble("lastPrice", 0.0)
                                    val chg = item.optDouble("price24hPcnt", 0.0) * 100.0
                                    val vol = item.optDouble("turnover24h", 0.0)
                                    allTickers.add(
                                        MarketTicker(
                                            symbol = "$base/USDT",
                                            lastPrice = p,
                                            priceChangePercent = chg,
                                            quoteVolume = vol,
                                            highPrice = item.optDouble("highPrice24h", p),
                                            lowPrice = item.optDouble("lowPrice24h", p),
                                            exchange = "Bybit",
                                            exchangeBadge = "🟠 BYBIT",
                                            displayName = "$base/USDT (Bybit)"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Bitget
        try {
            val req = Request.Builder().url("https://api.bitget.com/api/v2/spot/market/tickers").get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (body != null) {
                        val obj = org.json.JSONObject(body)
                        val array = obj.optJSONArray("data")
                        if (array != null) {
                            for (i in 0 until array.length()) {
                                if (allTickers.count { it.exchange == "Bitget" } >= 20) break
                                val item = array.getJSONObject(i)
                                val sym = item.optString("symbol")
                                if (sym.endsWith("USDT") && !sym.contains("UP") && !sym.contains("DOWN")) {
                                    val base = sym.removeSuffix("USDT")
                                    val p = item.optDouble("lastPr", 0.0)
                                    val chg = item.optDouble("change24h", 0.0) * 100.0
                                    val vol = item.optDouble("usdtVolume", 0.0)
                                    allTickers.add(
                                        MarketTicker(
                                            symbol = "$base/USDT",
                                            lastPrice = p,
                                            priceChangePercent = chg,
                                            quoteVolume = vol,
                                            highPrice = item.optDouble("high24h", p),
                                            lowPrice = item.optDouble("low24h", p),
                                            exchange = "Bitget",
                                            exchangeBadge = "🔵 BITGET",
                                            displayName = "$base/USDT (Bitget)"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Gate.io
        try {
            val req = Request.Builder().url("https://api.gateio.ws/api/v4/spot/tickers").get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (body != null) {
                        val array = org.json.JSONArray(body)
                        for (i in 0 until array.length()) {
                            if (allTickers.count { it.exchange == "Gate.io" } >= 20) break
                            val item = array.getJSONObject(i)
                            val cp = item.optString("currency_pair")
                            if (cp.endsWith("_USDT")) {
                                val base = cp.replace("_USDT", "")
                                val p = item.optDouble("last", 0.0)
                                val chg = item.optDouble("change_percentage", 0.0)
                                val vol = item.optDouble("quote_volume", 0.0)
                                allTickers.add(
                                    MarketTicker(
                                        symbol = "$base/USDT",
                                        lastPrice = p,
                                        priceChangePercent = chg,
                                        quoteVolume = vol,
                                        highPrice = item.optDouble("high_24h", p),
                                        lowPrice = item.optDouble("low_24h", p),
                                        exchange = "Gate.io",
                                        exchangeBadge = "🟣 GATE.IO",
                                        displayName = "$base/USDT (Gate.io)"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        allTickers.ifEmpty { binanceList }
    }

    suspend fun analyzeHtfStructure(symbol: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val candles4h = fetchOhlcv(symbol, timeframe = "4h", limit = 40)
        if (candles4h.size < 20) {
            return@withContext Pair("⚪ Neutral (4H)", "Consolidation Range")
        }

        val closes = candles4h.map { it.close }
        val ema21 = com.example.binanceterminal.strategy.TechnicalIndicators.computeEma(closes, 21).lastOrNull() ?: closes.last()
        val ema55 = com.example.binanceterminal.strategy.TechnicalIndicators.computeEma(closes, 55).lastOrNull() ?: closes.last()
        val rsi = com.example.binanceterminal.strategy.TechnicalIndicators.computeRsi(closes, 14).lastOrNull() ?: 50.0

        if (ema21 > ema55 && rsi >= 49.0) {
            Pair("🟢 Long Triggered (4H)", "Bullish 21/55 EMA Expansion & RSI " + String.format("%.1f", rsi))
        } else if (ema21 < ema55 && rsi <= 51.0) {
            Pair("🔴 Short Active (4H)", "Bearish 21/55 EMA Distribution & RSI " + String.format("%.1f", rsi))
        } else {
            Pair("⚪ Neutral (4H)", "Equilibrium Structure")
        }
    }

    suspend fun getTickerPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        val cleanSym = symbol.replace("/", "").replace("-", "")
        for (ep in endpoints) {
            try {
                val url = "$ep/api/v3/ticker/price?symbol=$cleanSym"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val obj = org.json.JSONObject(body)
                    return@withContext obj.optDouble("price", 0.0)
                }
            } catch (_: Exception) {
                // Fallback
            }
        }
        null
    }

    private var cachedPairs: List<String> = emptyList()
    private var lastPairsFetchTime: Long = 0L

    suspend fun getAllActivePairs(quote: String = "USDT", forceRefresh: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedPairs.isNotEmpty() && (now - lastPairsFetchTime < 900_000L)) {
            return@withContext cachedPairs
        }

        val ignored = listOf("UP", "DOWN", "BEAR", "BULL")
        for (ep in endpoints) {
            try {
                val url = "$ep/api/v3/exchangeInfo"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val json = org.json.JSONObject(body)
                    val symbolsArray = json.optJSONArray("symbols") ?: return@use
                    val pairs = mutableListOf<String>()

                    for (i in 0 until symbolsArray.length()) {
                        val symObj = symbolsArray.getJSONObject(i)
                        val status = symObj.optString("status")
                        val isSpot = symObj.optBoolean("isSpotTradingAllowed", true)
                        val q = symObj.optString("quoteAsset")
                        val base = symObj.optString("baseAsset")

                        if (status == "TRADING" && isSpot && q.equals(quote, ignoreCase = true)) {
                            if (ignored.none { base.contains(it) }) {
                                pairs.add("$base/$quote")
                            }
                        }
                    }

                    if (pairs.isNotEmpty()) {
                        val sorted = pairs.distinct().sorted()
                        cachedPairs = sorted
                        lastPairsFetchTime = now
                        return@withContext sorted
                    }
                }
            } catch (_: Exception) {
                // Try next mirror
            }
        }

        if (cachedPairs.isNotEmpty()) return@withContext cachedPairs

        listOf(
            "BTC/USDT", "ETH/USDT", "SOL/USDT", "BNB/USDT", "DOGE/USDT", "XRP/USDT",
            "ADA/USDT", "AVAX/USDT", "NEAR/USDT", "LINK/USDT", "SUI/USDT", "PEPE/USDT",
            "SHIB/USDT", "APT/USDT", "DOT/USDT", "MATIC/USDT", "LTC/USDT", "UNI/USDT"
        )
    }
}
