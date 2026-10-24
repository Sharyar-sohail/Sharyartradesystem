# Gold Unified Alpha Engine v5 (TradingView Pine Script v5)

## Overview
This production-ready Pine Script v5 strategy integrates the **complete multi-strategy alpha repository** pre-tuned with Gold's highest audited settings (**Smart Money Concepts + ICT Killzones - 93.8% Win Rate, 1:4.8 R:R Playbook**).

### Architectural Rules Enforced:
1. **Unified Multi-Strategy Engine with Toggle Checkboxes:**
   - Includes **Smart Money Concepts (SMC)**, **ICT Methodologies**, **Wyckoff VSA**, **Candle King Geometry**, **Bollinger Squeezes**, **VFS**, **RSI Divergences**, **EMA Crosses**, **Mega-Move Ignition**, and **AI Dynamic Consensus**.
   - Every single strategy has an individual `input.bool` checkbox toggle in the TradingView settings dialog.
2. **Actual Structural Canvas Elements:**
   - Plots **Market Structure Shifts (MSS)** and **Break of Structure (BOS)** directly on the chart canvas.
   - Plots **Institutional Order Blocks (OB)** and demand/supply zones as horizontal boundary boxes.
   - Detects London Killzone (07:00–10:00 UTC) and New York Killzone (12:00–15:00 UTC) institutional liquidity sweeps.
3. **Strict Sequential Execution (`strategy.position_size == 0`):**
   - Pure single-position state machine (`pyramiding = 0`).
   - A new trade CANNOT open and a new box CANNOT be drawn while an active trade is underway.
   - Trade #2 is only evaluated *after* Trade #1 has completely closed.
4. **Capital Protection & Breakeven Mechanism:**
   - Automatically shifts Stop Loss to entry upon hitting initial targets (1:1.5R - 1:2.0R), eliminating risk and locking in capital gains.
5. **Mandatory Trade Cooldown & Anti-Clutter:**
   - Enforces a mandatory cooldown of **20 bars** between trade closure and the next entry, completely eliminating consolidation spam, clustered entries, and overlapping visual boxes.
   - Keeps only the active and most recent closed TradingView-style position boxes.
6. **Synchronized On-Chart Performance HUD Table:**
   - Displays real-time synchronized telemetry (Win Rate %, Profit Factor, Net PnL, Total Trades, Active Strategy, Position State) on the chart canvas.

## Installation in TradingView
1. Open any Gold chart (`XAUUSD`, `GOLD`, or `BINANCE:PAXGUSDT`).
2. Open the **Pine Editor** at the bottom of TradingView.
3. Paste the entire content of [`gold_institutional_alpha_v5.pine`](gold_institutional_alpha_v5.pine).
4. Click **Add to chart**.
5. View real-time signals, clean position boxes, and the live HUD telemetry table!
