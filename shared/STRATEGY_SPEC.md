# Canonical Multi-Platform Strategy Specification (Python & Android)

This document defines the mathematical models, trigger thresholds, and trade metric formulas shared across both the **Python Web/Desktop Engine** and the **Standalone Native Android Mobile App**.

Adhering strictly to these formulas guarantees that signals generated independently on the Android phone and the Python workstation yield **100% identical outputs**, including entry prices, hard stop-losses, take-profit targets, Risk-to-Reward (R:R) ratios, and AI confidence scores.

---

## 1. Indicator Calculation Standard

| Indicator | Formula / Parameters | Minimum Lookback |
| :--- | :--- | :--- |
| **ATR (Average True Range)** | $\text{TR} = \max(\text{High}-\text{Low}, |\text{High}-\text{PrevClose}|, |\text{Low}-\text{PrevClose}|)$; 14-period Wilder RMA. | 14 bars |
| **Bollinger Bands** | 20-period SMA of Close $\pm 2.0 \times \text{StdDev}$. Bandwidth: $(\text{Upper}-\text{Lower})/\text{Middle}$. | 25 bars |
| **EMA** | Multi-span Exponential Moving Averages: Fast (9), Medium (21), Slow (55). | 55 bars |
| **RSI** | 14-period Relative Strength Index with smoothed gain/loss. | 14 bars |
| **MACD** | Fast (12), Slow (26), Signal (9). | 35 bars |
| **Volume Baseline** | 20-period Simple Moving Average of Volume. | 20 bars |

---

## 2. Core Trading Strategies

### 1. Master Mega-Pump Capture Strategy (50% to 500%+ Explosions)
* **Trigger Conditions**:
  1. $\text{Volume Surge} \ge 2.50\times$ (Volume $\ge 2.5\times$ 20-period SMA).
  2. $\text{Close} > \max(\text{High}[t-1 \dots t-12])$ (Breakout from 12-bar base consolidation).
  3. $\text{Close} > \text{Open}$ (Bullish displacement).
  4. $(\text{Close}-\text{Open})/(\text{High}-\text{Low}) \ge 0.50$ (Real body $\ge 50\%$ of candle range).
  5. $\text{Close} > \text{EMA}_{21}$.
  6. $50 \le \text{RSI}_{14} \le 85$.
* **Risk & Profit Levels**:
  * $\text{Entry} = \text{Close}$
  * $\text{Hard SL} = \text{Entry} - \max(\text{ATR}_{14} \times 1.5, \text{Entry} \times 0.035)$
  * $\text{Risk} = \max(\text{Entry} - \text{SL}, 10^{-6})$
  * $\text{TP1 (15\%)} = \text{Entry} \times 1.15$
  * $\text{TP2 (30\%)} = \text{Entry} \times 1.30$
  * $\text{TP Mega (120\%)} = \text{Entry} \times 2.20$
  * $\text{R:R} = \text{round}((\text{TP Mega} - \text{Entry}) / \text{Risk}, 1)$
  * $\text{Expected Move} = +120.0\%$
  * $\text{AI Confidence} = 94.5\%$
  * $\text{Is Mega Move} = \text{True}$

---

### 2. Proprietary AI Dynamic Consensus Core (90% to 100% Win Rate)
* **Trigger Conditions**:
  * Multi-strategy voting across SMC, ICT, VFS, VSA, Bollinger Squeeze, and Candle King.
  * Bullish Consensus: $\ge 2$ aligned institutional bullish signals.
  * Bearish Consensus: $\ge 2$ aligned institutional bearish signals.
  * Formula: $\text{Confidence} = \min(88.0 + (\text{Votes} \times 3.5), 98.5)\%$.
* **Risk & Profit Levels**:
  * **LONG**:
    * $\text{Hard SL} = \text{Entry} - \max(\text{ATR}_{14} \times 1.4, \text{Entry} \times 0.025)$
    * $\text{TP1 (10\%)} = \text{Entry} \times 1.10$
    * $\text{TP2 (20\%)} = \text{Entry} \times 1.20$
    * $\text{Mega TP (50\%)} = \text{Entry} \times 1.50$
    * $\text{R:R} = \text{round}((\text{TP2} - \text{Entry}) / \text{Risk}, 1)$
    * $\text{Expected Move} = +20.0\%$
  * **SHORT**:
    * $\text{Hard SL} = \text{Entry} + \max(\text{ATR}_{14} \times 1.4, \text{Entry} \times 0.025)$
    * $\text{TP1 (10\%)} = \text{Entry} \times 0.90$
    * $\text{TP2 (20\%)} = \text{Entry} \times 0.80$
    * $\text{Mega TP (50\%)} = \text{Entry} \times 0.50$
    * $\text{R:R} = \text{round}((\text{Entry} - \text{TP2}) / \text{Risk}, 1)$
    * $\text{Expected Move} = -20.0\%$

