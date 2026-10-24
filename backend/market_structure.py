"""
Institutional Market Structure & SMC/ICT Engine
Analyzes Price Action Mechanics:
- BOS (Break of Structure)
- CHoCH (Change of Character)
- Order Blocks (Bullish & Bearish OBs)
- Fair Value Gaps (FVG)
- Premium vs. Discount Dealing Ranges & OTE (Optimal Trade Entry 61.8% - 78.6%)
"""

import pandas as pd
import numpy as np
from typing import Dict, Any, List, Optional, Tuple

def analyze_market_structure(df: pd.DataFrame, swing_lookback: int = 5) -> Dict[str, Any]:
    """
    Computes complete Smart Money Concepts (SMC) & ICT market structure telemetry:
    1. Swing Highs & Lows
    2. BOS (Break of Structure) & CHoCH (Change of Character)
    3. Order Blocks (OBs)
    4. Fair Value Gaps (FVGs)
    5. Equilibrium, Premium and Discount Zones
    """
    if df is None or len(df) < 20:
        return {
            "trend": "NEUTRAL",
            "structure_bias": "NEUTRAL",
            "swing_high": 0.0,
            "swing_low": 0.0,
            "equilibrium": 0.0,
            "zone": "EQUILIBRIUM",
            "bos_events": [],
            "choch_events": [],
            "order_blocks": [],
            "fvgs": [],
            "ote_zone": (0.0, 0.0)
        }

    highs = df["high"].values
    lows = df["low"].values
    closes = df["close"].values
    opens = df["open"].values
    n = len(df)

    # 1. Identify Swing Pivots
    swing_highs = []
    swing_lows = []
    lb = max(2, min(swing_lookback, 7))

    for i in range(lb, n - lb):
        # Local high
        if highs[i] == max(highs[i - lb : i + lb + 1]):
            swing_highs.append({"index": i, "price": float(highs[i])})
        # Local low
        if lows[i] == min(lows[i - lb : i + lb + 1]):
            swing_lows.append({"index": i, "price": float(lows[i])})

    latest_price = float(closes[-1])
    recent_sh = swing_highs[-1]["price"] if swing_highs else float(highs.max())
    recent_sl = swing_lows[-1]["price"] if swing_lows else float(lows.min())

    # Dealing Range & Equilibrium
    range_span = max(1e-6, recent_sh - recent_sl)
    equilibrium = recent_sl + (range_span * 0.50)
    ote_low = recent_sl + (range_span * 0.618)
    ote_high = recent_sl + (range_span * 0.786)

    if latest_price > equilibrium + (range_span * 0.05):
        zone = "PREMIUM (Sell Side Optimal)"
    elif latest_price < equilibrium - (range_span * 0.05):
        zone = "DISCOUNT (Buy Side Optimal)"
    else:
        zone = "EQUILIBRIUM (Fair Value)"

    # 2. Detect BOS & CHoCH
    bos_events = []
    choch_events = []
    current_trend = "BULLISH" if closes[-1] > closes[0] else "BEARISH"

    if len(swing_highs) >= 2 and len(swing_lows) >= 2:
        prev_sh = swing_highs[-2]["price"]
        prev_sl = swing_lows[-2]["price"]

        # Bullish BOS: Price broke above previous swing high during uptrend
        if latest_price > recent_sh:
            bos_events.append({
                "type": "BULLISH_BOS",
                "label": "Bullish BOS (+MS)",
                "level": recent_sh,
                "description": "Price broke structure upward with institutional impulse."
            })
        elif latest_price < recent_sl:
            bos_events.append({
                "type": "BEARISH_BOS",
                "label": "Bearish BOS (-MS)",
                "level": recent_sl,
                "description": "Price broke structure downward with displacement."
            })

        # CHoCH (Change of Character): trend reversal across opposing key swing
        if current_trend == "BEARISH" and latest_price > prev_sh:
            choch_events.append({
                "type": "BULLISH_CHOCH",
                "label": "Bullish CHoCH (Reversal)",
                "level": prev_sh,
                "description": "Shift from bearish order flow to bullish expansion."
            })
        elif current_trend == "BULLISH" and latest_price < prev_sl:
            choch_events.append({
                "type": "BEARISH_CHOCH",
                "label": "Bearish CHoCH (Reversal)",
                "level": prev_sl,
                "description": "Shift from bullish order flow to bearish distribution."
            })

    # 3. Detect Order Blocks (OBs)
    order_blocks = []
    for i in range(2, n - 1):
        # Bullish OB: Bearish candle followed by large bullish displacement
        if closes[i - 1] < opens[i - 1] and closes[i] > opens[i]:
            body_prev = abs(closes[i - 1] - opens[i - 1])
            body_curr = abs(closes[i] - opens[i])
            if body_curr >= body_prev * 1.5 and closes[i] > highs[i - 1]:
                order_blocks.append({
                    "type": "BULLISH_OB",
                    "top": float(highs[i - 1]),
                    "bottom": float(lows[i - 1]),
                    "index": i - 1,
                    "mitigated": latest_price < lows[i - 1]
                })

        # Bearish OB: Bullish candle followed by large bearish displacement
        elif closes[i - 1] > opens[i - 1] and closes[i] < opens[i]:
            body_prev = abs(closes[i - 1] - opens[i - 1])
            body_curr = abs(closes[i] - opens[i])
            if body_curr >= body_prev * 1.5 and closes[i] < lows[i - 1]:
                order_blocks.append({
                    "type": "BEARISH_OB",
                    "top": float(highs[i - 1]),
                    "bottom": float(lows[i - 1]),
                    "index": i - 1,
                    "mitigated": latest_price > highs[i - 1]
                })

    # Keep the most recent 6 active order blocks
    active_obs = [ob for ob in order_blocks if not ob["mitigated"]][-6:]

    # 4. Detect Fair Value Gaps (FVG)
    fvgs = []
    for i in range(1, n - 1):
        # Bullish FVG: Low of candle[i+1] > High of candle[i-1]
        if lows[i + 1] > highs[i - 1]:
            gap_size = lows[i + 1] - highs[i - 1]
            if gap_size > (latest_price * 0.001):
                fvgs.append({
                    "type": "BULLISH_FVG",
                    "top": float(lows[i + 1]),
                    "bottom": float(highs[i - 1]),
                    "size": float(gap_size),
                    "index": i
                })
        # Bearish FVG: High of candle[i+1] < Low of candle[i-1]
        elif highs[i + 1] < lows[i - 1]:
            gap_size = lows[i - 1] - highs[i + 1]
            if gap_size > (latest_price * 0.001):
                fvgs.append({
                    "type": "BEARISH_FVG",
                    "top": float(lows[i - 1]),
                    "bottom": float(highs[i + 1]),
                    "size": float(gap_size),
                    "index": i
                })

    active_fvgs = fvgs[-6:]

    # Overall Structural Bias
    if choch_events and choch_events[-1]["type"] == "BULLISH_CHOCH":
        structure_bias = "STRONG BULLISH (CHoCH Shift)"
    elif choch_events and choch_events[-1]["type"] == "BEARISH_CHOCH":
        structure_bias = "STRONG BEARISH (CHoCH Shift)"
    elif bos_events and bos_events[-1]["type"] == "BULLISH_BOS":
        structure_bias = "BULLISH CONTINUATION (BOS)"
    elif bos_events and bos_events[-1]["type"] == "BEARISH_BOS":
        structure_bias = "BEARISH CONTINUATION (BOS)"
    else:
        structure_bias = "CONSOLIDATION / BALANCED"

    return {
        "trend": current_trend,
        "structure": structure_bias,
        "structure_bias": structure_bias,
        "swing_high": recent_sh,
        "swing_low": recent_sl,
        "equilibrium": round(equilibrium, 4) if equilibrium < 10 else round(equilibrium, 2),
        "zone": zone,
        "is_discount": (zone == "DISCOUNT"),
        "is_ote": bool(ote_low <= latest_price <= ote_high) if ote_low > 0 else False,
        "ote_zone": (round(ote_low, 4) if ote_low < 10 else round(ote_low, 2), round(ote_high, 4) if ote_high < 10 else round(ote_high, 2)),
        "bos_events": bos_events,
        "choch_events": choch_events,
        "order_blocks": active_obs,
        "fvgs": active_fvgs
    }

__all__ = ["analyze_market_structure"]
