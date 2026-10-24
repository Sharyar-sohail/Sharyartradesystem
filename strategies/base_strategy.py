"""
Base Strategy Module & Vectorized Quantitative Utilities
Provides high-performance technical calculations and standardized setup schemas
used across all independent strategy files.
"""

import math
import pandas as pd
import numpy as np
from typing import Dict, Any, Tuple, Optional, List

# =====================================================================
# TECHNICAL INDICATOR UTILITIES (Vectorized NumPy Computations)
# =====================================================================

def compute_ema(series: pd.Series, span: int) -> pd.Series:
    """Computes Exponential Moving Average."""
    return series.ewm(span=span, adjust=False).mean()

def compute_sma(series: pd.Series, window: int) -> pd.Series:
    """Computes Simple Moving Average."""
    return series.rolling(window=window).mean()

def compute_atr(df: pd.DataFrame, period: int = 14) -> pd.Series:
    """Computes Average True Range."""
    high = df["high"]
    low = df["low"]
    close = df["close"]
    prev_close = close.shift(1)

    tr1 = high - low
    tr2 = (high - prev_close).abs()
    tr3 = (low - prev_close).abs()
    tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
    return tr.rolling(window=period).mean()

def compute_rsi(series: pd.Series, period: int = 14) -> pd.Series:
    """Computes Relative Strength Index."""
    delta = series.diff()
    gain = delta.where(delta > 0, 0.0)
    loss = -delta.where(delta < 0, 0.0)

    avg_gain = gain.ewm(alpha=1/period, min_periods=period, adjust=False).mean()
    avg_loss = loss.ewm(alpha=1/period, min_periods=period, adjust=False).mean()

    rs = avg_gain / (avg_loss + 1e-10)
    return 100 - (100 / (1 + rs))

def compute_macd(series: pd.Series, fast: int = 12, slow: int = 26, signal: int = 9):
    """Computes MACD line, Signal line, and Histogram."""
    ema_fast = compute_ema(series, fast)
    ema_slow = compute_ema(series, slow)
    macd_line = ema_fast - ema_slow
    signal_line = compute_ema(macd_line, signal)
    histogram = macd_line - signal_line
    return macd_line, signal_line, histogram

def compute_bollinger_bands(series: pd.Series, period: int = 20, num_std: float = 2.0):
    """Computes Bollinger Bands and Bandwidth."""
    sma = series.rolling(window=period).mean()
    std = series.rolling(window=period).std()
    upper = sma + (std * num_std)
    lower = sma - (std * num_std)
    bandwidth = (upper - lower) / (sma + 1e-10)
    return upper, sma, lower, bandwidth

def compute_supertrend(df: pd.DataFrame, period: int = 10, multiplier: float = 3.0):
    """Computes SuperTrend indicator line and direction series."""
    atr = compute_atr(df, period)
    hl2 = (df["high"] + df["low"]) / 2
    basic_upper = hl2 + (multiplier * atr)
    basic_lower = hl2 - (multiplier * atr)

    final_upper = basic_upper.copy()
    final_lower = basic_lower.copy()
    supertrend = pd.Series(index=df.index, dtype=float)
    trend = pd.Series(index=df.index, dtype=int)

    close = df["close"].values
    n = len(df)
    
    for i in range(1, n):
        if basic_upper.iloc[i] < final_upper.iloc[i-1] or close[i-1] > final_upper.iloc[i-1]:
            final_upper.iloc[i] = basic_upper.iloc[i]
        else:
            final_upper.iloc[i] = final_upper.iloc[i-1]

        if basic_lower.iloc[i] > final_lower.iloc[i-1] or close[i-1] < final_lower.iloc[i-1]:
            final_lower.iloc[i] = basic_lower.iloc[i]
        else:
            final_lower.iloc[i] = final_lower.iloc[i-1]

    for i in range(1, n):
        if supertrend.iloc[i-1] == final_upper.iloc[i-1]:
            if close[i] > final_upper.iloc[i]:
                supertrend.iloc[i] = final_lower.iloc[i]
                trend.iloc[i] = 1
            else:
                supertrend.iloc[i] = final_upper.iloc[i]
                trend.iloc[i] = -1
        else:
            if close[i] < final_lower.iloc[i]:
                supertrend.iloc[i] = final_upper.iloc[i]
                trend.iloc[i] = -1
            else:
                supertrend.iloc[i] = final_lower.iloc[i]
                trend.iloc[i] = 1

    return supertrend, trend

# =====================================================================
# STANDARDIZED SETUP FORMATTER
# =====================================================================

def format_setup_payload(
    strategy_name: str,
    direction: str,
    entry_p: float,
    sl_p: float,
    tp10: float,
    tp20: float,
    tp_mega: float,
    risk_reward_ratio: float,
    expected_move_pct: float,
    ai_confidence_pct: float,
    bandwidth: float,
    vol_surge: float,
    reason: str,
    is_mega_move: bool = False
) -> Dict[str, Any]:
    """Generates an institutional quantitative setup dictionary."""
    is_long = (direction == "LONG")
    risk_pct = abs((entry_p - sl_p) / (entry_p + 1e-10)) * 100.0

    return {
        "strategy": strategy_name,
        "direction": direction,
        "entry_price": float(entry_p),
        "stop_loss": float(sl_p),
        "target_10pct": float(tp10),
        "target_20pct": float(tp20),
        "target_mega": float(tp_mega),
        "risk_reward_ratio": round(float(risk_reward_ratio), 2),
        "expected_move_pct": round(float(expected_move_pct), 2),
        "ai_confidence_pct": round(float(ai_confidence_pct), 1),
        "risk_percentage": round(float(risk_pct), 2),
        "bandwidth": float(bandwidth),
        "volume_surge": float(vol_surge),
        "reason": reason,
        "is_mega_move": bool(is_mega_move)
    }
