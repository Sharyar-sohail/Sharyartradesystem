"""
Configuration & System Settings for Binance Trading & Backtesting Terminal
"""

import os
from pathlib import Path

# Base Paths
BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
ASSETS_DIR = BASE_DIR / "assets"

os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)

# Database
DB_PATH = DATA_DIR / "trading_terminal.db"

# Memory Ring Buffer Limits (Low-storage RAM footprint)
MAX_LOG_BUFFER_SIZE = 600
MAX_TICKER_CACHE_SIZE = 100
MAX_CANDLE_BUFFER_SIZE = 1500

# SQLite Retention Limits (Auto-pruning to save disk space)
MAX_LOG_RECORDS = 2500
MAX_CACHE_DAYS = 30

# Network & Exchange
BINANCE_API_KEY = os.getenv("BINANCE_API_KEY", "")
BINANCE_API_SECRET = os.getenv("BINANCE_API_SECRET", "")
BINANCE_BASE_URLS = [
    "https://api.binance.com",
    "https://data-api.binance.vision",
    "https://api1.binance.com",
    "https://api2.binance.com",
    "https://api3.binance.com",
]

# Default Trading / Backtest Defaults
DEFAULT_TIMEFRAMES = ["5m", "15m", "1h", "4h", "1d"]
DEFAULT_TIMEFRAME = "1h"
DEFAULT_QUOTE = "USDT"

# Top Default Watchlist Pairs (Strict Priority: BTC & Gold First)
DEFAULT_PAIRS = [
    "BTC/USDT",
    "XAU/USD",
    "ETH/USDT",
    "SOL/USDT",
    "BNB/USDT",
    "DOGE/USDT",
    "XRP/USDT",
    "ADA/USDT",
    "AVAX/USDT",
    "NEAR/USDT",
    "LINK/USDT"
]

# Risk Management & Execution Assumptions
DEFAULT_INITIAL_CAPITAL = 10000.0  # USDT
DEFAULT_RISK_PER_TRADE_PCT = 2.0   # 2% of equity per trade
DEFAULT_SL_ATR_MULT = 1.5          # 1.5x ATR for stop loss
DEFAULT_TP_ATR_MULT = 3.0          # 3.0x ATR for take profit (1:2 Risk/Reward)
DEFAULT_FEE_PCT = 0.001            # 0.1% Binance VIP0 Taker fee
DEFAULT_SLIPPAGE_PCT = 0.0005      # 0.05% slippage

# Server Settings
STREAMLIT_PORT = 8501
STREAMLIT_HOST = "localhost"

# Pakistan Stock Exchange (PSX) Configuration
PSX_API_KEY = os.getenv("PSX_API_KEY", "")
PSX_API_URL = os.getenv("PSX_API_URL", "https://dps.psx.com.pk")
PSX_DATA_SOURCE = os.getenv("PSX_DATA_SOURCE", "auto")
PSX_RATE_LIMIT_SEC = float(os.getenv("PSX_RATE_LIMIT_SEC", "0.10"))
PSX_CACHE_TTL_SEC = float(os.getenv("PSX_CACHE_TTL_SEC", "15.0"))
