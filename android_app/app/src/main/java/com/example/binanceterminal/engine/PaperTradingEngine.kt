package com.example.binanceterminal.engine

import com.example.binanceterminal.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.round

/**
 * Autonomous Paper Trading & Demo Station Engine.
 * Supports both manual order execution and autonomous AI auto-trading.
 * Tracks live positions, auto-executes SL/TP rules with trailing protection,
 * and compiles comprehensive session performance analytics.
 */
object PaperTradingEngine {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val _account = MutableStateFlow(PaperAccount())
    val account: StateFlow<PaperAccount> = _account.asStateFlow()

    private val _openPositions = MutableStateFlow<List<PaperPosition>>(emptyList())
    val openPositions: StateFlow<List<PaperPosition>> = _openPositions.asStateFlow()

    private val _closedTrades = MutableStateFlow<List<ClosedTrade>>(emptyList())
    val closedTrades: StateFlow<List<ClosedTrade>> = _closedTrades.asStateFlow()

    private val _analytics = MutableStateFlow(PaperAnalytics())
    val analytics: StateFlow<PaperAnalytics> = _analytics.asStateFlow()

    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    fun clearNotification() {
        _notificationMessage.value = null
    }

    @Synchronized
    fun openPaperTrade(signal: TradeSignal, customSizeUsd: Double? = null): Boolean = openManualTrade(signal, customSizeUsd)

    @Synchronized
    fun openManualTrade(signal: TradeSignal, customSizeUsd: Double? = null): Boolean {
        val acc = _account.value
        val tradeSize = customSizeUsd ?: acc.tradeSizeUsd

        if (acc.balanceUsd < tradeSize) {
            _notificationMessage.value = "⚠️ Insufficient Demo Funds: Need \$${round(tradeSize * 10.0) / 10.0}, have \$${round(acc.balanceUsd * 10.0) / 10.0}"
            return false
        }

        val currentList = _openPositions.value
        if (currentList.any { it.symbol == signal.symbol }) {
            _notificationMessage.value = "⚠️ Position already active for ${signal.symbol}"
            return false
        }

        val now = System.currentTimeMillis()
        val entryP = if (signal.currentPrice > 0) signal.currentPrice else signal.entryPrice
        val qty = tradeSize / max(entryP, 1e-6)

        val position = PaperPosition(
            id = UUID.randomUUID().toString().take(8),
            signalId = signal.id,
            symbol = signal.symbol,
            strategyName = signal.strategyName,
            direction = signal.direction,
            entryPrice = entryP,
            currentPrice = entryP,
            stopLoss = signal.stopLoss,
            target10pct = signal.target10pct,
            target20pct = signal.target20pct,
            targetMega = signal.targetMega,
            positionSizeUsd = tradeSize,
            quantity = qty,
            unrealizedPnlUsd = 0.0,
            unrealizedPnlPct = 0.0,
            highestPnlPct = 0.0,
            openedAt = now,
            openedAtFormatted = timeFormat.format(Date(now)),
            isAutoTrade = false
        )

        _account.value = acc.copy(balanceUsd = acc.balanceUsd - tradeSize)
        _openPositions.value = listOf(position) + currentList
        _notificationMessage.value = "🚀 Demo Trade Opened: ${signal.direction} ${signal.symbol} (\$${round(tradeSize)})"
        return true
    }

