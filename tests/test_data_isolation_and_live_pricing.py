"""
Unit Tests for Data Isolation and Real-Time Live Pricing.
Verifies:
1. PSX module isolation (Zero crypto, zero forex, zero gold).
2. Forex module isolation (Zero crypto, zero gold/metals, zero PSX).
3. Live Crypto Pricing integrity (Real market BTC/ETH price, multi-source fallback, no hardcoded fake prices).
4. MarketDataHub strict routing.
"""

import unittest
from backend.psx_engine import (
    is_psx_symbol,
    validate_psx_symbol,
    clean_psx_symbol,
    get_psx_signals,
    ALL_PSX_ALLOWED_SYMBOLS,
    get_live_psx_price
)
from backend.forex_engine import (
    is_forex_symbol,
    validate_forex_symbol,
    forex_engine,
    FOREX_CURRENCY_PAIRS,
    FOREX_WHITELIST
)
from backend.binance_client import binance_client
from backend.market_data_hub import (
    get_live_market_price,
    get_live_forex_price,
    get_live_crypto_price_with_metadata
)


class TestPSXDataIsolation(unittest.TestCase):
    """Test Suite to ensure the PSX module is 100% isolated from crypto, forex, and gold."""

    def test_psx_symbol_validation(self):
        # Valid PSX symbols
        self.assertTrue(is_psx_symbol("OGDC"))
        self.assertTrue(is_psx_symbol("PPL"))
        self.assertTrue(is_psx_symbol("LUCK"))
        self.assertTrue(is_psx_symbol("SYS"))
        self.assertTrue(is_psx_symbol("KSE100"))
        self.assertTrue(is_psx_symbol("KSE-100"))

        # Non-PSX symbols MUST be rejected
        self.assertFalse(is_psx_symbol("BTC/USDT"))
        self.assertFalse(is_psx_symbol("BTCUSDT"))
        self.assertFalse(is_psx_symbol("ETH/USDT"))
        self.assertFalse(is_psx_symbol("XAU/USD"))
        self.assertFalse(is_psx_symbol("GOLD"))
        self.assertFalse(is_psx_symbol("EUR/USD"))
        self.assertFalse(is_psx_symbol("USD/JPY"))

    def test_psx_allowed_symbols_whitelist(self):
        # Verify no crypto or forex in the master PSX symbol pool
        for sym in ALL_PSX_ALLOWED_SYMBOLS:
            self.assertNotIn("BTC", sym)
            self.assertNotIn("ETH", sym)
            self.assertNotIn("USDT", sym)
            self.assertNotIn("XAU", sym)
            self.assertNotIn("EUR", sym)

    def test_psx_live_price_rejection(self):
        # Non-PSX queries to PSX live price must return 0.0 or reject
        price_btc = get_live_psx_price("BTC/USDT")
        self.assertEqual(price_btc, 0.0)

        price_gold = get_live_psx_price("XAU/USD")
        self.assertEqual(price_gold, 0.0)

        price_eur = get_live_psx_price("EUR/USD")
        self.assertEqual(price_eur, 0.0)

    def test_psx_signals_filtering(self):
        # Pass mixed symbols into signals method
        mixed = ["OGDC", "BTC/USDT", "LUCK", "XAU/USD", "EUR/USD", "PPL"]
        signals = get_psx_signals(sample_symbols=mixed)
        for sig in signals:
            sym = sig.get("symbol")
            self.assertTrue(is_psx_symbol(sym), f"Non-PSX symbol found in PSX signals: {sym}")
            self.assertNotIn("BTC", sym)
            self.assertNotIn("ETH", sym)
            self.assertNotIn("XAU", sym)


