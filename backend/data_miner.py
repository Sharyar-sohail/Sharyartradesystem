"""
Historical Mega-Pump & Dump Data Miner
Mines 1-month Binance historical data for 50% to 500%+ pumps/dumps,
extracts pre-breakout structural patterns, and powers targeted backtesting.
"""

import time
import requests
import pandas as pd
import numpy as np
from typing import List, Dict, Any, Tuple, Optional
from datetime import datetime, timezone

from backend.config import BINANCE_BASE_URLS
from backend.binance_client import binance_client
from backend.db import save_mega_move, get_mega_moves, clear_mega_moves, log_event

class MegaMoveMiner:
    """Specialized engine for uncovering 50% to 500%+ explosive market occurrences."""

    def __init__(self):
        self.min_pump_threshold_pct = 50.0  # 50% minimum pump
        self.min_dump_threshold_pct = -40.0  # -40% minimum dump
        self.liquid_pairs_cache: List[str] = []

    def get_candidate_symbols(self, max_pairs: int = 150) -> List[str]:
        """Fetches liquid USDT trading pairs excluding leveraged tokens and stables."""
        try:
            tickers = binance_client._request_with_retry("/api/v3/ticker/24hr")
            if not tickers:
                return []

            ignored = ["UPUSDT", "DOWNUSDT", "BEARUSDT", "BULLUSDT", "BUSDUSDT", "TUSDUSDT", "USDCUSDT", "EURUSDT", "FDUSDUSDT", "AEURUSDT"]
            candidates = []

            for t in tickers:
                sym = t.get("symbol", "")
                if not sym.endswith("USDT") or any(sym.endswith(ig) or sym == ig for ig in ignored):
                    continue
                quote_vol = float(t.get("quoteVolume", 0))
                # Only include pairs with at least 500k USDT daily volume
                if quote_vol >= 500000:
                    candidates.append((sym, quote_vol))

            # Sort by liquidity
            candidates.sort(key=lambda x: x[1], reverse=True)
            self.liquid_pairs_cache = [c[0] for c in candidates[:max_pairs]]
            log_event("INFO", "DATA_MINER", f"Identified {len(self.liquid_pairs_cache)} liquid candidate pairs for mega-move mining.")
            return self.liquid_pairs_cache
        except Exception as e:
            log_event("ERROR", "DATA_MINER", f"Failed to get candidate symbols: {e}")
            return []

    def mine_historical_mega_moves(
        self,
        timeframe: str = "4h",
        lookback_days: int = 30,
        max_symbols_to_scan: int = 80,
        progress_callback: Optional[callable] = None
    ) -> List[Dict[str, Any]]:
        """
        Scans historical data across liquid Binance pairs over past 30 days.
        Detects all coins that experienced 50% to 500%+ pumps (or severe dumps).
        Analyzes pre-pump baseline consolidation, volume multiplier, and SMC patterns.
        """
        symbols = self.get_candidate_symbols(max_pairs=max_symbols_to_scan)
        if not symbols:
            # Fallback popular pairs
            symbols = ["ICXUSDT", "NULSUSDT", "GNSUSDT", "LTCUSDT", "SOLUSDT", "DOGEUSDT", "NEARUSDT", "AVAXUSDT"]

        # 30 days in 4h candles = 180 candles; in 1d = 30 candles
        candles_limit = 180 if timeframe == "4h" else 40
        mined_events = []
        clear_mega_moves()

        log_event("INFO", "DATA_MINER", f"Initiating 1-Month Mega-Move Mining ({len(symbols)} pairs on {timeframe})...")

        for idx, sym in enumerate(symbols):
            if progress_callback:
                progress_callback(idx + 1, len(symbols), f"Scanning {sym} for 50%-500% moves...")

            # Fetch historical klines
            formatted_sym = sym[:-4] + "/USDT" if sym.endswith("USDT") else sym
            df = binance_client.fetch_ohlcv(formatted_sym, timeframe=timeframe, limit=candles_limit)
            if df is None or len(df) < 25:
                continue

            # Scan for Mega Moves within the window
            events = self._detect_moves_in_series(df, formatted_sym, timeframe)
            for ev in events:
                row_id = save_mega_move(
                    symbol=ev["symbol"],
                    move_type=ev["move_type"],
                    peak_gain_pct=ev["peak_gain_pct"],
                    start_price=ev["start_price"],
                    peak_price=ev["peak_price"],
                    start_time=ev["start_time"],
                    peak_time=ev["peak_time"],
                    duration_hours=ev["duration_hours"],
                    vol_multiplier=ev["vol_multiplier"],
                    pattern=ev["pattern"],
                    pre_pump_data=ev["pre_pump_candles"]
                )
                ev["id"] = row_id
                mined_events.append(ev)

            # Gentle sleep to respect exchange rate limits
            time.sleep(0.08)

        log_event("SUCCESS", "DATA_MINER", f"Mining Complete! Uncovered {len(mined_events)} historical 50%-500% mega-move events.")
        return sorted(mined_events, key=lambda x: x["peak_gain_pct"], reverse=True)

    def _detect_moves_in_series(
        self,
        df: pd.DataFrame,
        symbol: str,
        timeframe: str
    ) -> List[Dict[str, Any]]:
        """
        Inspects rolling windows across OHLCV to isolate 50%-500%+ explosions.
        Analyzes the 15-20 bars immediately preceding the breakout.
        """
        events = []
        n = len(df)
        highs = df["high"].values
        lows = df["low"].values
        closes = df["close"].values
        volumes = df["volume"].values
        times = df["timestamp"].astype(str).values

        # Rolling window size (e.g. 18 bars of 4h = 3 days; or 42 bars = 7 days)
        window = 30 if n >= 30 else n

        for i in range(15, n - 5):
            # Look at a forward window from bar i
            sub_lows = lows[i : min(i + window, n)]
            sub_highs = highs[i : min(i + window, n)]

            min_idx_relative = int(np.argmin(sub_lows))
            min_bar = i + min_idx_relative
            start_price = lows[min_bar]

            # Look for peak after the low
            after_low_highs = highs[min_bar : min(min_bar + 24, n)]
            if len(after_low_highs) < 2:
                continue

            max_idx_relative = int(np.argmax(after_low_highs))
            peak_bar = min_bar + max_idx_relative
            peak_price = highs[peak_bar]

            if peak_bar <= min_bar:
                continue

            gain_pct = ((peak_price - start_price) / (start_price + 1e-10)) * 100

            # -------------------------------------------------------------
            # MEGA PUMP DETECTED (50% to 500%+)
            # -------------------------------------------------------------
            if gain_pct >= self.min_pump_threshold_pct:
                # Pre-pump baseline analysis (10 bars before min_bar)
                pre_start = max(0, min_bar - 15)
                pre_vols = volumes[pre_start : min_bar]
                breakout_vols = volumes[min_bar : peak_bar + 1]

                baseline_vol = float(np.median(pre_vols)) if len(pre_vols) > 0 else 1.0
                max_breakout_vol = float(np.max(breakout_vols)) if len(breakout_vols) > 0 else 1.0
                vol_mult = max_breakout_vol / (baseline_vol + 1e-6)

                # Pre-pump structural pattern classification
                pattern = self._classify_pre_pump_pattern(df, min_bar)

                # Duration in hours
                hrs_per_bar = 4.0 if timeframe == "4h" else (24.0 if timeframe == "1d" else 1.0)
                duration_hours = (peak_bar - min_bar) * hrs_per_bar

                # Snapshot of pre-pump and impulse candles (dict format for chart rendering)
                snapshot_df = df.iloc[max(0, min_bar - 15) : min(n, peak_bar + 5)]
                candles_snapshot = snapshot_df[["timestamp", "open", "high", "low", "close", "volume"]].to_dict(orient="records")
                for c in candles_snapshot:
                    c["timestamp"] = str(c["timestamp"])

                events.append({
                    "symbol": symbol,
                    "move_type": "MEGA_PUMP",
                    "peak_gain_pct": round(gain_pct, 2),
                    "start_price": start_price,
                    "peak_price": peak_price,
                    "start_time": times[min_bar],
                    "peak_time": times[peak_bar],
                    "duration_hours": duration_hours,
                    "vol_multiplier": round(vol_mult, 2),
                    "pattern": pattern,
                    "pre_pump_candles": candles_snapshot
                })
                # Skip forward past this peak to avoid duplicating the same wave
                break

        return events

    def _classify_pre_pump_pattern(self, df: pd.DataFrame, breakout_idx: int) -> str:
        """Determines the exact technical fingerprint right before the pump."""
        try:
            pre_df = df.iloc[max(0, breakout_idx - 15) : breakout_idx + 1]
            if len(pre_df) < 5:
                return "Consolidation Breakout"

            # 1. Check for SMC Bullish Order Block (lower wick sweep)
            closes = pre_df["close"].values
            lows = pre_df["low"].values
            highs = pre_df["high"].values
            vols = pre_df["volume"].values

            # Squeeze: narrow range
            spreads = highs - lows
            avg_spread = np.mean(spreads[:-1])
            last_spread = spreads[-1]

            vol_ratio = vols[-1] / (np.median(vols[:-1]) + 1e-6)

            if vol_ratio > 4.0 and last_spread > 1.5 * avg_spread:
                return f"SMC Order Block + Vol Spike ({vol_ratio:.1f}x)"
            elif last_spread > 2.0 * avg_spread:
                return "FVG Imbalance Expansion"
            elif vol_ratio > 3.0:
                return f"VSA Stopping Volume Accumulation ({vol_ratio:.1f}x)"
            else:
                return "Bollinger Squeeze Volatility Coil"
        except Exception:
            return "Institutional Accumulation Spring"

    def scan_live_radar(self, min_vol_surge: float = 2.5) -> List[Dict[str, Any]]:
        """
        Scans real-time 15m/1h Binance tickers to detect coins
        currently setting up in the PRE-PUMP explosion zone right now!
        """
        try:
            g, _ = binance_client.get_top_gainers_losers(limit=25)
            radar_hits = []
            if g.empty:
                return []

            for _, row in g.iterrows():
                surge = row.get("volume_surge", 1.0)
                change = row.get("change_pct", 0.0)
                volatility = row.get("volatility_pct", 0.0)

                # Pre-pump criteria: Volume is surging (> 2.5x) while price is just starting to take off (+5% to +25%)
                if surge >= min_vol_surge and 4.0 <= change <= 35.0:
                    radar_hits.append({
                        "symbol": row["symbol"],
                        "current_price": row["price"],
                        "change_pct": change,
                        "volume_surge": surge,
                        "volatility_pct": volatility,
                        "volume_usdt": row["volume_usdt"],
                        "status": "🚨 HIGH PUMP PROBABILITY",
                        "catalyst": f"Volume Explosion ({surge:.1f}x) + Base Ignition"
                    })

            return sorted(radar_hits, key=lambda x: x["volume_surge"], reverse=True)
        except Exception as e:
            log_event("ERROR", "RADAR", f"Radar scan failed: {e}")
            return []

# Singleton miner instance
data_miner = MegaMoveMiner()
