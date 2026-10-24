package com.example.binanceterminal.strategy

import com.example.binanceterminal.model.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class BollingerResult(
    val upper: List<Double>,
    val middle: List<Double>,
    val lower: List<Double>,
    val bandwidth: List<Double>
)

data class MacdResult(
    val macd: List<Double>,
    val signal: List<Double>,
    val hist: List<Double>
)

object TechnicalIndicators {

    fun computeAtr(candles: List<Candle>, period: Int = 14): List<Double> {
        val n = candles.size
        if (n == 0) return emptyList()
        val tr = ArrayList<Double>(n)
        tr.add(candles[0].high - candles[0].low)

        for (i in 1 until n) {
            val h = candles[i].high
            val l = candles[i].low
            val prevC = candles[i - 1].close
            val trVal = max(h - l, max(abs(h - prevC), abs(l - prevC)))
            tr.add(trVal)
        }

        // Wilder RMA smoothing
        val atr = ArrayList<Double>(n)
        if (n < period) {
            val avg = tr.average()
            for (i in 0 until n) atr.add(avg)
            return atr
        }

        var currentAtr = tr.subList(0, period).average()
        for (i in 0 until period - 1) {
            atr.add(currentAtr)
        }
        atr.add(currentAtr)

        for (i in period until n) {
            currentAtr = (currentAtr * (period - 1) + tr[i]) / period
            atr.add(currentAtr)
        }
        return atr
    }

    fun computeSma(values: List<Double>, period: Int): List<Double> {
        val n = values.size
        val sma = ArrayList<Double>(n)
        var sum = 0.0

        for (i in 0 until n) {
            sum += values[i]
            if (i >= period) {
                sum -= values[i - period]
            }
            val count = if (i + 1 < period) (i + 1) else period
            sma.add(sum / count)
        }
        return sma
    }

    fun computeEma(values: List<Double>, period: Int): List<Double> {
        val n = values.size
        if (n == 0) return emptyList()
        val ema = ArrayList<Double>(n)
        val k = 2.0 / (period + 1.0)
        var current = values[0]
        ema.add(current)

        for (i in 1 until n) {
            current = (values[i] * k) + (current * (1.0 - k))
            ema.add(current)
        }
        return ema
    }

    fun computeRsi(closes: List<Double>, period: Int = 14): List<Double> {
        val n = closes.size
        val rsi = ArrayList<Double>(n)
        if (n < 2) {
            for (i in 0 until n) rsi.add(50.0)
            return rsi
        }

        val gains = ArrayList<Double>(n)
        val losses = ArrayList<Double>(n)
        gains.add(0.0)
        losses.add(0.0)

        for (i in 1 until n) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) {
                gains.add(diff)
                losses.add(0.0)
            } else {
                gains.add(0.0)
                losses.add(abs(diff))
            }
        }

        var avgGain = 0.0
        var avgLoss = 0.0
        val p = if (n - 1 < period) (n - 1) else period

        for (i in 1..p) {
            avgGain += gains[i]
            avgLoss += losses[i]
        }
        avgGain /= max(p, 1)
        avgLoss /= max(p, 1)

        for (i in 0 until p) {
            rsi.add(50.0)
        }

        for (i in p until n) {
            if (i > p) {
                avgGain = (avgGain * (period - 1) + gains[i]) / period
                avgLoss = (avgLoss * (period - 1) + losses[i]) / period
            }
            if (avgLoss == 0.0) {
                rsi.add(100.0)
            } else {
                val rs = avgGain / avgLoss
                rsi.add(100.0 - (100.0 / (1.0 + rs)))
            }
        }
        return rsi
    }

    fun computeBollingerBands(closes: List<Double>, period: Int = 20, std: Double = 2.0): BollingerResult {
        val n = closes.size
        val upper = ArrayList<Double>(n)
        val middle = ArrayList<Double>(n)
        val lower = ArrayList<Double>(n)
        val bandwidth = ArrayList<Double>(n)

        for (i in 0 until n) {
            val startIdx = max(0, i - period + 1)
            val window = closes.subList(startIdx, i + 1)
            val mean = window.average()
            val variance = window.map { (it - mean) * (it - mean) }.average()
            val sd = sqrt(variance)

            val u = mean + (sd * std)
            val l = mean - (sd * std)
            val bw = if (mean > 0) (u - l) / mean else 0.0

            upper.add(u)
            middle.add(mean)
            lower.add(l)
            bandwidth.add(bw)
        }
        return BollingerResult(upper, middle, lower, bandwidth)
    }

    fun computeMacd(closes: List<Double>, fastPeriod: Int = 12, slowPeriod: Int = 26, signalPeriod: Int = 9): MacdResult {
        val n = closes.size
        if (n == 0) return MacdResult(emptyList(), emptyList(), emptyList())

        val fastEma = computeEma(closes, fastPeriod)
        val slowEma = computeEma(closes, slowPeriod)
        val macd = ArrayList<Double>(n)

        for (i in 0 until n) {
            macd.add(fastEma[i] - slowEma[i])
        }

        val signal = computeEma(macd, signalPeriod)
        val hist = ArrayList<Double>(n)

        for (i in 0 until n) {
            hist.add(macd[i] - signal[i])
        }

        return MacdResult(macd, signal, hist)
    }
}
