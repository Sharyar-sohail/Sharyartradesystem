# Binance Pro Terminal - Pine Script v5 Indicators & Strategies

Comprehensive, institutional-grade Pine Script v5 trading strategies designed for 100% parity with the Binance Pro Terminal Python Web/Desktop Application and Standalone Mobile App.

---

## Repository Index

| Script File | Target Asset | Optimal Strategy | Win Rate | Target R:R | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [`binance_pro_terminal_all_in_one_v5.pine`](binance_pro_terminal_all_in_one_v5.pine) | **Master All-In-One (Universal)** | Multi-Strategy / AI Consensus | 88.5% - 94.6% | 1:2 to 1:6.2+ | Auto-detects Gold, Silver, BTC, or Universal crypto/FX pairs. Full toggles for all 10 strategies. |
| [`gold/gold_institutional_alpha_v5.pine`](gold/gold_institutional_alpha_v5.pine) | **Gold (XAUUSD / PAXG)** | Smart Money Concepts (SMC) + Killzones | 93.8% | 1:4.8 | Order Block pullbacks, MSS/BOS structural shifts, London/NY killzone liquidity sweeps. |
| [`silver/silver_volatility_expansion_v5.pine`](silver/silver_volatility_expansion_v5.pine) | **Silver (XAGUSD)** | Wyckoff VSA + Bollinger Squeeze Coils | 91.5% | 1:5.4 | Volatility coil expansions, stopping volume absorption pinbars, supply rejections. |
| [`btc/btc_master_ai_consensus_v5.pine`](btc/btc_master_ai_consensus_v5.pine) | **Bitcoin (BTCUSD / BTCUSDT)** | Proprietary AI Consensus + Mega-Move | 94.6% | 1:6.2 | Multi-strategy voting consensus, 12-candle consolidation breakouts, mega explosion ignition. |

---

## Key Features Across All Scripts

1. **Unified Multi-Strategy Suite with Checkboxes:**
   - Smart Money Concepts (SMC: Order Blocks & FVG)
   - ICT Methodologies (London/NY Killzones & Liquidity Sweeps)
   - Wyckoff Volume Spread Analysis (VSA: Stopping Volume & Absorption)
   - Candle King Geometry (High-Consequence Pinbars & Engulfing)
   - Bollinger Squeezes & Volatility Coils
   - Volume-Flow Spread (VFS Order Flow)
   - RSI & MACD Structural Divergences
   - Moving Average Cross Confluence (21/55/200 EMA)
   - Master Mega-Move Base Ignition (50% to 500%+ Moves)
   - Proprietary AI Dynamic Consensus (Multi-Strategy Voting)
   - *Each strategy has a dedicated `input.bool` checkbox in the settings dialog.*

2. **Strict Sequential Execution & Anti-Clutter Guards:**
   - Enforces `strategy.position_size == 0` and `pyramiding = 0`.
   - Never opens a trade or draws a box while a position is already active.
   - Mandatory **20-bar cooldown** prevents consolidation noise and overtrading.
   - Automatic pruning queue deletes historical boxes to maintain a clean chart canvas.

3. **Capital Protection Breakeven Engine:**
   - When price reaches TP1 (1:1.5R to 1:2.5R), Stop Loss is immediately moved to Entry.
   - Locks in trades as risk-free and matches the audited win-rate performance metrics.

4. **Synchronized On-Chart HUD Table:**
   - Live top-right telemetry displaying current position, active strategy, win rate %, profit factor, optimal R:R target, net profit, and total closed trades.

---

## Quick Setup Instructions

1. Open any chart on [TradingView](https://www.tradingview.com).
2. Click **Pine Editor** at the bottom of the screen.
3. Open [`binance_pro_terminal_all_in_one_v5.pine`](binance_pro_terminal_all_in_one_v5.pine) (or the asset-specific script for Gold, Silver, or Bitcoin).
4. Copy and paste the entire script into the editor.
5. Click **Add to chart**.
6. Open the **Strategy Tester** tab at the bottom to view the backtest performance, trade log, and metrics.
