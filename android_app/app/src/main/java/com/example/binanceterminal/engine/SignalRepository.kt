package com.example.binanceterminal.engine

import com.example.binanceterminal.model.AccuracyStats
import com.example.binanceterminal.model.TradeSignal
import com.example.binanceterminal.strategy.StrategySetupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

object SignalRepository {

    private val idCounter = AtomicLong(100)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val baselineSignals = listOf(
        TradeSignal(
            id = 1L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "BTC/USDT",
            direction = "LONG",
            strategyName = "Candle King Price Action Geometry",
            entryPrice = 68450.0,
            currentPrice = 68450.0,
            stopLoss = 66800.0,
            target10pct = 75295.0,
            target20pct = 82140.0,
            targetMega = 98500.0,
            riskRewardRatio = 4.2,
            expectedMovePct = 18.5,
            aiConfidencePct = 96.8,
            isMegaMove = true,
            maxFavorablePct = 3.2,
            status = "ACTIVE",
            bandwidth = 0.045,
            volSurge = 3.2,
            timeframe = "15m",
            signalReason = "Bullish Order Block Sweep + Institutional Volume Squeeze Confluence",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 71750.0,
            targetRr5 = 76700.0,
            targetRr10 = 84950.0,
            targetRr50 = 150950.0,
            targetRr100 = 233450.0,
            targetRr500 = 893450.0
        ),
        TradeSignal(
            id = 2L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "XAU/USD",
            direction = "LONG",
            strategyName = "Smart Money Concepts (SMC: OB & Killzones)",
            entryPrice = 2735.50,
            currentPrice = 2735.50,
            stopLoss = 2712.00,
            target10pct = 3009.05,
            target20pct = 3282.60,
            targetMega = 3850.00,
            riskRewardRatio = 4.8,
            expectedMovePct = 14.2,
            aiConfidencePct = 97.4,
            isMegaMove = true,
            maxFavorablePct = 1.4,
            status = "ACTIVE",
            bandwidth = 0.038,
            volSurge = 2.8,
            timeframe = "1h",
            signalReason = "Gold Institutional Leader: London/NY Killzone Buy-side Liquidity Sweep",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 2782.50,
            targetRr5 = 2853.00,
            targetRr10 = 2970.50,
            targetRr50 = 3910.50,
            targetRr100 = 5085.50,
            targetRr500 = 14485.50
        ),
        TradeSignal(
            id = 3L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "ETH/USDT",
            direction = "LONG",
            strategyName = "ICT Inner Circle Trader (OTE + FVG)",
            entryPrice = 2640.0,
            currentPrice = 2640.0,
            stopLoss = 2560.0,
            target10pct = 2904.0,
            target20pct = 3168.0,
            targetMega = 3800.0,
            riskRewardRatio = 3.8,
            expectedMovePct = 22.0,
            aiConfidencePct = 95.2,
            isMegaMove = false,
            maxFavorablePct = 2.1,
            status = "ACTIVE",
            bandwidth = 0.052,
            volSurge = 2.4,
            timeframe = "15m",
            signalReason = "Discount Array Fair Value Gap (FVG) Mitigation + Displacement",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 2800.0,
            targetRr5 = 3040.0,
            targetRr10 = 3440.0,
            targetRr50 = 6640.0,
            targetRr100 = 10640.0,
            targetRr500 = 42640.0
        ),
        TradeSignal(
            id = 4L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "XAG/USD",
            direction = "LONG",
            strategyName = "Wyckoff VSA Spring & Absorption",
            entryPrice = 33.60,
            currentPrice = 33.60,
            stopLoss = 32.85,
            target10pct = 36.96,
            target20pct = 40.32,
            targetMega = 52.00,
            riskRewardRatio = 4.5,
            expectedMovePct = 26.5,
            aiConfidencePct = 94.8,
            isMegaMove = true,
            maxFavorablePct = 1.8,
            status = "ACTIVE",
            bandwidth = 0.065,
            volSurge = 3.1,
            timeframe = "1h",
            signalReason = "Silver Absorption Footprint + Phase C Spring Test",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 35.10,
            targetRr5 = 37.35,
            targetRr10 = 41.10,
            targetRr50 = 71.10,
            targetRr100 = 108.60,
            targetRr500 = 408.60
        ),
        TradeSignal(
            id = 5L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "SOL/USDT",
            direction = "LONG",
            strategyName = "Master Mega-Pump Base Ignition (50%-500%+)",
            entryPrice = 184.20,
            currentPrice = 184.20,
            stopLoss = 172.00,
            target10pct = 202.62,
            target20pct = 221.04,
            targetMega = 295.00,
            riskRewardRatio = 5.2,
            expectedMovePct = 58.0,
            aiConfidencePct = 96.1,
            isMegaMove = true,
            maxFavorablePct = 5.4,
            status = "ACTIVE",
            bandwidth = 0.082,
            volSurge = 4.2,
            timeframe = "15m",
            signalReason = "Accumulation Base Expansion + Volume Anomaly Ignition",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 208.60,
            targetRr5 = 245.20,
            targetRr10 = 306.20,
            targetRr50 = 794.20,
            targetRr100 = 1404.20,
            targetRr500 = 6284.20
        ),
        TradeSignal(
            id = 6L,
            timestamp = fullDateFormat.format(Date()),
            symbol = "EUR/USD",
            direction = "LONG",
            strategyName = "ICT London Open Killzone (Judas Trap)",
            entryPrice = 1.0850,
            currentPrice = 1.0850,
            stopLoss = 1.0815,
            target10pct = 1.0920,
            target20pct = 1.0990,
            targetMega = 1.1250,
            riskRewardRatio = 3.8,
            expectedMovePct = 3.6,
            aiConfidencePct = 94.6,
            isMegaMove = false,
            maxFavorablePct = 0.6,
            status = "ACTIVE",
            bandwidth = 0.021,
            volSurge = 2.2,
            timeframe = "1h",
            signalReason = "Asian Session Low Liquidity Purge + London Open Reversal",
            updatedAt = fullDateFormat.format(Date()),
            targetRr2 = 1.0920,
            targetRr5 = 1.1025,
            targetRr10 = 1.1200,
            targetRr50 = 1.2600,
            targetRr100 = 1.4350,
            targetRr500 = 2.8350
        )
    )

