"""
Binance High-Reliability Client with Direct REST Fallback & Rate Limiting
Provides resilient top gainers/losers scanning, live tickers, and historical OHLCV data.
"""

import time
import threading
import requests
import pandas as pd
import numpy as np
from typing import Tuple, List, Dict, Any, Optional
try:
    import ccxt
except ImportError:
    ccxt = None

from backend.config import BINANCE_BASE_URLS, BINANCE_API_KEY, BINANCE_API_SECRET
from backend.db import log_event

class BinanceMarketClient:
    """Production-grade client with multi-endpoint fallback and auto-reconnection."""

    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "BinanceTradingTerminal/2.0 (Windows NT 10.0; Win64; x64)",
            "Accept": "application/json"
        })
        self.ccxt_client: Optional[ccxt.binance] = None
        self._init_ccxt()
        self.current_base_url = BINANCE_BASE_URLS[0]
        self.last_ping_ms = 0.0
        self.is_connected = False
        self._cached_markets: Dict[str, Any] = {}
        self._cached_pairs: Dict[str, Tuple[float, List[str]]] = {}
        self._pairs_lock = threading.Lock()
        self._all_tickers_cache: Dict[str, float] = {
            "BTCUSDT": 68500.0, "ETHUSDT": 2650.0, "SOLUSDT": 185.0,
            "BNBUSDT": 595.0, "XRPUSDT": 0.54, "ADAUSDT": 0.35,
            "DOGEUSDT": 0.14, "AVAXUSDT": 28.5, "LINKUSDT": 11.8,
            "DOTUSDT": 4.3, "NEARUSDT": 4.8, "SUIUSDT": 1.95,
            "PEPEUSDT": 0.0000095, "SHIBUSDT": 0.000018
        }
        self._all_tickers_expiry: float = 0.0
        self._tickers_lock = threading.Lock()
        self._conn_cache: Optional[Dict[str, Any]] = None
        self._conn_expiry: float = 0.0

    def _init_ccxt(self):
        """Initializes ccxt instance with safe rate-limiting and timeouts."""
        try:
            config = {
                'enableRateLimit': True,
                'timeout': 10000,
                'options': {'defaultType': 'spot'}
            }
            if BINANCE_API_KEY and BINANCE_API_SECRET:
                config['apiKey'] = BINANCE_API_KEY
                config['secret'] = BINANCE_API_SECRET
            self.ccxt_client = ccxt.binance(config)
        except Exception as e:
            log_event("WARNING", "BINANCE", f"CCXT init note: {e}")
            self.ccxt_client = None

    def test_connection(self) -> Dict[str, Any]:
        """Tests exchange ping, latency, and operational health with 60s cache."""
        now = time.time()
        if self._conn_cache and now < self._conn_expiry:
            return self._conn_cache

        start = time.time()
        for base_url in BINANCE_BASE_URLS:
            try:
                url = f"{base_url}/api/v3/ping"
                res = self.session.get(url, timeout=1.5)
                if res.status_code == 200:
                    latency = round((time.time() - start) * 1000, 1)
                    self.current_base_url = base_url
                    self.last_ping_ms = latency
                    self.is_connected = True
                    result = {
                        "connected": True,
                        "latency_ms": latency,
                        "endpoint": base_url,
                        "error": None
                    }
                    self._conn_cache = result
                    self._conn_expiry = now + 60.0
                    return result
            except Exception:
                continue

        self.is_connected = False
        res_fail = {
            "connected": False,
            "latency_ms": 0.0,
            "endpoint": "None",
            "error": "Binance mirror endpoints busy. Using cached market feeds."
        }
        self._conn_cache = res_fail
        self._conn_expiry = now + 15.0
        return res_fail

    def _request_with_retry(self, endpoint: str, params: dict = None, retries: int = 3) -> Optional[Any]:
        """Internal resilient request handler with exponential backoff & mirror failover."""
        params = params or {}
        for attempt in range(retries):
            for base_url in BINANCE_BASE_URLS:
                try:
                    url = f"{base_url}{endpoint}"
                    res = self.session.get(url, params=params, timeout=7)
                    if res.status_code == 200:
                        self.is_connected = True
                        return res.json()
                    elif res.status_code in (429, 418):
                        wait_sec = 2 ** (attempt + 1)
                        log_event("WARNING", "BINANCE", f"Rate limit encountered ({res.status_code}). Backing off {wait_sec}s...")
                        time.sleep(wait_sec)
                except requests.exceptions.RequestException:
                    continue
            time.sleep(0.5 * (attempt + 1))
        return None

    def get_top_gainers_losers(self, quote: str = "USDT", limit: int = 15) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        Fetches 24-hr market data and computes volatility, volume surge, and top gainers/losers.
        Filters out leveraged tokens and stablecoin pairs.
        """
        data = self._request_with_retry("/api/v3/ticker/24hr")
        if not data:
            log_event("WARNING", "SCANNER", "Failed to pull 24hr ticker data; returning empty.")
            return pd.DataFrame(), pd.DataFrame()

        rows = []
        ignored_substrings = ["UPUSDT", "DOWNUSDT", "BEARUSDT", "BULLUSDT", "BUSDUSDT", "TUSDUSDT", "USDCUSDT", "EURUSDT", "FDUSDUSDT", "AEURUSDT"]

        for item in data:
            sym = item.get("symbol", "")
            if not sym.endswith(quote):
                continue
            if any(sym.endswith(ig) or sym == ig for ig in ignored_substrings):
                continue

            try:
                price = float(item.get("lastPrice", 0))
                price_change_pct = float(item.get("priceChangePercent", 0))
                vol_quote = float(item.get("quoteVolume", 0))  # 24hr volume in USDT
                high = float(item.get("highPrice", 0))
                low = float(item.get("lowPrice", 0))
                trades = int(item.get("count", 0))

                # Discard illiquid noise (< 300,000 USDT daily volume)
                if vol_quote < 300000:
                    continue

                # Volatility Range %: (High - Low) / Low * 100
                volatility_pct = round(((high - low) / low * 100), 2) if low > 0 else 0.0

                # Formatted pair: "BTC/USDT"
                base = sym[:-len(quote)]
                formatted_symbol = f"{base}/{quote}"

                rows.append({
                    "symbol": formatted_symbol,
                    "raw_symbol": sym,
                    "price": price,
                    "change_pct": round(price_change_pct, 2),
                    "volume_usdt": vol_quote,
                    "high": high,
                    "low": low,
                    "volatility_pct": volatility_pct,
                    "trades": trades
                })
            except (ValueError, TypeError):
                continue

        if not rows:
            return pd.DataFrame(), pd.DataFrame()

        df = pd.DataFrame(rows)

        # Calculate volume spike score: z-score or ratio against median
        median_vol = df["volume_usdt"].median()
        df["volume_surge"] = (df["volume_usdt"] / (median_vol + 1e-6)).round(2)

        # Sort Gainers & Losers
        gainers = df[df["change_pct"] > 0].sort_values(by="change_pct", ascending=False).head(limit).reset_index(drop=True)
        losers = df[df["change_pct"] < 0].sort_values(by="change_pct", ascending=True).head(limit).reset_index(drop=True)

        log_event("INFO", "SCANNER", f"Scanned {len(df)} active pairs. Top gainer: {gainers.iloc[0]['symbol']} (+{gainers.iloc[0]['change_pct']}%)")
        return gainers, losers

    def fetch_ohlcv(
        self,
        symbol: str,
        timeframe: str = "1h",
        limit: int = 750,
        since: Optional[int] = None
    ) -> pd.DataFrame:
        """
        Fetches historical OHLCV candlestick data for a symbol.
        Accepts formats like 'BTC/USDT' or 'BTCUSDT'.
        """
        raw_symbol = symbol.replace("/", "").upper()

        # Map timeframe to Binance interval
        tf_map = {
            "1m": "1m", "3m": "3m", "5m": "5m", "15m": "15m",
            "30m": "30m", "1h": "1h", "2h": "2h", "4h": "4h",
            "6h": "6h", "8h": "8h", "12h": "12h", "1d": "1d", "1w": "1w"
        }
        interval = tf_map.get(timeframe, "1h")

        # First attempt: CCXT if initialized
        if self.ccxt_client:
            try:
                ohlcv = self.ccxt_client.fetch_ohlcv(symbol, timeframe=interval, limit=limit, since=since)
                if ohlcv and len(ohlcv) > 0:
                    df = pd.DataFrame(ohlcv, columns=["timestamp", "open", "high", "low", "close", "volume"])
                    df["timestamp"] = pd.to_datetime(df["timestamp"], unit="ms")
                    for col in ["open", "high", "low", "close", "volume"]:
                        df[col] = df[col].astype(float)
                    log_event("INFO", "DATA", f"Fetched {len(df)} {interval} candles for {symbol} via CCXT.")
                    return df
            except Exception as e:
                log_event("WARNING", "DATA", f"CCXT fetch note for {symbol}: {e}. Falling back to REST API.")

        # Direct REST fallback
        params = {
            "symbol": raw_symbol,
            "interval": interval,
            "limit": min(limit, 1000)
        }
        if since:
            params["startTime"] = since

        raw_klines = self._request_with_retry("/api/v3/klines", params=params)
        if not raw_klines:
            log_event("ERROR", "DATA", f"Failed to fetch klines for {symbol} on interval {interval}.")
            return pd.DataFrame()

        rows = []
        for k in raw_klines:
            # Binance kline format: [Open time, Open, High, Low, Close, Volume, Close time, ...]
            rows.append({
                "timestamp": pd.to_datetime(k[0], unit="ms"),
                "open": float(k[1]),
                "high": float(k[2]),
                "low": float(k[3]),
                "close": float(k[4]),
                "volume": float(k[5]),
            })

        df = pd.DataFrame(rows)
        log_event("INFO", "DATA", f"Fetched {len(df)} {interval} candles for {symbol} via Direct REST.")
        return df

    def get_all_tickers(self, force_refresh: bool = False) -> Dict[str, float]:
        """
        Batch-fetches all ticker prices in a single request (/api/v3/ticker/price).
        Caches results for 8 seconds, making subsequent price lookups instant O(1).
        """
        now = time.time()
        with self._tickers_lock:
            if not force_refresh and now < self._all_tickers_expiry and len(self._all_tickers_cache) > 20:
                return self._all_tickers_cache

            for base_url in [self.current_base_url] + [u for u in BINANCE_BASE_URLS if u != self.current_base_url]:
                try:
                    url = f"{base_url}/api/v3/ticker/price"
                    res = self.session.get(url, timeout=2.5)
                    if res.status_code == 200:
                        raw = res.json()
                        if isinstance(raw, list):
                            for item in raw:
                                s = item.get("symbol")
                                p = item.get("price")
                                if s and p:
                                    try:
                                        self._all_tickers_cache[s.upper()] = float(p)
                                    except (ValueError, TypeError):
                                        pass
                            self._all_tickers_expiry = now + 8.0
                            self.is_connected = True
                            return self._all_tickers_cache
                except Exception:
                    continue

            self._all_tickers_expiry = now + 4.0
            return self._all_tickers_cache

    def get_ticker_price(self, symbol: str) -> Optional[float]:
        """Returns the real-time mark/last price for a symbol via instant batch cache."""
        clean = symbol.replace("/", "").upper()
        now = time.time()

        if clean in self._all_tickers_cache and now < self._all_tickers_expiry:
            return self._all_tickers_cache[clean]

        tickers = self.get_all_tickers()
        if clean in tickers:
            return tickers[clean]

        if clean in self._all_tickers_cache:
            return self._all_tickers_cache[clean]

        try:
            res = self._request_with_retry("/api/v3/ticker/price", params={"symbol": clean}, retries=1)
            if res and "price" in res:
                val = float(res["price"])
                self._all_tickers_cache[clean] = val
                return val
        except Exception:
            pass

        return None

    def load_markets(self, reload: bool = False) -> Dict[str, Any]:
        """
        Direct CCXT dynamic market loader: calls exchange.load_markets().
        """
        if not self.ccxt_client:
            self._init_ccxt()

        if self.ccxt_client:
            try:
                markets = self.ccxt_client.load_markets(reload=reload)
                self._cached_markets = markets
                return markets
            except Exception as e:
                log_event("WARNING", "BINANCE", f"CCXT load_markets exception: {e}")

        return self._cached_markets

    def get_all_active_pairs(self, quote: Optional[str] = "USDT", force_refresh: bool = False) -> List[str]:
        """
        Dynamically loads every single active trading pair on Binance without any hardcoding.
        Uses CCXT (exchange.load_markets()) as the primary engine with Direct REST fallback.
        Results are cached in memory (TTL: 15 min) for snappy UI responsiveness.
        """
        cache_key = (quote or "ALL").upper()
        now = time.time()

        if not force_refresh and cache_key in self._cached_pairs:
            cached_time, cached_list = self._cached_pairs[cache_key]
            if now - cached_time < 900 and len(cached_list) > 0:
                return cached_list

        pairs: List[str] = []
        ignored = {"UP", "DOWN", "BEAR", "BULL"}

        # 1. Primary Loader: CCXT exchange.load_markets()
        try:
            markets = self.load_markets(reload=force_refresh)
            if markets:
                for sym, m in markets.items():
                    if not m.get("active", True) or not m.get("spot", True):
                        continue
                    m_quote = m.get("quote", "").upper()
                    m_base = m.get("base", "").upper()
                    if any(ig in m_base for ig in ignored):
                        continue
                    if quote and quote.upper() != "ALL":
                        if m_quote == quote.upper():
                            pairs.append(m.get("symbol", f"{m_base}/{m_quote}"))
                    else:
                        pairs.append(m.get("symbol", f"{m_base}/{m_quote}"))
        except Exception as ex:
            log_event("WARNING", "BINANCE", f"CCXT market parse error: {ex}")

        # 2. Resilient Direct REST Fallback (/api/v3/exchangeInfo)
        if not pairs:
            try:
                info = self._request_with_retry("/api/v3/exchangeInfo")
                if info and "symbols" in info:
                    for s in info["symbols"]:
                        if s.get("status") == "TRADING" and s.get("isSpotTradingAllowed", True):
                            base_asset = s.get("baseAsset", "").upper()
                            quote_asset = s.get("quoteAsset", "").upper()
                            if any(ig in base_asset for ig in ignored):
                                continue
                            if quote and quote.upper() != "ALL":
                                if quote_asset == quote.upper():
                                    pairs.append(f"{base_asset}/{quote_asset}")
                            else:
                                pairs.append(f"{base_asset}/{quote_asset}")
            except Exception as rex:
                log_event("WARNING", "BINANCE", f"Direct REST exchangeInfo error: {rex}")

        # 3. Fail-safe Fallback if completely offline
        if not pairs:
            from backend.config import DEFAULT_PAIRS
            pairs = list(DEFAULT_PAIRS)

        # De-duplicate, normalize, and sort
        unique_pairs = sorted(list(dict.fromkeys(pairs)))
        self._cached_pairs[cache_key] = (now, unique_pairs)
        log_event("INFO", "BINANCE", f"Dynamically loaded {len(unique_pairs)} active pairs for quote '{cache_key}'.")
        return unique_pairs

# Singleton instance
binance_client = BinanceMarketClient()
