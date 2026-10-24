package com.example.binanceterminal.model

data class MarketTicker(
    val symbol: String,
    val lastPrice: Double,
    val priceChangePercent: Double,
    val quoteVolume: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val exchange: String = "Binance",
    val exchangeBadge: String = "🟡 BINANCE",
    val displayName: String = "$symbol ($exchange)"
)
