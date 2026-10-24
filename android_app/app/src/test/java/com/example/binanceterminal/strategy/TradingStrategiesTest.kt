package com.example.binanceterminal.strategy

import com.example.binanceterminal.model.Candle
import org.junit.Assert.*
import org.junit.Test

class TradingStrategiesTest {

    @Test
    fun testTechnicalIndicatorsComputation() {
        val candles = mutableListOf<Candle>()
        var price = 100.0
        val now = System.currentTimeMillis()

        for (i in 0 until 50) {
            price += (if (i % 2 == 0) 1.5 else -0.8)
            candles.add(
                Candle(
                    timestamp = now + (i * 900000L),
                    open = price - 0.5,
                    high = price + 1.2,
                    low = price - 1.0,
                    close = price,
                    volume = 1000.0 + (i * 20.0)
                )
            )
        }

        val atr = TechnicalIndicators.computeAtr(candles, 14)
        assertEquals(50, atr.size)
        assertTrue(atr.last() > 0.0)

        val closes = candles.map { it.close }
        val rsi = TechnicalIndicators.computeRsi(closes, 14)
        assertEquals(50, rsi.size)
        assertTrue(rsi.last() in 0.0..100.0)

        val bb = TechnicalIndicators.computeBollingerBands(closes, 20, 2.0)
        assertEquals(50, bb.upper.size)
        assertTrue(bb.upper.last() >= bb.middle.last())
        assertTrue(bb.middle.last() >= bb.lower.last())
    }

    @Test
    fun testMegaPumpCaptureEvaluation() {
        val candles = mutableListOf<Candle>()
        val now = System.currentTimeMillis()

        // 35 base consolidation candles
        for (i in 0 until 35) {
            candles.add(
                Candle(
                    timestamp = now + (i * 900000L),
                    open = 100.0,
                    high = 101.5,
                    low = 99.0,
                    close = 100.2,
                    volume = 500.0
                )
            )
        }

        // Ignition candle: 4x volume, closes above base high, bullish body >= 50%
        candles.add(
            Candle(
                timestamp = now + (35 * 900000L),
                open = 100.5,
                high = 110.0,
                low = 100.0,
                close = 108.5,
                volume = 2500.0
            )
        )

        val setup = TradingStrategies.evaluateMarketWideSetup("BTC/USDT", candles)
        assertNotNull(setup)
        assertEquals("LONG", setup?.direction)
        assertTrue(setup?.expectedMovePct ?: 0.0 >= 20.0)
        assertTrue(setup?.riskRewardRatio ?: 0.0 > 1.0)
        assertTrue(setup?.stopLoss ?: 0.0 < (setup?.entryPrice ?: 0.0))
    }
}