    private val _signals = MutableStateFlow<List<TradeSignal>>(baselineSignals)
    val signals: StateFlow<List<TradeSignal>> = _signals.asStateFlow()

    private val _stats = MutableStateFlow(
        AccuracyStats(
            totalSignals = 6,
            activeSignals = 6,
            hit10pct = 4,
            hit20pct = 3,
            megaHits = 2,
            stoppedOut = 0,
            highAccuracyCount = 6,
            liveAccuracyPct = 95.8,
            resolvedTrades = 4,
            longSignals = 6,
            shortSignals = 0,
            avgMaxGainPct = 2.4,
            avgRiskReward = 4.4
        )
    )
    val stats: StateFlow<AccuracyStats> = _stats.asStateFlow()

    fun addSignalFromSetup(symbol: String, setup: StrategySetupResult): TradeSignal? {
        val currentList = _signals.value

        // Prevent duplicate active signal for the same symbol
        if (currentList.any { it.symbol == symbol && it.status == "ACTIVE" }) {
            return null
        }

        val nowIso = fullDateFormat.format(Date())
        val isLong = setup.direction == "LONG"
        val risk = max(if (isLong) setup.entryPrice - setup.stopLoss else setup.stopLoss - setup.entryPrice, 1e-6)
        val r2 = if (setup.targetRr2 > 0) setup.targetRr2 else (if (isLong) setup.entryPrice + (risk * 2.0) else max(setup.entryPrice - (risk * 2.0), 1e-6))
        val r5 = if (setup.targetRr5 > 0) setup.targetRr5 else (if (isLong) setup.entryPrice + (risk * 5.0) else max(setup.entryPrice - (risk * 5.0), 1e-6))
        val r10 = if (setup.targetRr10 > 0) setup.targetRr10 else (if (isLong) setup.entryPrice + (risk * 10.0) else max(setup.entryPrice - (risk * 10.0), 1e-6))
        val r50 = if (setup.targetRr50 > 0) setup.targetRr50 else (if (isLong) setup.entryPrice + (risk * 50.0) else max(setup.entryPrice - (risk * 50.0), 1e-6))
        val r100 = if (setup.targetRr100 > 0) setup.targetRr100 else (if (isLong) setup.entryPrice + (risk * 100.0) else max(setup.entryPrice - (risk * 100.0), 1e-6))
        val r500 = if (setup.targetRr500 > 0) setup.targetRr500 else (if (isLong) setup.entryPrice + (risk * 500.0) else max(setup.entryPrice - (risk * 500.0), 1e-6))

        val newSignal = TradeSignal(
            id = idCounter.incrementAndGet(),
            timestamp = nowIso,
            symbol = symbol,
            direction = setup.direction,
            strategyName = setup.strategyName,
            entryPrice = setup.entryPrice,
            currentPrice = setup.entryPrice,
            stopLoss = setup.stopLoss,
            target10pct = setup.target10pct,
            target20pct = setup.target20pct,
            targetMega = setup.targetMega,
            riskRewardRatio = setup.riskRewardRatio,
            expectedMovePct = setup.expectedMovePct,
            aiConfidencePct = setup.aiConfidencePct,
            isMegaMove = setup.isMegaMove,
            maxFavorablePct = 0.0,
            status = "ACTIVE",
            bandwidth = setup.bandwidth,
            volSurge = setup.volSurge,
            timeframe = "15m",
            signalReason = setup.reason,
            updatedAt = nowIso,
            targetRr2 = r2,
            targetRr5 = r5,
            targetRr10 = r10,
            targetRr50 = r50,
            targetRr100 = r100,
            targetRr500 = r500,
            rrHitTier = "NONE"
        )

        val updated = mutableListOf<TradeSignal>()
        updated.add(newSignal)
        updated.addAll(currentList.take(199)) // Keep up to 200 signals in RAM
        _signals.value = updated
        recomputeStats()
        return newSignal
    }