    @Synchronized
    fun processSignalForAutoTrade(signal: TradeSignal): Boolean {
        val acc = _account.value
        if (!acc.autoTradeEnabled) return false

        val currentPositions = _openPositions.value
        if (currentPositions.size >= acc.autoTradeMaxPositions) return false
        if (signal.aiConfidencePct < acc.autoTradeMinConfidence) return false
        if (acc.autoTradeOnlyMega && !signal.isMegaMove) return false
        if (currentPositions.any { it.symbol == signal.symbol }) return false
        if (acc.balanceUsd < acc.tradeSizeUsd) return false

        val now = System.currentTimeMillis()
        val entryP = if (signal.currentPrice > 0) signal.currentPrice else signal.entryPrice
        val qty = acc.tradeSizeUsd / max(entryP, 1e-6)

        val position = PaperPosition(
            id = UUID.randomUUID().toString().take(8),
            signalId = signal.id,
            symbol = signal.symbol,
            strategyName = signal.strategyName,
            direction = signal.direction,
            entryPrice = entryP,
            currentPrice = entryP,
            stopLoss = signal.stopLoss,
            target10pct = signal.target10pct,
            target20pct = signal.target20pct,
            targetMega = signal.targetMega,
            positionSizeUsd = acc.tradeSizeUsd,
            quantity = qty,
            unrealizedPnlUsd = 0.0,
            unrealizedPnlPct = 0.0,
            highestPnlPct = 0.0,
            openedAt = now,
            openedAtFormatted = timeFormat.format(Date(now)),
            isAutoTrade = true
        )

        _account.value = acc.copy(balanceUsd = acc.balanceUsd - acc.tradeSizeUsd)
        _openPositions.value = listOf(position) + currentPositions
        _notificationMessage.value = "🤖 Auto-Trade Executed: ${signal.direction} ${signal.symbol} (${signal.strategyName})"
        return true
    }

    @Synchronized
    fun updateSymbolPrice(symbol: String, price: Double) {
        val currentList = _openPositions.value
        if (currentList.none { it.symbol == symbol }) return

        val remainingPositions = mutableListOf<PaperPosition>()
        val closedNow = mutableListOf<ClosedTrade>()
        var balanceRefund = 0.0

        for (pos in currentList) {
            if (pos.symbol != symbol) {
                remainingPositions.add(pos)
                continue
            }

            val pnlPct = if (pos.direction == "LONG") {
                ((price - pos.entryPrice) / pos.entryPrice) * 100.0
            } else {
                ((pos.entryPrice - price) / pos.entryPrice) * 100.0
            }
            val pnlUsd = (pnlPct / 100.0) * pos.positionSizeUsd
            val newHighest = max(pos.highestPnlPct, pnlPct)

            // 1:1 R:R Target Milestone & Breakeven Protection
            val initialRiskPct = max(abs(pos.entryPrice - pos.stopLoss) / max(pos.entryPrice, 1e-6) * 100.0, 1.5)
            val is1R = pos.achieved1R || (newHighest >= initialRiskPct)
            var updatedStopLoss = pos.stopLoss
            var earlyAlert: String? = pos.earlyExitAlert

            if (is1R) {
                // Ratchet SL to Breakeven once 1R is hit to permanently protect demo capital
                if (pos.direction == "LONG" && updatedStopLoss < pos.entryPrice) {
                    updatedStopLoss = pos.entryPrice
                } else if (pos.direction == "SHORT" && updatedStopLoss > pos.entryPrice) {
                    updatedStopLoss = pos.entryPrice
                }

                // Smart Reversal Detection: Gave back >= 35% of peak gains after achieving 1R
                if (newHighest >= 2.0 && pnlPct <= newHighest * 0.65 && earlyAlert == null) {
                    val peakStr = "${round(newHighest * 10.0) / 10.0}%"
                    earlyAlert = "⚠️ Reversal Alert: Reached +$peakStr (1:1+ R:R) & showing momentum exhaustion. Tap to secure profits!"
                    _notificationMessage.value = "⚠️ SMART REVERSAL: ${pos.symbol} reached 1:1 R:R (+$peakStr). Secure profits now!"
                }
            }

            // Check automated exit triggers
            var shouldClose = false
            var exitReason = ""
            var isWin = false

            if (pos.direction == "LONG") {
                if (price <= updatedStopLoss) {
                    shouldClose = true
                    exitReason = if (is1R) "BREAKEVEN SL HIT" else "HARD SL HIT"
                    isWin = is1R && (pnlPct >= -0.05)
                } else if (pos.targetMega > 0 && price >= pos.targetMega) {
                    shouldClose = true
                    exitReason = "MEGA TP HIT (+50%+)"
                    isWin = true
                } else if (price >= pos.target20pct) {
                    shouldClose = true
                    exitReason = "TP2 HIT (+20%)"
                    isWin = true
                } else if (price >= pos.target10pct && updatedStopLoss < pos.entryPrice) {
                    updatedStopLoss = pos.entryPrice
                }
            } else {
                if (price >= updatedStopLoss) {
                    shouldClose = true
                    exitReason = if (is1R) "BREAKEVEN SL HIT" else "HARD SL HIT"
                    isWin = is1R && (pnlPct >= -0.05)
                } else if (pos.targetMega > 0 && price <= pos.targetMega) {
                    shouldClose = true
                    exitReason = "MEGA TP HIT (-50%+)"
                    isWin = true
                } else if (price <= pos.target20pct) {
                    shouldClose = true
                    exitReason = "TP2 HIT (+20%)"
                    isWin = true
                } else if (price <= pos.target10pct && updatedStopLoss > pos.entryPrice) {
                    updatedStopLoss = pos.entryPrice
                }
            }

            if (shouldClose) {
                val now = System.currentTimeMillis()
                val closedTrade = ClosedTrade(
                    id = pos.id,
                    signalId = pos.signalId,
                    symbol = pos.symbol,
                    strategyName = pos.strategyName,
                    direction = pos.direction,
                    entryPrice = pos.entryPrice,
                    exitPrice = price,
                    positionSizeUsd = pos.positionSizeUsd,
                    realizedPnlUsd = pnlUsd,
                    realizedPnlPct = pnlPct,
                    exitReason = exitReason,
                    openedAt = pos.openedAt,
                    closedAt = now,
                    closedAtFormatted = timeFormat.format(Date(now)),
                    isWin = isWin
                )
                closedNow.add(closedTrade)
                balanceRefund += max(0.0, pos.positionSizeUsd + pnlUsd)
                _notificationMessage.value = "${if (isWin) "🎉" else "🛑"} Closed: ${pos.symbol} ($exitReason: ${round(pnlPct * 10.0) / 10.0}%)"
            } else {
                remainingPositions.add(
                    pos.copy(
                        currentPrice = price,
                        unrealizedPnlUsd = pnlUsd,
                        unrealizedPnlPct = pnlPct,
                        highestPnlPct = newHighest,
                        stopLoss = updatedStopLoss,
                        achieved1R = is1R,
                        earlyExitAlert = earlyAlert
                    )
                )
            }
        }

        if (closedNow.isNotEmpty()) {
            val acc = _account.value
            _account.value = acc.copy(balanceUsd = acc.balanceUsd + balanceRefund)
            _closedTrades.value = closedNow + _closedTrades.value
            recomputeAnalytics()
        }
        _openPositions.value = remainingPositions
    }

