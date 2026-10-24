package com.example.binanceterminal.model

/**
 * Data models for the Autonomous Paper Trading & Demo Station.
 */

data class PaperPosition(
    val id: String,
    val signalId: Long,
    val symbol: String,
    val strategyName: String,
    val direction: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val target10pct: Double,
    val target20pct: Double,
    val targetMega: Double,
    val positionSizeUsd: Double,
    val quantity: Double, // positionSizeUsd / entryPrice
    val unrealizedPnlUsd: Double,
    val unrealizedPnlPct: Double,
    val highestPnlPct: Double,
    val openedAt: Long,
    val openedAtFormatted: String,
    val isAutoTrade: Boolean,
    val achieved1R: Boolean = false,
    val earlyExitAlert: String? = null
)

data class ClosedTrade(
    val id: String,
    val signalId: Long,
    val symbol: String,
    val strategyName: String,
    val direction: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val positionSizeUsd: Double,
    val realizedPnlUsd: Double,
    val realizedPnlPct: Double,
    val exitReason: String, // "TP1 (+10%)", "TP2 (+20%)", "MEGA TP (50%+)", "HARD SL HIT", "MANUAL EXIT"
    val openedAt: Long,
    val closedAt: Long,
    val closedAtFormatted: String,
    val isWin: Boolean
)

data class PaperAccount(
    val balanceUsd: Double = 10000.0,
    val startingBalanceUsd: Double = 10000.0,
    val tradeSizeUsd: Double = 1.0, // Configurable position sizing ($1 default)
    val autoTradeEnabled: Boolean = false,
    val autoTradeMinConfidence: Double = 90.0,
    val autoTradeMaxPositions: Int = 10,
    val autoTradeOnlyMega: Boolean = false
)

data class StrategyStats(
    val strategyName: String,
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val winRatePct: Double,
    val netPnlUsd: Double
)

data class PaperAnalytics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val winRatePct: Double = 0.0,
    val netPnlUsd: Double = 0.0,
    val netPnlPct: Double = 0.0,
    val profitFactor: Double = 0.0,
    val bestTradeUsd: Double = 0.0,
    val worstTradeUsd: Double = 0.0,
    val strategyBreakdown: List<StrategyStats> = emptyList()
)