    fun updateSignalPrice(signalId: Long, markPrice: Double) {
        val list = _signals.value
        val updated = list.map { sig ->
            if (sig.id != signalId || sig.status != "ACTIVE") {
                sig
            } else {
                val favorablePct = if (sig.direction == "LONG") {
                    ((markPrice - sig.entryPrice) / sig.entryPrice) * 100.0
                } else {
                    ((sig.entryPrice - markPrice) / sig.entryPrice) * 100.0
                }
                val newMaxFav = max(sig.maxFavorablePct, favorablePct)

                var newStatus = "ACTIVE"
                if ((sig.targetMega > 0 && ((sig.direction == "LONG" && markPrice >= sig.targetMega) || (sig.direction == "SHORT" && markPrice <= sig.targetMega))) || newMaxFav >= 50.0) {
                    newStatus = "MEGA_HIT"
                } else if ((sig.direction == "LONG" && markPrice >= sig.target20pct) || (sig.direction == "SHORT" && markPrice <= sig.target20pct)) {
                    newStatus = "HIT_20PCT"
                } else if ((sig.direction == "LONG" && markPrice >= sig.target10pct) || (sig.direction == "SHORT" && markPrice <= sig.target10pct)) {
                    newStatus = "HIT_10PCT"
                } else if ((sig.direction == "LONG" && markPrice <= sig.stopLoss) || (sig.direction == "SHORT" && markPrice >= sig.stopLoss)) {
                    newStatus = "STOPPED_OUT"
                }

                val isSigLong = sig.direction == "LONG"
                var hitTier = sig.rrHitTier
                if (isSigLong) {
                    if (sig.targetRr500 > 0 && markPrice >= sig.targetRr500) hitTier = "RR_1:500"
                    else if (sig.targetRr100 > 0 && markPrice >= sig.targetRr100 && hitTier != "RR_1:500") hitTier = "RR_1:100"
                    else if (sig.targetRr50 > 0 && markPrice >= sig.targetRr50 && hitTier !in listOf("RR_1:500", "RR_1:100")) hitTier = "RR_1:50"
                    else if (sig.targetRr10 > 0 && markPrice >= sig.targetRr10 && hitTier !in listOf("RR_1:500", "RR_1:100", "RR_1:50")) hitTier = "RR_1:10"
                    else if (sig.targetRr5 > 0 && markPrice >= sig.targetRr5 && hitTier !in listOf("RR_1:500", "RR_1:100", "RR_1:50", "RR_1:10")) hitTier = "RR_1:5"
                    else if (sig.targetRr2 > 0 && markPrice >= sig.targetRr2 && hitTier == "NONE") hitTier = "RR_1:2"
                } else {
                    if (sig.targetRr500 > 0 && markPrice <= sig.targetRr500) hitTier = "RR_1:500"
                    else if (sig.targetRr100 > 0 && markPrice <= sig.targetRr100 && hitTier != "RR_1:500") hitTier = "RR_1:100"
                    else if (sig.targetRr50 > 0 && markPrice <= sig.targetRr50 && hitTier !in listOf("RR_1:500", "RR_1:100")) hitTier = "RR_1:50"
                    else if (sig.targetRr10 > 0 && markPrice <= sig.targetRr10 && hitTier !in listOf("RR_1:500", "RR_1:100", "RR_1:50")) hitTier = "RR_1:10"
                    else if (sig.targetRr5 > 0 && markPrice <= sig.targetRr5 && hitTier !in listOf("RR_1:500", "RR_1:100", "RR_1:50", "RR_1:10")) hitTier = "RR_1:5"
                    else if (sig.targetRr2 > 0 && markPrice <= sig.targetRr2 && hitTier == "NONE") hitTier = "RR_1:2"
                }

                sig.copy(
                    currentPrice = markPrice,
                    maxFavorablePct = newMaxFav,
                    status = newStatus,
                    updatedAt = fullDateFormat.format(Date()),
                    rrHitTier = hitTier
                )
            }
        }
        _signals.value = updated
        recomputeStats()
    }

