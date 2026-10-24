package com.example.binanceterminal.model

data class TradeSignal(
    val id: Long,
    val timestamp: String,
    val symbol: String,
    val direction: String, // "LONG" or "SHORT"
    val strategyName: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val target10pct: Double,
    val target20pct: Double,
    val targetMega: Double,
    val riskRewardRatio: Double,
    val expectedMovePct: Double,
    val aiConfidencePct: Double,
    val isMegaMove: Boolean,
    val maxFavorablePct: Double,
    val status: String, // "ACTIVE", "MEGA_HIT", "HIT_20PCT", "HIT_10PCT", "STOPPED_OUT"
    val bandwidth: Double,
    val volSurge: Double,
    val timeframe: String,
    val signalReason: String,
    val updatedAt: String,
    val targetRr2: Double = 0.0,
    val targetRr5: Double = 0.0,
    val targetRr10: Double = 0.0,
    val targetRr50: Double = 0.0,
    val targetRr100: Double = 0.0,
    val targetRr500: Double = 0.0,
    val rrHitTier: String = "NONE"
) {
    val livePnlPct: Double
        get() = if (direction == "LONG") {
            if (entryPrice > 0) ((currentPrice - entryPrice) / entryPrice) * 100.0 else 0.0
        } else {
            if (entryPrice > 0) ((entryPrice - currentPrice) / entryPrice) * 100.0 else 0.0
        }
}
