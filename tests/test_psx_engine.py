"""
Unit & Integration Test Suite for Pakistan Stock Exchange (PSX) Engine
Tests:
1. Configuration & Env Variable Handling
2. Instrument Registry & Symbol Normalization
3. Real-Time Index Feeds (KSE-100, KSE-30, KMI-30)
4. Live Equities Pricing & Detailed Company Quotes
5. Standardized OHLCV Candle DataFrames
6. Institutional Signal Generation with Circuit Breaker Buffer
7. Market Structure Trade Side-Evaluation
8. Universal Market Data Hub Routing
"""

import os
import sys
import unittest
import pandas as pd

# Add root directory to sys.path
_ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _ROOT_DIR not in sys.path:
    sys.path.insert(0, _ROOT_DIR)

from backend.config import PSX_API_KEY, PSX_API_URL, PSX_RATE_LIMIT_SEC, PSX_CACHE_TTL_SEC
from backend.psx_engine import (
    clean_psx_symbol,
    get_psx_indices,
    get_live_psx_price,
    get_psx_company_quote,
    fetch_psx_ohlcv,
    get_psx_signals,
    get_psx_board_of_high,
    get_psx_circuit_breaker_scanner,
    evaluate_psx_trade_side,
    ALL_PSX_SYMBOLS,
    MAJOR_PSX_SYMBOLS,
    PSX_INDICES,
    PSX_SECTORS
)
from backend.market_data_hub import get_live_market_price, fetch_ohlcv_universal

class TestPSXEngine(unittest.TestCase):

    def test_01_configuration_and_env(self):
        """Validates that PSX configuration parameters are loaded from environment / defaults."""
        self.assertIsInstance(PSX_API_URL, str)
        self.assertTrue(len(PSX_API_URL) > 0)
        self.assertIsInstance(PSX_RATE_LIMIT_SEC, float)
        self.assertGreater(PSX_RATE_LIMIT_SEC, 0.0)
        self.assertIsInstance(PSX_CACHE_TTL_SEC, float)
        self.assertGreater(PSX_CACHE_TTL_SEC, 0.0)

    def test_02_symbol_registry_and_normalization(self):
        """Verifies full coverage of PSX symbols and normalization logic."""
        self.assertGreaterEqual(len(ALL_PSX_SYMBOLS), 50, "Expected at least 50 listed PSX equities")
        self.assertGreaterEqual(len(PSX_INDICES), 10, "Expected at least 10 PSX indices")
        self.assertGreaterEqual(len(PSX_SECTORS), 10, "Expected at least 10 PSX sectors")

        # Test normalization
        self.assertEqual(clean_psx_symbol("PSX:OGDC"), "OGDC")
        self.assertEqual(clean_psx_symbol("PPL.KA"), "PPL")
        self.assertEqual(clean_psx_symbol("LUCK/PKR"), "LUCK")
        self.assertEqual(clean_psx_symbol("sys"), "SYS")

    def test_03_indices_feed(self):
        """Validates that KSE-100 and benchmark indices return valid prices and point changes."""
        indices = get_psx_indices()
        self.assertIsInstance(indices, list)
        self.assertGreaterEqual(len(indices), 5)

        kse100 = next((idx for idx in indices if idx["symbol"] == "KSE100"), None)
        self.assertIsNotNone(kse100, "KSE-100 index must be present in indices feed")
        self.assertGreater(kse100["current"], 50000.0, "KSE-100 current value must be realistic")
        self.assertIn("change", kse100)
        self.assertIn("pct_change", kse100)

    def test_04_live_equities_pricing_and_quotes(self):
        """Validates real-time price retrieval and company quote details."""
        # Test Top 3 equities
        for sym in ["OGDC", "PPL", "LUCK"]:
            price = get_live_psx_price(sym)
            self.assertIsInstance(price, float)
            self.assertGreater(price, 0.0, f"Price for {sym} must be positive")

            quote = get_psx_company_quote(sym)
            self.assertEqual(quote["symbol"], sym)
            self.assertIn("sector", quote)
            self.assertIn("upper_lock", quote)
            self.assertIn("lower_lock", quote)
            self.assertGreater(quote["upper_lock"], quote["price"])
            self.assertLess(quote["lower_lock"], quote["price"])

    def test_05_ohlcv_dataframe_schema(self):
        """Validates standardized OHLCV bar format for PSX equities."""
        df = fetch_psx_ohlcv("OGDC", timeframe="1h", limit=50)
        self.assertIsInstance(df, pd.DataFrame)
        self.assertGreaterEqual(len(df), 20)

        required_cols = ["timestamp", "open", "high", "low", "close", "volume"]
        for col in required_cols:
            self.assertIn(col, df.columns)
            self.assertTrue(pd.api.types.is_numeric_dtype(df[col]))

    def test_06_signals_generation_and_circuit_breakers(self):
        """Validates signal generation with micro-tight SL and circuit breaker boundaries."""
        signals = get_psx_signals(sample_symbols=["OGDC", "PPL", "LUCK", "SYS", "HBL"])
        self.assertIsInstance(signals, list)
        self.assertGreaterEqual(len(signals), 3)

        for sig in signals:
            self.assertIn(sig["side"], ["LONG", "SHORT"])
            self.assertGreater(sig["entry_price"], 0.0)
            self.assertGreater(sig["stop_loss"], 0.0)
            self.assertGreater(sig["take_profit_1"], 0.0)
            self.assertGreater(sig["take_profit_2"], 0.0)
            self.assertGreaterEqual(sig["win_rate_confidence"], 90.0)

            # Circuit breaker constraints
            if sig["side"] == "LONG":
                self.assertLess(sig["stop_loss"], sig["entry_price"])
                self.assertGreater(sig["take_profit_1"], sig["entry_price"])
                self.assertLessEqual(sig["take_profit_2"], sig["upper_lock"] + 0.50)
            else:
                self.assertGreater(sig["stop_loss"], sig["entry_price"])
                self.assertLess(sig["take_profit_1"], sig["entry_price"])
                self.assertGreaterEqual(sig["take_profit_2"], sig["lower_lock"] - 0.50)

    def test_07_board_of_high_and_circuit_scanner(self):
        """Validates the Board of High and Circuit Breaker scanner."""
        board = get_psx_board_of_high()
        self.assertIn("indices", board)
        self.assertIn("top_gainers", board)
        self.assertIn("volume_leaders", board)
        self.assertIn("sector_heatmap", board)

        cb_list = get_psx_circuit_breaker_scanner()
        self.assertIsInstance(cb_list, list)

    def test_08_trade_side_evaluation(self):
        """Validates market structure evaluation for PSX equities."""
        eval_long = evaluate_psx_trade_side("OGDC", "LONG", 131.87)
        self.assertIn("status", eval_long)
        self.assertIn(eval_long["status"], ["RIGHT SIDE OF MARKET", "WRONG SIDE OF MARKET"])
        self.assertIn("confidence_score", eval_long)
        self.assertIn("risk_to_reward", eval_long)

    def test_09_market_data_hub_routing(self):
        """Validates that the Universal Market Data Hub routes PSX symbols correctly."""
        p = get_live_market_price("OGDC")
        self.assertGreater(p, 0.0)

        df = fetch_ohlcv_universal("LUCK", timeframe="1h", limit=30)
        self.assertIsInstance(df, pd.DataFrame)
        self.assertGreaterEqual(len(df), 20)

if __name__ == "__main__":
    unittest.main()
