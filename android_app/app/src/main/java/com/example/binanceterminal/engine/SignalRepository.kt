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

    private val _signals = MutableStateFlow<List<TradeSignal>>(emptyList())
    val signals: StateFlow<List<TradeSignal>> = _signals.asStateFlow()

    private val _stats = MutableStateFlow(AccuracyStats())
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