class TestForexDataIsolation(unittest.TestCase):
    """Test Suite to ensure the Forex module is 100% isolated to currency pairs only."""

    def test_forex_symbol_validation(self):
        # Valid Forex pairs
        self.assertTrue(is_forex_symbol("EUR/USD"))
        self.assertTrue(is_forex_symbol("GBP/USD"))
        self.assertTrue(is_forex_symbol("USD/JPY"))
        self.assertTrue(is_forex_symbol("EURUSD"))
        self.assertTrue(is_forex_symbol("USDJPY"))

        # Non-Forex symbols MUST be rejected
        self.assertFalse(is_forex_symbol("BTC/USDT"))
        self.assertFalse(is_forex_symbol("ETH/USDT"))
        self.assertFalse(is_forex_symbol("XAU/USD"))  # Metals removed from Forex
        self.assertFalse(is_forex_symbol("XAG/USD"))  # Silver removed from Forex
        self.assertFalse(is_forex_symbol("OGDC"))
        self.assertFalse(is_forex_symbol("LUCK"))

    def test_forex_pair_whitelist(self):
        for pair in FOREX_CURRENCY_PAIRS:
            self.assertNotIn("BTC", pair)
            self.assertNotIn("ETH", pair)
            self.assertNotIn("XAU", pair)
            self.assertNotIn("XAG", pair)
            self.assertTrue(is_forex_symbol(pair))

    def test_forex_engine_live_price_rejection(self):
        # Crypto and PSX must be completely rejected by forex engine
        self.assertIsNone(forex_engine.get_live_price("BTC/USDT"))
        self.assertIsNone(forex_engine.get_live_price("ETH/USDT"))
        self.assertIsNone(forex_engine.get_live_price("OGDC"))
        # Pure forex pricing must strictly reject metals and crypto
        self.assertIsNone(forex_engine.get_forex_price("XAU/USD"))
        self.assertIsNone(forex_engine.get_forex_price("BTC/USDT"))

    def test_forex_signals_filtering(self):
        mixed = ["EUR/USD", "BTC/USDT", "GBP/USD", "XAU/USD", "OGDC"]
        signals = forex_engine.get_forex_signals(pairs=mixed)
        for sig in signals:
            sym = sig.get("symbol")
            self.assertTrue(is_forex_symbol(sym), f"Non-Forex symbol in Forex signals: {sym}")
            self.assertNotIn("BTC", sym)
            self.assertNotIn("XAU", sym)


class TestLiveCryptoPricing(unittest.TestCase):
    """Test Suite to ensure Bitcoin, Ethereum, and crypto pricing are real-time, live, and not fake."""

    def setUp(self):
        self.client = binance_client

    def test_btc_price_is_real_and_live(self):
        btc_meta = self.client.get_ticker_price_with_metadata("BTCUSDT")
        price = btc_meta.get("price", 0.0)
        status = btc_meta.get("status")
        source = btc_meta.get("source")

        # Crucial: price must NOT be the old fake 68,500.0
        self.assertNotEqual(price, 68500.0, "BTC price is returning hardcoded 68,500 fake seed price!")
        # Current real market price is > 70,000 (around 80,000)
        self.assertGreater(price, 70000.0, f"BTC price {price} is unreasonably low or fake!")
        self.assertEqual(status, "LIVE")
        self.assertIn(source, ["Binance", "COINBASE_PUBLIC", "KRAKEN_PUBLIC", "BINANCE_GLOBAL", "COINGECKO_PUBLIC"])

    def test_eth_price_is_real_and_live(self):
        eth_meta = self.client.get_ticker_price_with_metadata("ETHUSDT")
        price = eth_meta.get("price", 0.0)
        status = eth_meta.get("status")

        # ETH should be > 1,500 and not 0
        self.assertGreater(price, 1500.0, f"ETH price {price} is unreasonably low!")
        self.assertEqual(status, "LIVE")

    def test_invalid_symbol_returns_explicit_unavailable(self):
        bad_meta = self.client.get_ticker_price_with_metadata("COMPLETELY_FAKE_COIN_12345")
        self.assertEqual(bad_meta.get("status"), "UNAVAILABLE")
        self.assertTrue(bad_meta.get("price") is None or bad_meta.get("price") == 0.0)


class TestMarketDataHubRouting(unittest.TestCase):
    """Test Suite to ensure the unified hub routes properly without cross-contamination."""

    def test_crypto_routing(self):
        btc_price = get_live_market_price("BTC/USDT", category="crypto")
        self.assertGreater(btc_price, 70000.0)

    def test_psx_routing_isolation(self):
        # Requesting crypto through psx category should return 0.0
        btc_psx = get_live_market_price("BTC/USDT", category="psx")
        self.assertEqual(btc_psx, 0.0)

    def test_forex_routing_isolation(self):
        # Requesting crypto through forex should return None
        fx_btc = get_live_forex_price("BTC/USDT")
        self.assertIsNone(fx_btc)


if __name__ == "__main__":
    unittest.main()