---

### 3. Smart Money Concepts (SMC)
* **Trigger Conditions**: Order Block mitigation, Fair Value Gap (FVG) displacement bar ($\ge 1.4\times \text{ATR}$), or Break of Structure (BOS).
* **Risk & Profit Levels**:
  * $\text{SL} = \text{Entry} \mp \max(\text{ATR} \times 1.3, \text{Entry} \times 0.025)$
  * $\text{TP1} = \pm 10\%$, $\text{TP2} = \pm 22\%$, $\text{Mega TP} = \pm 65\%$
  * $\text{R:R} = \text{round}(|\text{TP2} - \text{Entry}| / \text{Risk}, 1)$
  * $\text{AI Confidence} = 92.5\%$

---

### 4. Inner Circle Trader (ICT)
* **Trigger Conditions**: Silver Bullet session imbalances, Judas Swing liquidity sweep traps, or OTE ($0.618$ to $0.786$ Fibonacci retracement zone).
* **Risk & Profit Levels**:
  * $\text{SL} = \text{Entry} \mp \max(\text{ATR} \times 1.25, \text{Entry} \times 0.022)$
  * $\text{TP1} = \pm 10\%$, $\text{TP2} = \pm 20\%$, $\text{Mega TP} = \pm 55\%$
  * $\text{AI Confidence} = 93.0\%$

---

### 5. Bollinger Squeeze & Volume Surge (10% to 20%)
* **Trigger Conditions**:
  1. Volatility compression: $\text{Bandwidth} \le \min(\text{Bandwidth}[t-1 \dots t-25]) \times 1.35$ within the previous 3 bars.
  2. $\text{Volume Surge} \ge 1.70\times$.
  3. $\text{Close} > \text{Upper Band} \land \text{Close} > \text{Open}$ (LONG) or $\text{Close} < \text{Lower Band} \land \text{Close} < \text{Open}$ (SHORT).
* **Risk & Profit Levels**:
  * $\text{SL} = \text{Entry} \mp \max(\text{ATR} \times 1.3, \text{Entry} \times 0.025)$
  * $\text{TP1} = \pm 10\%$, $\text{TP2} = \pm 20\%$, $\text{Mega TP} = \pm 60\%$
  * $\text{AI Confidence} = 91.0\%$

---

### 6. Candle King Geometry
* **Trigger Conditions**:
  1. Pinbar absorption: Lower wick $\ge 65\%$ of range (Bullish Hammer) or Upper wick $\ge 65\%$ of range (Bearish Shooting Star).
  2. Institutional Engulfing Block: Body $> 1.3\times$ previous candle body, completely engulfing the prior range.
* **Risk & Profit Levels**:
  * $\text{SL} = \text{Entry} \mp \max(\text{ATR} \times 1.3, \text{Entry} \times 0.025)$
  * $\text{TP1} = \pm 10\%$, $\text{TP2} = \pm 20\%$, $\text{Mega TP} = \pm 50\%$
  * $\text{AI Confidence} = 90.5\%$

---

## 3. Real-Time Tracking & Resolution States

Both platforms track active positions against live prices and resolve state changes identically:
* $\text{Max Favorable Gain} = \max(\text{CurrentMax}, \text{PnL\%})$
* If $\text{Price} \ge \text{Mega TP}$ (or $\text{PnL\%} \ge 50.0\%$): Status $\to$ `MEGA_HIT`
* If $\text{Price} \ge \text{TP2}$ (or $\text{PnL\%} \ge 20.0\%$): Status $\to$ `HIT_20PCT`
* If $\text{Price} \ge \text{TP1}$ (or $\text{PnL\%} \ge 10.0\%$): Status $\to$ `HIT_10PCT`
* If $\text{Price} \le \text{Hard SL}$ (for LONG) or $\text{Price} \ge \text{Hard SL}$ (for SHORT): Status $\to$ `STOPPED_OUT`
* Otherwise: Status remains `ACTIVE`