    @Synchronized
    fun closePosition(positionId: String, reason: String = "MANUAL EXIT"): ClosedTrade? {
        val currentList = _openPositions.value
        val pos = currentList.find { it.id == positionId } ?: return null

        val now = System.currentTimeMillis()
        val isWin = pos.unrealizedPnlUsd >= 0.0
        val closedTrade = ClosedTrade(
            id = pos.id,
            signalId = pos.signalId,
            symbol = pos.symbol,
            strategyName = pos.strategyName,
            direction = pos.direction,
            entryPrice = pos.entryPrice,
            exitPrice = pos.currentPrice,
            positionSizeUsd = pos.positionSizeUsd,
            realizedPnlUsd = pos.unrealizedPnlUsd,
            realizedPnlPct = pos.unrealizedPnlPct,
            exitReason = reason,
            openedAt = pos.openedAt,
            closedAt = now,
            closedAtFormatted = timeFormat.format(Date(now)),
            isWin = isWin
        )

        val acc = _account.value
        val refund = max(0.0, pos.positionSizeUsd + pos.unrealizedPnlUsd)
        _account.value = acc.copy(balanceUsd = acc.balanceUsd + refund)
        _openPositions.value = currentList.filter { it.id != positionId }
        _closedTrades.value = listOf(closedTrade) + _closedTrades.value
        recomputeAnalytics()

        _notificationMessage.value = "Manual Exit: ${pos.symbol} (${round(pos.unrealizedPnlPct * 10.0) / 10.0}%)"
        return closedTrade
    }

    @Synchronized
    fun closeAllPositions() {
        val positions = _openPositions.value.toList()
        for (pos in positions) {
            closePosition(pos.id, "MANUAL EXIT ALL")
        }
    }

