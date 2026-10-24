package com.example.binanceterminal.engine

import com.example.binanceterminal.model.TradeSignal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaperTradingEngineTest {

    @Before
    fun setup() {
        PaperTradingEngine.resetAccount(10000.0)
        PaperTradingEngine.setTradeSize(100.0)
        PaperTradingEngine.setAutoTradeSettings(enabled = false)
    }

    private fun createDummySignal(
        symbol: String = "BTC/USDT",
        direction: String = "LONG",
        entry: Double = 50000.0,
        sl: Double = 48000.0,
        tp1: Double = 55000.0,
        tp2: Double = 60000.0,
        mega: Double = 75000.0,
        conf: Double = 94.0
    ): TradeSignal {
        return TradeSignal(
            id = 101L,
            timestamp = "2026-10-22 12:00:00",
            symbol = symbol,
            direction = direction,
            strategyName = "Smart Money Concepts (SMC)",
            entryPrice = entry,
            currentPrice = entry,
            stopLoss = sl,
            target10pct = tp1,
            target20pct = tp2,
            targetMega = mega,
            riskRewardRatio = 5.0,
            expectedMovePct = 20.0,
            aiConfidencePct = conf,
            isMegaMove = false,
            maxFavorablePct = 0.0,
            status = "ACTIVE",
            bandwidth = 0.04,
            volSurge = 2.5,
            timeframe = "15m",
            signalReason = "Bullish FVG Test",
            updatedAt = "2026-10-22 12:00:00"
        )
    }

    @Test
    fun testDemoAccountInitialState() {
        val acc = PaperTradingEngine.account.value
        assertEquals(10000.0, acc.balanceUsd, 0.01)
        assertTrue(PaperTradingEngine.openPositions.value.isEmpty())
        assertTrue(PaperTradingEngine.closedTrades.value.isEmpty())
    }

    @Test
    fun testManualTradeExecution() {
        val sig = createDummySignal(symbol = "ETH/USDT", entry = 3000.0, sl = 2850.0, tp2 = 3600.0)
        val opened = PaperTradingEngine.openManualTrade(sig, customSizeUsd = 200.0)

        assertTrue(opened)
        assertEquals(9800.0, PaperTradingEngine.account.value.balanceUsd, 0.01)
        assertEquals(1, PaperTradingEngine.openPositions.value.size)

        val pos = PaperTradingEngine.openPositions.value.first()
        assertEquals("ETH/USDT", pos.symbol)
        assertEquals(3000.0, pos.entryPrice, 0.01)
        assertEquals(200.0, pos.positionSizeUsd, 0.01)
    }

    @Test
    fun testAutomatedStopLossExecution() {
        val sig = createDummySignal(symbol = "SOL/USDT", entry = 150.0, sl = 140.0, tp2 = 180.0)
        PaperTradingEngine.openManualTrade(sig, customSizeUsd = 100.0)

        // Price drops to 139.0 (below Stop Loss 140.0)
        PaperTradingEngine.updateSymbolPrice("SOL/USDT", 139.0)

        assertTrue(PaperTradingEngine.openPositions.value.isEmpty())
        assertEquals(1, PaperTradingEngine.closedTrades.value.size)

        val closed = PaperTradingEngine.closedTrades.value.first()
        assertEquals("HARD SL HIT", closed.exitReason)
        assertFalse(closed.isWin)
        assertTrue(closed.realizedPnlUsd < 0.0)

        val analytics = PaperTradingEngine.analytics.value
        assertEquals(1, analytics.totalTrades)
        assertEquals(0, analytics.winningTrades)
        assertEquals(1, analytics.losingTrades)
        assertEquals(0.0, analytics.winRatePct, 0.01)
    }

    @Test
    fun testAutomatedTakeProfitExecution() {
        val sig = createDummySignal(symbol = "BNB/USDT", entry = 500.0, sl = 480.0, tp2 = 600.0)
        PaperTradingEngine.openManualTrade(sig, customSizeUsd = 100.0)

        // Price rallies to 605.0 (hits TP2 600.0)
        PaperTradingEngine.updateSymbolPrice("BNB/USDT", 605.0)

        assertTrue(PaperTradingEngine.openPositions.value.isEmpty())
        assertEquals(1, PaperTradingEngine.closedTrades.value.size)

        val closed = PaperTradingEngine.closedTrades.value.first()
        assertEquals("TP2 HIT (+20%)", closed.exitReason)
        assertTrue(closed.isWin)
        assertTrue(closed.realizedPnlUsd > 0.0)

        val analytics = PaperTradingEngine.analytics.value
        assertEquals(1, analytics.totalTrades)
        assertEquals(1, analytics.winningTrades)
        assertEquals(100.0, analytics.winRatePct, 0.01)
        assertTrue(analytics.netPnlUsd > 0.0)
        assertEquals(1, analytics.strategyBreakdown.size)
    }

    @Test
    fun testAutonomousAutoTradingEngine() {
        PaperTradingEngine.setAutoTradeSettings(enabled = true, minConfidence = 90.0, maxPositions = 3)

        // Signal with 94% confidence should trigger auto-trade
        val highConfSignal = createDummySignal(symbol = "AVAX/USDT", conf = 94.0)
        val opened = PaperTradingEngine.processSignalForAutoTrade(highConfSignal)

        assertTrue(opened)
        assertEquals(1, PaperTradingEngine.openPositions.value.size)
        assertTrue(PaperTradingEngine.openPositions.value.first().isAutoTrade)

        // Low confidence signal should be rejected
        val lowConfSignal = createDummySignal(symbol = "DOGE/USDT", conf = 85.0)
        val rejected = PaperTradingEngine.processSignalForAutoTrade(lowConfSignal)
        assertFalse(rejected)
    }
}