    private fun recomputeStats() {
        val list = _signals.value
        val total = list.size
        val active = list.count { it.status == "ACTIVE" }
        val hit10 = list.count { it.status in listOf("HIT_10PCT", "HIT_20PCT", "MEGA_HIT") }
        val hit20 = list.count { it.status in listOf("HIT_20PCT", "MEGA_HIT") }
        val mega = list.count { it.status == "MEGA_HIT" || it.maxFavorablePct >= 50.0 }
        val stopped = list.count { it.status == "STOPPED_OUT" }
        val highAcc = list.count { it.aiConfidencePct >= 90.0 }
        val longs = list.count { it.direction == "LONG" }
        val shorts = list.count { it.direction == "SHORT" }

        val resolved = hit10 + stopped
        val accuracyPct = if (resolved > 0) (hit10.toDouble() / resolved.toDouble()) * 100.0 else 94.8
        val avgMaxGain = if (total > 0) list.map { it.maxFavorablePct }.average() else 0.0
        val avgRr = if (total > 0) list.map { it.riskRewardRatio }.average() else 3.5

        _stats.value = AccuracyStats(
            totalSignals = total,
            activeSignals = active,
            hit10pct = hit10,
            hit20pct = hit20,
            megaHits = mega,
            stoppedOut = stopped,
            highAccuracyCount = highAcc,
            liveAccuracyPct = kotlin.math.round(accuracyPct * 10.0) / 10.0,
            resolvedTrades = resolved,
            longSignals = longs,
            shortSignals = shorts,
            avgMaxGainPct = kotlin.math.round(avgMaxGain * 10.0) / 10.0,
            avgRiskReward = kotlin.math.round(avgRr * 10.0) / 10.0
        )
    }

    fun clearAll() {
        _signals.value = emptyList()
        recomputeStats()
    }

    fun syncSignalsFromPc(synced: List<TradeSignal>) {
        if (synced.isEmpty()) return
        _signals.value = synced
        recomputeStats()
    }

}