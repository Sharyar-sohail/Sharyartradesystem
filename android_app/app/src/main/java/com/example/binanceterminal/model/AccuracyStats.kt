package com.example.binanceterminal.model

data class AccuracyStats(
    val totalSignals: Int = 0,
    val activeSignals: Int = 0,
    val hit10pct: Int = 0,
    val hit20pct: Int = 0,
    val megaHits: Int = 0,
    val stoppedOut: Int = 0,
    val highAccuracyCount: Int = 0,
    val liveAccuracyPct: Double = 0.0,
    val resolvedTrades: Int = 0,
    val longSignals: Int = 0,
    val shortSignals: Int = 0,
    val avgMaxGainPct: Double = 0.0,
    val avgRiskReward: Double = 3.5
)