    @Synchronized
    fun resetAccount(newBalance: Double = 10000.0) {
        _account.value = _account.value.copy(
            balanceUsd = newBalance,
            startingBalanceUsd = newBalance
        )
        _openPositions.value = emptyList()
        _closedTrades.value = emptyList()
        recomputeAnalytics()
        _notificationMessage.value = "Demo Account Reset: \$${round(newBalance)} Starting Balance"
    }

    @Synchronized
    fun setTradeSize(sizeUsd: Double) {
        _account.value = _account.value.copy(tradeSizeUsd = max(1.0, sizeUsd))
    }

    @Synchronized
    fun setAutoTradeSettings(
        enabled: Boolean,
        minConfidence: Double = 90.0,
        maxPositions: Int = 10,
        onlyMega: Boolean = false
    ) {
        _account.value = _account.value.copy(
            autoTradeEnabled = enabled,
            autoTradeMinConfidence = minConfidence,
            autoTradeMaxPositions = maxPositions,
            autoTradeOnlyMega = onlyMega
        )
        _notificationMessage.value = if (enabled) "🤖 Auto-Trading Activated (Min Conf: ${round(minConfidence)}%)" else "⏹ Auto-Trading Paused"

        if (enabled) {
            val active = SignalRepository.signals.value.filter { it.status == "ACTIVE" }
            for (sig in active) {
                if (_openPositions.value.size >= maxPositions) break
                processSignalForAutoTrade(sig)
            }
        }
    }

    @Synchronized
    fun recomputeAnalytics() {
        val trades = _closedTrades.value
        val total = trades.size
        if (total == 0) {
            _analytics.value = PaperAnalytics()
            return
        }

        val wins = trades.count { it.isWin }
        val losses = total - wins
        val winRate = (wins.toDouble() / total.toDouble()) * 100.0
        val netPnlUsd = trades.sumOf { it.realizedPnlUsd }

        val startingBal = _account.value.startingBalanceUsd
        val netPnlPct = if (startingBal > 0) (netPnlUsd / startingBal) * 100.0 else 0.0

        val grossProfit = trades.filter { it.realizedPnlUsd > 0 }.sumOf { it.realizedPnlUsd }
        val grossLoss = abs(trades.filter { it.realizedPnlUsd < 0 }.sumOf { it.realizedPnlUsd })
        val profitFactor = if (grossLoss > 0) round((grossProfit / grossLoss) * 10.0) / 10.0 else (if (grossProfit > 0) 99.9 else 0.0)

        val bestTrade = trades.maxOfOrNull { it.realizedPnlUsd } ?: 0.0
        val worstTrade = trades.minOfOrNull { it.realizedPnlUsd } ?: 0.0

        // Strategy-wise breakdown
        val breakdown = trades.groupBy { it.strategyName }.map { (strat, tList) ->
            val sTotal = tList.size
            val sWins = tList.count { it.isWin }
            val sLoss = sTotal - sWins
            val sWinRate = if (sTotal > 0) (sWins.toDouble() / sTotal.toDouble()) * 100.0 else 0.0
            val sPnl = tList.sumOf { it.realizedPnlUsd }
            StrategyStats(
                strategyName = strat,
                totalTrades = sTotal,
                wins = sWins,
                losses = sLoss,
                winRatePct = round(sWinRate * 10.0) / 10.0,
                netPnlUsd = round(sPnl * 10.0) / 10.0
            )
        }.sortedByDescending { it.winRatePct }

        _analytics.value = PaperAnalytics(
            totalTrades = total,
            winningTrades = wins,
            losingTrades = losses,
            winRatePct = round(winRate * 10.0) / 10.0,
            netPnlUsd = round(netPnlUsd * 10.0) / 10.0,
            netPnlPct = round(netPnlPct * 10.0) / 10.0,
            profitFactor = profitFactor,
            bestTradeUsd = round(bestTrade * 10.0) / 10.0,
            worstTradeUsd = round(worstTrade * 10.0) / 10.0,
            strategyBreakdown = breakdown
        )
    }

    private fun abs(d: Double): Double = kotlin.math.abs(d)
}
